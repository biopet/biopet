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
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.fastq.{AsyncFastqWriter, BasicFastqWriter, FastqReader}
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.util.matching.Regex
import scala.collection.JavaConversions._

/**
  * Created by pjvan_thof on 28-10-16.
  */
object FastqFilter extends ToolCommand {

  /**
    * Arg for commandline program
    * @param inputFile input fastq file
    * @param outputFile output fastq files
    */
  case class Args(inputFile: File = null, outputFile: File = null, idRegex: Option[Regex] = None)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "inputFile") required () valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "Path to input file"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "Path to output file"
    opt[String]("idRegex") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(idRegex = Some(x.r))
    } text "Regex to match ID"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val reader = new FastqReader(cmdArgs.inputFile)
    val writer = new AsyncFastqWriter(new BasicFastqWriter(cmdArgs.outputFile), 10000)

    var total = 0
    var kept = 0
    for (record <- reader.iterator()) {
      if (cmdArgs.idRegex.forall(
            _.findFirstIn(record.getReadHeader.takeWhile(_ != ' ')).isDefined)) {
        writer.write(record)
        kept += 1
      }
      total += 1
      if (total % 100000 == 0) logger.info(s"Total reads: $total,  reads left: $kept")
    }
    logger.info(s"Total reads: $total,  reads left: $kept")

    writer.close()
    reader.close()

    logger.info("Done")
  }
}
