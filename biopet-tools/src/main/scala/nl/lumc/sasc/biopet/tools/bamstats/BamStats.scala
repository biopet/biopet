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

import java.io.{ File, PrintWriter }

import htsjdk.samtools.{ SAMSequenceDictionary, SamReaderFactory }
import nl.lumc.sasc.biopet.utils.BamUtils.SamDictCheck
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, FastaUtils, ToolCommand }
import nl.lumc.sasc.biopet.utils.intervals.{ BedRecord, BedRecordList }

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.io.Source
import scala.language.postfixOps

/**
 * This tool will collect stats from a bamfile
 *
 * Created by pjvanthof on 25/05/16.
 */
object BamStats extends ToolCommand {
  case class Args(outputDir: File = null,
                  bamFile: File = null,
                  referenceFasta: Option[File] = None,
                  binSize: Int = 10000,
                  threadBinSize: Int = 10000000) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('R', "reference") valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = Some(x))
    } text "Fasta file of reference"
    opt[File]('o', "outputDir") required () valueName "<directory>" action { (x, c) =>
      c.copy(outputDir = x)
    } text "Output directory"
    opt[File]('b', "bam") required () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    } text "Input bam file"
    opt[Int]("binSize") valueName "<int>" action { (x, c) =>
      c.copy(binSize = x)
    } text "Bin size of stats (beta)"
    opt[Int]("threadBinSize") valueName "<int>" action { (x, c) =>
      c.copy(threadBinSize = x)
    } text "Size of region per thread"
  }

  /** This is the main entry to [[BamStats]], this will do the argument parsing. */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val sequenceDict = validateReferenceInBam(cmdArgs.bamFile, cmdArgs.referenceFasta)

    init(cmdArgs.outputDir, cmdArgs.bamFile, sequenceDict, cmdArgs.binSize, cmdArgs.threadBinSize)

    logger.info("Done")
  }

  /**
   * This will retrieve the [[SAMSequenceDictionary]] from the bam file.
   * When `referenceFasta is given he will validate this against the bam file.`
   */
  def validateReferenceInBam(bamFile: File, referenceFasta: Option[File]) = {
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    val samHeader = samReader.getFileHeader
    samReader.close()
    referenceFasta.map { f =>
      samHeader.getSequenceDictionary.assertSameDictionary(FastaUtils.getCachedDict(f), false)
      FastaUtils.getCachedDict(f)
    }.getOrElse(samHeader.getSequenceDictionary)
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
  def init(outputDir: File, bamFile: File, referenceDict: SAMSequenceDictionary, binSize: Int, threadBinSize: Int): Unit = {
    val contigsFutures = BedRecordList.fromDict(referenceDict).allRecords.map { contig =>
      contig.chr -> processContig(contig, bamFile, binSize, threadBinSize, outputDir)
    }.toList

    val stats = waitOnFutures(processUnmappedReads(bamFile) :: contigsFutures.map(_._2))

    val statsWriter = new PrintWriter(new File(outputDir, "stats.json"))
    val statsMap = Map(
      "total" -> stats.toSummaryMap,
      "contigs" -> contigsFutures.map(x => x._1 -> x._2.value.get.get.toSummaryMap).toMap
    )
    statsWriter.println(ConfigUtils.mapToJson(statsMap).spaces2)
    statsWriter.close()
    //stats.writeStatsToFiles(outputDir)

    // FIXME: getting aggregated stats

    val clippingHistogram = tsvToMap(new File(outputDir, "clipping.tsv"))
    val mappingQualityHistogram = tsvToMap(new File(outputDir, "mapping_quality.tsv"))

    val summary = Map(
      "flagstats" -> ConfigUtils.fileToConfigMap(new File(outputDir, "flagstats.summary.json")),
      "flagstats_per_contig" -> referenceDict.getSequences.map {
        c =>
          c.getSequenceName -> ConfigUtils.fileToConfigMap(
            new File(outputDir, "contigs" + File.separator + c.getSequenceName + File.separator + "flagstats.summary.json"))
      }.toMap,
      "mapping_quality" -> Map("general" -> aggregateStats(mappingQualityHistogram), "histogram" -> mappingQualityHistogram),
      "clipping" -> Map("general" -> aggregateStats(clippingHistogram), "histogram" -> clippingHistogram)
    )

    val summaryWriter = new PrintWriter(new File(outputDir, "bamstats.summary.json"))
    summaryWriter.println(ConfigUtils.mapToJson(summary).spaces2)
    summaryWriter.close()
  }

  def aggregateStats(table: Map[String, Array[Long]]): Map[String, Any] = {
    val values = table("value")
    val counts = table("count")
    require(values.size == counts.size)
    if (values.nonEmpty) {
      val modal = values(counts.indexOf(counts.max))
      val totalCounts = counts.sum
      val mean: Double = values.zip(counts).map(x => x._1 * x._2).sum.toDouble / totalCounts
      val median: Long = values(values.zip(counts).zipWithIndex.sortBy(_._1._1).foldLeft((0L, 0)) {
        case (a, b) =>
          val total = a._1 + b._1._2
          if (total >= totalCounts / 2) (total, a._2)
          else (total, b._2)
      }._2)
      Map("min" -> values.min, "max" -> values.max, "median" -> median, "mean" -> mean, "modal" -> modal)
    } else Map()
  }

  /**
   * This will start the subjobs for each contig and collect [[Stats]] on contig level
   *
   * @param region Region to check, mostly yhis is the complete contig
   * @param bamFile Input bam file
   * @param binSize stats binsize
   * @param threadBinSize Thread binsize
   * @return Output stats
   */
  def processContig(region: BedRecord, bamFile: File, binSize: Int, threadBinSize: Int, outputDir: File): Future[Stats] = Future {
    val scattersFutures = region
      .scatter(binSize)
      .grouped((region.length.toDouble / binSize).ceil.toInt / (region.length.toDouble / threadBinSize).ceil.toInt)
      .map(scatters => processThread(scatters, bamFile))
      .toList
    waitOnFutures(scattersFutures, Some(region.chr))
  }

  /**
   * This method will wait when all futures are complete and collect a single [[Stats]] instance
   *
   * @param futures List of futures to monitor
   * @param msg Optional message for logging
   * @return Output stats
   */
  def waitOnFutures(futures: List[Future[Stats]], msg: Option[String] = None): Stats = {
    msg.foreach(m => logger.info(s"Start monitoring jobs for '$m', ${futures.size} jobs"))
    futures.foreach(_.onFailure { case t => throw new RuntimeException(t) })
    val stats = Await.result(Future.sequence(futures).map(_.fold(Stats())(_ += _)), Duration.Inf)
    msg.foreach(m => logger.info(s"All jobs for '$m' are done"))
    stats
  }

  /**
   * This method will process 1 thread bin
   *
   * @param scatters bins to check, there should be no gaps withing the scatters
   * @param bamFile Input bamfile
   * @return Output stats
   */
  def processThread(scatters: List[BedRecord], bamFile: File): Future[Stats] = Future {
    val totalStats = Stats()
    val sortedScatters = scatters.sortBy(_.start)
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    val threadChr = sortedScatters.head.chr
    val threadStart = sortedScatters.head.start
    val threadEnd = sortedScatters.last.end
    val it = samReader.query(threadChr, threadStart, threadEnd, false).buffered
    for (samRecord <- it) {

      // Read based stats
      if (samRecord.getAlignmentStart > threadStart && samRecord.getAlignmentStart <= threadEnd) {
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
    samReader.close()

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
      require(values.size == header.size, s"Line does not have the number of field as header: $line")
      for (array <- arrays) {
        array._2._2.append(values(array._1).toLong)
      }
    }
    reader.close()
    arrays.map(x => x._2._1 -> x._2._2.toArray).toMap
  }

}
