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

import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList

/**
  * Created by pjvanthof on 22/08/15.
  */
object SquishBed extends ToolCommand {

  case class Args(input: File = null, output: File = null, strandSensitive: Boolean = false)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
    opt[Unit]('s', "strandSensitive") unbounded () valueName "<file>" action { (_, c) =>
      c.copy(strandSensitive = true)
    }
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (!cmdArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + cmdArgs.input)

    logger.info("Start")

    val records = BedRecordList.fromFile(cmdArgs.input)
    val length = records.length
    val refLength = records.combineOverlap.length
    logger.info(s"Total bases: $length")
    logger.info(s"Total bases on reference: $refLength")
    logger.info("Start squishing")
    val squishBed = records.squishBed(cmdArgs.strandSensitive).sorted
    logger.info("Done squishing")
    val squishLength = squishBed.length
    val squishRefLength = squishBed.combineOverlap.length
    logger.info(s"Total bases left: $squishLength")
    logger.info(s"Total bases left on reference: $squishRefLength")
    logger.info(s"Total bases removed from ref: ${refLength - squishRefLength}")
    squishBed.writeToFile(cmdArgs.output)

    logger.info("Done")
  }
}
