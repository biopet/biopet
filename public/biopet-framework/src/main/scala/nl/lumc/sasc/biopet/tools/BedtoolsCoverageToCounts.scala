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

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ ToolCommand, ToolCommandFuntion }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.{ mutable, SortedMap }
import scala.io.Source

class BedtoolsCoverageToCounts(val root: Configurable) extends ToolCommandFuntion {
  javaMainClass = getClass.getName

  @Input(doc = "Input fasta", shortName = "input", required = true)
  var input: File = _

  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _

  override def defaultCoreMemory = 3.0

  override def cmdLine = super.cmdLine +
    required("-I", input) +
    required("-o", output)
}

object BedtoolsCoverageToCounts extends ToolCommand {
  case class Args(input: File = null, output: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    if (!commandArgs.input.exists) throw new IllegalStateException("Input file not found, file: " + commandArgs.input)

    val counts: mutable.Map[String, Long] = mutable.Map()
    for (line <- Source.fromFile(commandArgs.input).getLines()) {
      val values = line.split("\t")
      val gene = values(3)
      val count = values(6).toLong
      if (counts.contains(gene)) counts(gene) += count
      else counts += gene -> count
    }

    val sortedCounts: SortedMap[String, Long] = SortedMap(counts.toArray: _*)

    val writer = new PrintWriter(commandArgs.output)
    for ((seq, count) <- sortedCounts) {
      if (count > 0) writer.println(seq + "\t" + count)
    }
    writer.close()
  }
}