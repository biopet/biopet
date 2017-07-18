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
package nl.lumc.sasc.biopet.tools.flagstat

import java.io.{File, PrintWriter}

import htsjdk.samtools.{SAMRecord, SamReaderFactory}
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ConfigUtils, ToolCommand}

import scala.collection.JavaConversions._
import scala.collection.mutable

object BiopetFlagstat extends ToolCommand {

  case class Args(inputFile: File = null,
                  outputFile: Option[File] = None,
                  summaryFile: Option[File] = None,
                  region: Option[String] = None)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "inputFile") required () valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "input bam file"
    opt[File]('o', "outputFile") valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "output file"
    opt[File]('s', "summaryFile") valueName "<file>" action { (x, c) =>
      c.copy(summaryFile = Some(x))
    } text "summary output file"
    opt[String]('r', "region") valueName "<chr:start-stop>" action { (x, c) =>
      c.copy(region = Some(x))
    }
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val inputSam = SamReaderFactory.makeDefault.open(commandArgs.inputFile)
    val iterSam =
      if (commandArgs.region.isEmpty) inputSam.iterator
      else {
        val regionRegex = """(.*):(.*)-(.*)""".r
        commandArgs.region.get match {
          case regionRegex(chr, start, stop) => inputSam.query(chr, start.toInt, stop.toInt, false)
          case _ => sys.error("Region wrong format")
        }
      }

    val flagstatCollector = new FlagstatCollector
    flagstatCollector.loadDefaultFunctions()
    flagstatCollector.loadQualityFunctions()

    logger.info("Start reading file: " + commandArgs.inputFile)
    for (record <- iterSam) {
      if (flagstatCollector.readsCount % 1e6 == 0 && flagstatCollector.readsCount > 0)
        logger.info("Reads processed: " + flagstatCollector.readsCount)
      flagstatCollector.loadRecord(record)
    }

    commandArgs.summaryFile.foreach(file => flagstatCollector.writeSummaryTofile(file))

    commandArgs.outputFile match {
      case Some(file) => flagstatCollector.writeReportToFile(file)
      case _ => println(flagstatCollector.report)
    }
  }
}
