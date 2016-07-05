package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File

import htsjdk.samtools.{SAMSequenceDictionary, SamReaderFactory}
import htsjdk.samtools.reference.FastaSequenceFile
import nl.lumc.sasc.biopet.utils.BamUtils.SamDictCheck
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import nl.lumc.sasc.biopet.utils.{BamUtils, ToolCommand}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._

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

    val sequenceDict = validateReferenceinBam(cmdArgs.bamFile, cmdArgs.referenceFasta)

    init(cmdArgs.outputDir, cmdArgs.bamFile, sequenceDict, cmdArgs.binSize, cmdArgs.threadBinSize)
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
      val f = Future { processContig(contig, bamFile, binSize, threadBinSize) }
      f.onFailure { case t => throw new RuntimeException(t) }
      contig -> f
    }

    // Waiting on all contigs to complete
    contigsFutures.foreach { x =>
      Await.ready(x._2, Duration.Inf)
      x._2.value match {
        case Some(x) =>
          require(x.isSuccess)
          x.foreach( stats += _)
      }
    }

    println(stats.totalReads)
  }

  def processContig(region: BedRecord, bamFile: File, binSize: Int, threadBinSize: Int): Stats = {
    var stats = Stats()

    val scattersPerThread = threadBinSize / binSize
    val scattersFutures = region
      .scatter(binSize)
      .grouped(scattersPerThread).map { scatters =>
      val f = Future { processThread(scatters, bamFile) }
      f.onFailure { case t => throw new RuntimeException(t) }
      f
    }

    // Waiting on all contigs to complete
    scattersFutures.foreach { x =>
      Await.ready(x, Duration.Inf)
      x.value match {
        case Some(x) =>
          require(x.isSuccess)
          x.foreach(stats += _)
      }
    }

    stats
  }

  def processThread(scatters: List[BedRecord], bamFile: File): Stats = {
    val totalStats = Stats()
    val sortedScatters = scatters.sortBy(_.start)
    val samReader = SamReaderFactory.makeDefault().open(bamFile)
    val threadChr = sortedScatters.head.chr
    val threadStart = sortedScatters.head.start
    val threadEnd = sortedScatters.last.end
    val it = samReader.query(threadChr, threadStart, threadEnd, false).buffered
    for (samRecord <- it) {
      if (samRecord.getAlignmentStart > threadStart && samRecord.getAlignmentStart <= threadEnd) {
        totalStats.totalReads += 1
        //TODO: Read counting
      }

      //TODO: bases counting
    }
    samReader.close()

    totalStats
  }
}
