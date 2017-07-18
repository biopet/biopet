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

import htsjdk.samtools.fastq.{AsyncFastqWriter, BasicFastqWriter, FastqReader, FastqRecord}
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}
import nl.lumc.sasc.biopet.utils.config.Configurable

object PrefixFastq extends ToolCommand {

  /**
    * Args for commandline program
    * @param input input fastq file (can be zipper)
    * @param output output fastq file (can be zipper)
    * @param seq Seq to prefix the reads with
    */
  case class Args(input: File = null, output: File = null, seq: String = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('i', "input") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
    opt[String]('s', "seq") required () maxOccurs 1 valueName "<prefix seq>" action { (x, c) =>
      c.copy(seq = x)
    }
  }

  /**
    * Program will prefix reads with a given seq
    *
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val writer = new AsyncFastqWriter(new BasicFastqWriter(cmdArgs.output), 3000)
    val reader = new FastqReader(cmdArgs.input)

    var counter = 0
    while (reader.hasNext) {
      val read = reader.next()

      val maxQuality = read.getBaseQualityString.max

      val readHeader = read.getReadHeader
      val readSeq = cmdArgs.seq + read.getReadString
      val baseQualityHeader = read.getBaseQualityHeader
      val baseQuality = Array
        .fill(cmdArgs.seq.length)(maxQuality)
        .mkString + read.getBaseQualityString

      writer.write(new FastqRecord(readHeader, readSeq, baseQualityHeader, baseQuality))

      counter += 1
      if (counter % 1e6 == 0) logger.info(counter + " reads processed")
    }

    if (counter % 1e6 != 0) logger.info(counter + " reads processed")
    writer.close()
    reader.close()
    logger.info("Done")
  }
}
