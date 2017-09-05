/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.tools.bamstats

import java.io.{File, PrintWriter}

import htsjdk.samtools.{SAMSequenceDictionary, SamReader, SamReaderFactory}
import nl.lumc.sasc.biopet.utils.BamUtils.SamDictCheck
import nl.lumc.sasc.biopet.utils.{ConfigUtils, FastaUtils, ToolCommand}
import nl.lumc.sasc.biopet.utils.intervals.BedRecord

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.io.Source
import scala.language.postfixOps

/**
  * This tool will collect stats from a bamfile
  *
  * Created by pjvanthof on 25/05/16.
  */
object BamStats extends ToolCommand {

  type Args = BamStatsArgs

  /** This is the main entry to [[BamStats]], this will do the argument parsing. */
  def main(args: Array[String]): Unit = {
    val argsParser = new BamStatsOptParser
    val cmdArgs = argsParser.parse(args, BamStatsArgs()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val sequenceDict = validateReferenceInBam(cmdArgs.bamFile, cmdArgs.referenceFasta)

    init(cmdArgs.outputDir,
         cmdArgs.bamFile,
         sequenceDict,
         cmdArgs.binSize,
         cmdArgs.threadBinSize,
         cmdArgs.tsvOutputs)

    logger.info("Done")
  }

  /**
    * This will retrieve the [[SAMSequenceDictionary]] from the bam file.
    * When `referenceFasta is given he will validate this against the bam file.`
    */
  def validateReferenceInBam(bamFile: File, referenceFasta: Option[File]): SAMSequenceDictionary = {
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    val samHeader = samReader.getFileHeader
    samReader.close()
    referenceFasta
      .map { f =>
        samHeader.getSequenceDictionary.assertSameDictionary(FastaUtils.getCachedDict(f), false)
        FastaUtils.getCachedDict(f)
      }
      .getOrElse(samHeader.getSequenceDictionary)
  }

  /**
    * This is the main running function of [[BamStats]]. This will start the threads and collect and write the results.
    *
    * @param outputDir All output files will be placed here
    * @param bamFile Input bam file
    * @param referenceDict Dict for scattering
    * @param binSize stats binsize
    * @param threadBinSize Thread binsize
    */
  def init(outputDir: File,
           bamFile: File,
           referenceDict: SAMSequenceDictionary,
           binSize: Int,
           threadBinSize: Int,
           tsvOutput: Boolean): Unit = {
    val contigs = referenceDict.getSequences
      .flatMap(r => BedRecord(r.getSequenceName, 0, r.getSequenceLength).scatter(threadBinSize))
    val groups = contigs.foldLeft((List[List[BedRecord]](), List[BedRecord](), 0L)) {
      case ((finalList, tempList, oldSize), b) =>
        if (oldSize < threadBinSize) (finalList, b :: tempList, oldSize + b.length)
        else (tempList :: finalList, b :: Nil, b.length)
    }
    val contigsFutures = (groups._2 :: groups._1).map(x => processThread(x, bamFile))

    val unmappedStats = processUnmappedReads(bamFile)
    val (stats, contigStats) = waitOnFutures(contigsFutures)
    stats += Await.result(unmappedStats, Duration.Inf)

    if (tsvOutput) {
      stats.flagstat.writeAsTsv(new File(outputDir, "flagstats.tsv"))

      stats.insertSizeHistogram.writeFilesAndPlot(outputDir,
                                                  "insertsize",
                                                  "Insertsize",
                                                  "Reads",
                                                  "Insertsize distribution")
      stats.mappingQualityHistogram.writeFilesAndPlot(outputDir,
                                                      "mappingQuality",
                                                      "Mapping Quality",
                                                      "Reads",
                                                      "Mapping Quality distribution")
      stats.clippingHistogram.writeFilesAndPlot(outputDir,
                                                "clipping",
                                                "CLipped bases",
                                                "Reads",
                                                "Clipping distribution")

      stats.leftClippingHistogram.writeFilesAndPlot(outputDir,
                                                    "left_clipping",
                                                    "CLipped bases",
                                                    "Reads",
                                                    "Left Clipping distribution")
      stats.rightClippingHistogram.writeFilesAndPlot(outputDir,
                                                     "right_clipping",
                                                     "CLipped bases",
                                                     "Reads",
                                                     "Right Clipping distribution")
      stats._3_ClippingHistogram.writeFilesAndPlot(outputDir,
                                                   "3prime_clipping",
                                                   "CLipped bases",
                                                   "Reads",
                                                   "3 Prime Clipping distribution")
      stats._5_ClippingHistogram.writeFilesAndPlot(outputDir,
                                                   "5prime_clipping",
                                                   "CLipped bases",
                                                   "Reads",
                                                   "5 Prime Clipping distribution")
    }

    val statsWriter = new PrintWriter(new File(outputDir, "bamstats.json"))
    val totalStats = stats.toSummaryMap
    val statsMap = Map(
      "total" -> totalStats,
      "contigs" -> contigStats
    )
    statsWriter.println(ConfigUtils.mapToJson(statsMap).nospaces)
    statsWriter.close()

    val summaryWriter = new PrintWriter(new File(outputDir, "bamstats.summary.json"))
    summaryWriter.println(ConfigUtils.mapToJson(totalStats).nospaces)
    summaryWriter.close()
  }

  /**
    * This method will wait when all futures are complete and collect a single [[Stats]] instance
    *
    * @param futures List of futures to monitor
    * @param msg Optional message for logging
    * @return Output stats
    */
  def waitOnFutures(futures: List[Future[Map[BedRecord, Stats]]],
                    msg: Option[String] = None): (Stats, Map[String, Stats]) = {
    msg.foreach(m => logger.info(s"Start monitoring jobs for '$m', ${futures.size} jobs"))
    futures.foreach(_.onFailure { case t => throw new RuntimeException(t) })
    val totalSize = futures.size
    val totalStats = Stats()
    val contigStats: mutable.Map[String, Stats] = mutable.Map()

    def wait(todo: List[Future[Map[BedRecord, Stats]]]): Unit = {
      try {
        logger.info(s"${totalSize - todo.size}/$totalSize tasks done")
        val completed = todo.groupBy(_.isCompleted)
        completed.getOrElse(true, Nil).foreach { f =>
          Await.result(f, Duration.Inf).foreach {
            case (region, stats) =>
              totalStats += stats
              if (contigStats.contains(region.chr)) contigStats(region.chr) += stats
              else contigStats(region.chr) = stats
          }
        }
        if (completed.contains(false)) {
          Thread.sleep(10000)
          wait(completed(false))
        }
      } catch {
        case _: TimeoutException =>
          wait(todo)
      }
    }

    wait(futures)

    msg.foreach(m => logger.info(s"All jobs for '$m' are done"))
    (totalStats, contigStats.toMap)
  }

  /**
    * This method will process 1 thread bin
    *
    * @param scatters bins to check, there should be no gaps withing the scatters
    * @param bamFile Input bamfile
    * @return Output stats
    */
  def processThread(scatters: List[BedRecord], bamFile: File): Future[Map[BedRecord, Stats]] =
    Future {
      logger.debug(s"Start task on ${scatters.size} regions")
      val samReader: SamReader = SamReaderFactory.makeDefault().open(bamFile)
      val results = scatters.map { bedRecord =>
        bedRecord -> processRegion(bedRecord, samReader)
      }
      samReader.close()

      results.toMap
    }

  def processRegion(bedRecord: BedRecord, samReader: SamReader): Stats = {
    //logger.debug(s"Start on $bedRecord")
    val totalStats = Stats()
    val it = samReader.query(bedRecord.chr, bedRecord.start, bedRecord.end, false)
    for (samRecord <- it) {

      // Read based stats
      if (samRecord.getAlignmentStart > bedRecord.start && samRecord.getAlignmentStart <= bedRecord.end) {
        totalStats.flagstat.loadRecord(samRecord)
        if (!samRecord.getReadUnmappedFlag) { // Mapped read
          totalStats.mappingQualityHistogram.add(samRecord.getMappingQuality)
        }
        if (samRecord.getReadPairedFlag && samRecord.getProperPairFlag && samRecord.getFirstOfPairFlag && !samRecord.getSecondOfPairFlag)
          totalStats.insertSizeHistogram.add(samRecord.getInferredInsertSize.abs)

        val leftClipping = samRecord.getAlignmentStart - samRecord.getUnclippedStart
        val rightClipping = samRecord.getUnclippedEnd - samRecord.getAlignmentEnd

        totalStats.clippingHistogram.add(leftClipping + rightClipping)
        totalStats.leftClippingHistogram.add(leftClipping)
        totalStats.rightClippingHistogram.add(rightClipping)

        if (samRecord.getReadNegativeStrandFlag) {
          totalStats._5_ClippingHistogram.add(leftClipping)
          totalStats._3_ClippingHistogram.add(rightClipping)
        } else {
          totalStats._5_ClippingHistogram.add(rightClipping)
          totalStats._3_ClippingHistogram.add(leftClipping)
        }

        //TODO: Bin Support
      }

      //TODO: bases counting
    }
    it.close()
    totalStats
  }

  /**
    * This method will only count the unmapped fragments
    *
    * @param bamFile Input bamfile
    * @return Output stats
    */
  def processUnmappedReads(bamFile: File): Future[Stats] = Future {
    val stats = Stats()
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    for (samRecord <- samReader.queryUnmapped()) {
      stats.flagstat.loadRecord(samRecord)
    }
    samReader.close()
    stats
  }

  def tsvToMap(tsvFile: File): Map[String, Array[Long]] = {
    val reader = Source.fromFile(tsvFile)
    val it = reader.getLines()
    val header = it.next().split("\t")
    val arrays = header.zipWithIndex.map(x => x._2 -> (x._1 -> ArrayBuffer[Long]()))
    for (line <- it) {
      val values = line.split("\t")
      require(values.size == header.size,
              s"Line does not have the number of field as header: $line")
      for (array <- arrays) {
        array._2._2.append(values(array._1).toLong)
      }
    }
    reader.close()
    arrays.map(x => x._2._1 -> x._2._2.toArray).toMap
  }

}
