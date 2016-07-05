package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File

import htsjdk.samtools.{SAMSequenceDictionary, SamReaderFactory}
import htsjdk.samtools.reference.FastaSequenceFile
import nl.lumc.sasc.biopet.utils.BamUtils.SamDictCheck
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import nl.lumc.sasc.biopet.utils.{BamUtils, ToolCommand}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.blocking
import scala.util.{Failure, Success}
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._
import scala.collection.immutable.Queue

/**
  * Created by pjvanthof on 25/05/16.
  */
object BamStats extends ToolCommand {
  case class Args(outputDir: File = null,
                  bamFile: File = null,
                  referenceFasta: Option[File] = None,
                  binSize: Int = 10000,
                  threadBinSize: Int = 10000000) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('R', "reference")  valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = Some(x))
    }
    opt[File]('o', "outputDir") required () valueName "<directory>" action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[File]('b', "bam") required () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    }
    opt[Int]("binSize") valueName "<int>" action { (x, c) =>
      c.copy(binSize = x)
    }
    opt[Int]("threadBinSize") valueName "<int>" action { (x, c) =>
      c.copy(threadBinSize = x)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val sequenceDict = validateReferenceinBam(cmdArgs.bamFile, cmdArgs.referenceFasta)

    init(cmdArgs.outputDir, cmdArgs.bamFile, sequenceDict, cmdArgs.binSize, cmdArgs.threadBinSize)

    logger.info("Done")
  }

  def validateReferenceinBam(bamFile: File, referenceFasta: Option[File]) = {
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

  def init(outputDir: File, bamFile: File, referenceDict: SAMSequenceDictionary, binSize: Int, threadBinSize: Int): Unit = {
    var stats = Stats()

    val contigsFutures = BedRecordList.fromDict(referenceDict).allRecords.map { contig =>
      val f = Future {
        val s = processContig(contig, bamFile, binSize, threadBinSize)
        blocking { stats += s }
      }
      f.onFailure { case t => throw new RuntimeException(t) }
      contig -> f
    }

    val unmappedFuture = Future { processUnmappedReads(bamFile) }
    unmappedFuture.onFailure { case t => throw new RuntimeException(t) }

    // Waiting on all contigs to complete
    contigsFutures.foreach { x => Await.ready(x._2, Duration.Inf) }

    Await.ready(unmappedFuture, Duration.Inf)
    stats += unmappedFuture.value.get.get

    logger.info(s"total: ${stats.totalReads},  unmapped: ${stats.unmapped}, secondary: ${stats.secondary}")
    stats.insertSizeHistogram.keys.toList.sorted.foreach(x => println(s"$x\t${stats.insertSizeHistogram(x)}"))
  }

  def processContig(region: BedRecord, bamFile: File, binSize: Int, threadBinSize: Int): Stats = {
    logger.info(s"Contig '${region.chr}' starting")
    var stats = Stats()

    val scatters = region
      .scatter(binSize)

    val scattersPerThread = (region.length.toDouble / threadBinSize).ceil.toInt

    val scattersFutures = scatters.grouped(scattersPerThread).map { scatters =>
      val f = Future {
        val s = processThread(scatters, bamFile)
        blocking { stats += s }
      }
      f.onFailure { case t => throw new RuntimeException(t) }
      f
    }

    // Waiting on all contigs to complete
    scattersFutures.foreach { x => Await.ready(x, Duration.Inf) }

    logger.info(s"Contig '${region.chr}' done")

    stats
  }

  def processThread(scatters: List[BedRecord], bamFile: File): Stats = {
    var totalStats = Stats()
    val sortedScatters = scatters.sortBy(_.start)
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    val threadChr = sortedScatters.head.chr
    val threadStart = sortedScatters.head.start
    val threadEnd = sortedScatters.last.end
    val it = samReader.query(threadChr, threadStart, threadEnd, false).buffered
    for (samRecord <- it) {

      // Read based stats
      if (samRecord.getAlignmentStart > threadStart && samRecord.getAlignmentStart <= threadEnd) {
        totalStats.totalReads += 1
        if (samRecord.isSecondaryOrSupplementary) totalStats.secondary += 1
        if (samRecord.getReadUnmappedFlag) totalStats.unmapped += 1
        else { // Mapped read
          totalStats.mappingQualityHistogram += samRecord.getMappingQuality -> (totalStats.mappingQualityHistogram.getOrElse(samRecord.getMappingQuality, 0L) + 1)
        }
        if (samRecord.getProperPairFlag && samRecord.getFirstOfPairFlag && !samRecord.getSecondOfPairFlag)
          totalStats.insertSizeHistogram += samRecord.getInferredInsertSize.abs -> (totalStats.insertSizeHistogram.getOrElse(samRecord.getInferredInsertSize.abs, 0L) + 1)

        //TODO: Read counting
      }

      //TODO: bases counting
    }
    samReader.close()

    totalStats
  }

  def processUnmappedReads(bamFile: File): Stats = {
    var stats = Stats()
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    var size = samReader.queryUnmapped().size
    stats.totalReads += size
    stats.unmapped += size
    samReader.close()
    stats
  }
}
