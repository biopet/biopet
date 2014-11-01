/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 *
 * This tool is a port of a Python implementation written by Martijn Vermaat[1]
 *
 * [1] https://github.com/martijnvermaat/bio-playground/blob/master/sync-paired-end-reads/sync_paired_end_reads.py
 */
package nl.lumc.sasc.biopet.tools

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * FastqSync function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class FastqSync(val root: Configurable) extends BiopetJavaCommandLineFunction {

  javaMainClass = getClass.getName

}

object FastqSync extends ToolCommand {

  case class Args() extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Sync paired-end FASTQ files
      """.stripMargin)

  }

  /**
   * Parses the command line argument
   *
   * @param args Array of arguments
   * @return
   */
  def parseArgs(args: Array[String]): Args = new OptParser()
    .parse(args, Args())
    .getOrElse(sys.exit(1))

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

  }
}

