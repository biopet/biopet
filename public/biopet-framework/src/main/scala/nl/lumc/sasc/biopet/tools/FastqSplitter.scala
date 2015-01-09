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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import htsjdk.samtools.fastq.{ AsyncFastqWriter, FastqReader, BasicFastqWriter }
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import scala.collection.JavaConversions._

class FastqSplitter(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName

  @Input(doc = "Input fastq", shortName = "input", required = true)
  var input: File = _

  @Output(doc = "Output fastq files", shortName = "output", required = true)
  var output: List[File] = Nil

  override val defaultVmem = "8G"
  memoryLimit = Option(4.0)

  override def commandLine = super.commandLine + required("-I", input) + repeat("-o", output)
}

object FastqSplitter extends ToolCommand {
  case class Args(inputFile: File = null, outputFile: List[File] = Nil) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () valueName ("<file>") action { (x, c) =>
      c.copy(inputFile = x)
    } text ("out is a required file property")
    opt[File]('o', "output") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(outputFile = x :: c.outputFile)
    } text ("out is a required file property")
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val groupsize = 100
    val output = for (file <- commandArgs.outputFile) yield new AsyncFastqWriter(new BasicFastqWriter(file), groupsize)
    val reader = new FastqReader(commandArgs.inputFile)

    logger.info("Starting to split fatsq file: " + commandArgs.inputFile)
    logger.info("Output files: " + commandArgs.outputFile.mkString(", "))

    while (reader.hasNext) {
      for (writer <- output) {
        for (t <- 1 to groupsize if reader.hasNext) {
          writer.write(reader.next())
        }
      }
    }
    for (writer <- output) writer.close
  }
}
