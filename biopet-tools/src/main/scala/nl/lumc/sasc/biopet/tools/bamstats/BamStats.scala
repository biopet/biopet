package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File
import java.util.concurrent.TimeoutException

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.samtools.{ SAMSequenceDictionary, SamReaderFactory }
import nl.lumc.sasc.biopet.utils.BamUtils.SamDictCheck
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.{ BedRecord, BedRecordList }

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
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
      val referenceReader = new FastaSequenceFile(f, true)
      val referenceDict = referenceReader.getSequenceDictionary
      samHeader.getSequenceDictionary.assertSameDictionary(referenceDict, false)
      referenceReader.close()
      referenceDict
    }.getOrElse(samHeader.getSequenceDictionary)
  }

  /**
   * This is the main running function of [[BamStats]]. This will start the thereads and collect and write the results.
   *
   * @param outputDir All output files will be placed here
   * @param bamFile Input bam file
   * @param referenceDict Dict for scattering
   * @param binSize stats binsize
   * @param threadBinSize Thread binsize
   */
  def init(outputDir: File, bamFile: File, referenceDict: SAMSequenceDictionary, binSize: Int, threadBinSize: Int): Unit = {
    val contigsFutures = BedRecordList.fromDict(referenceDict).allRecords.map { contig =>
      Future { processContig(contig, bamFile, binSize, threadBinSize, outputDir) }
    }

    val unmappedFuture = Future { processUnmappedReads(bamFile) }

    val stats = waitOnFutures(unmappedFuture :: contigsFutures.toList)

    stats.writeStatsToFiles(outputDir)
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
  def processContig(region: BedRecord, bamFile: File, binSize: Int, threadBinSize: Int, outputDir: File): Stats = {
    val scattersFutures = region
      .scatter(binSize)
      .grouped((region.length.toDouble / binSize).ceil.toInt / (region.length.toDouble / threadBinSize).ceil.toInt)
      .map(scatters => Future { processThread(scatters, bamFile) })
      .toList
    val stats = waitOnFutures(scattersFutures, Some(region.chr))
    val contigDir = new File(outputDir, "contigs" + File.separator + region.chr)
    contigDir.mkdirs()
    stats.writeStatsToFiles(contigDir)
    stats
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
    var stats = Stats()
    var running = futures
    while (running.nonEmpty) {
      val done = running.filter(_.value.isDefined)
      done.foreach(stats += _.value.get.get)
      running = running.filterNot(done.contains(_))
      if (running.nonEmpty && done.nonEmpty) msg.foreach(m => logger.info(s"Jobs for '$m', ${running.size}/${futures.size} jobs"))
      if (running.nonEmpty) try {
        Await.ready(running.head, 1 second)
      } catch {
        case e: TimeoutException =>
      }
    }
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
  def processThread(scatters: List[BedRecord], bamFile: File): Stats = {
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
        if (samRecord.getProperPairFlag && samRecord.getFirstOfPairFlag && !samRecord.getSecondOfPairFlag)
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
  def processUnmappedReads(bamFile: File): Stats = {
    val stats = Stats()
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    for (samRecord <- samReader.queryUnmapped()) {
      stats.flagstat.loadRecord(samRecord)
    }
    samReader.close()
    stats
  }
}
