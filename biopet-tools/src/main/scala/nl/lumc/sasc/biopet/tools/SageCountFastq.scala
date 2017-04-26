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

import java.io.{File, PrintWriter}

import htsjdk.samtools.fastq.FastqReader
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConversions._

import scala.collection.{SortedMap, mutable}

object SageCountFastq extends ToolCommand {
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
    val commandArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (!commandArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + commandArgs.input)

    val counts: mutable.Map[String, Long] = mutable.Map()
    val reader: FastqReader = new FastqReader(commandArgs.input)
    var count = 0
    logger.info("Reading fastq file: " + commandArgs.input)

    for (read <- reader.iterator()) {
      val seq = read.getReadString
      if (counts.contains(seq)) counts(seq) += 1
      else counts += (seq -> 1)
      count += 1
      if (count % 1000000 == 0) logger.info(count + " sequences done")
    }

    reader.close()
    logger.info(count + " sequences done")

    logger.info("Sorting")
    val sortedCounts: SortedMap[String, Long] = SortedMap(counts.toArray: _*)

    logger.info("Writting outputfile: " + commandArgs.output)
    val writer = new PrintWriter(commandArgs.output)
    sortedCounts.foreach { case (s, c) => writer.println(s + "\t" + c) }
    writer.close()
  }
}
