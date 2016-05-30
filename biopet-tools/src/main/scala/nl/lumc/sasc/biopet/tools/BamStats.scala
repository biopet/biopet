package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList

/**
  * Created by pjvanthof on 25/05/16.
  */
object BamStats extends ToolCommand {
  case class Args(outputDir: File = null,
                  bamFile: File = null,
                  reference: File = null,
                  binSize: Int = 10000,
                  threadBinSize: Int = 10000000) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('R', "reference") required () valueName "<file>" action { (x, c) =>
      c.copy(reference = x)
    }
    opt[File]('o', "outputDir") required () valueName "<directory>" action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[File]('b', "bam") required () valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    }
    opt[Int]("binSize") required () valueName "<int>" action { (x, c) =>
      c.copy(binSize = x)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val regions = BedRecordList.fromReference(cmdArgs.reference)
      .scatter(cmdArgs.binSize)
      .sorted
      .chrRecords.flatMap { case (chr, regions) =>
        val numberThreads = cmdArgs.threadBinSize / cmdArgs.binSize
        regions.grouped(numberThreads).map(BedRecordList.fromList(_))
      }.par

    for (regionList <- regions) {
      require(regionList.chrRecords.size == 1, "A thread can only have 1 contig")
      val chr = regionList.chrRecords.head._1
      val start = regionList.allRecords.map(_.start).min + 1
      val end = regionList.allRecords.map(_.end).max
    }
  }
}
