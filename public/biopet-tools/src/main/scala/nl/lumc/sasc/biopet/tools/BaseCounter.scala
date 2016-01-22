package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.{SAMRecord, SamReaderFactory}
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Created by pjvanthof on 22/01/16.
  */
object BaseCounter extends ToolCommand {
  case class Args(bedFile: File = null,
                  outputDir: File = null,
                  bamFile: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]("bedFile") required () valueName "<file>" action { (x, c) =>
      c.copy(bedFile = x)
    }
    opt[File]('o', "outputDir") required () valueName "<file>" action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[File]('b', "bam") required () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse(throw new IllegalArgumentException(""))

    val bedrecords = BedRecordList.fromFile(cmdArgs.bedFile).combineOverlap

    val counts = for (region <- bedrecords.allRecords) yield {
      val bamReader = SamReaderFactory.makeDefault.open(cmdArgs.bamFile)
      val interval = region.toSamInterval
      val samIterator = bamReader.queryOverlapping(interval.getContig, interval.getStart, interval.getEnd)
      for (samRecord <- samIterator) samRecordToCounts(samRecord, region.originals())
    }
  }

  def samRecordToCounts(samRecord: SAMRecord, bedRecords: List[BedRecord]): Unit = {
    samRecord.getAlignmentBlocks.foreach { x =>
      val bedStart = x.getReferenceStart - 1
      val bedEnd = bedStart + x.getLength
    }
  }
}
