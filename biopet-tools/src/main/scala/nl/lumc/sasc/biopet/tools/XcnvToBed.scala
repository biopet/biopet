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

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import nl.lumc.sasc.biopet.tools.VepNormalizer.Args
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.io.Source

/**
  * Created by Sander Bollen on 24-11-16.
  */
object XcnvToBed extends ToolCommand {

  def main(args: Array[String]): Unit = {
    val commandArgs: Args = new OptParser()
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

    val writer = new PrintWriter(commandArgs.outputBed)
    Source
      .fromFile(commandArgs.inputXcnv)
      .getLines()
      .filter(!_.startsWith("SAMPLE"))
      .map(x => x.split("\t"))
      .map(x => XcnvBedLine(x(0), x(1), x(2)))
      .filter(_.sample == commandArgs.sample)
      .foreach(x => writer.println(x.toString))

    writer.close()
  }

  case class XcnvBedLine(sample: String, cnvType: String, location: String) {
    override def toString: String = {
      val cnv = if (cnvType == "DEL") {
        -1
      } else if (cnvType == "DUP") {
        1
      } else 0

      val locs = location.split(":")
      val chr = locs(0)
      val start = locs(1).split("-")(0)
      val stop = locs(1).split("-")(1)
      s"$chr\t$start\t$stop\t$cnv"
    }
  }

  case class Args(inputXcnv: File = null, outputBed: File = null, sample: String = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    head(
      "Convert a sample track within an XHMM XCNV file to a BED track. Fourt column indicates deletion (-1), normal (0) or duplication (1) of region")

    opt[File]('I', "Input") required () valueName "<xcnv>" action { (x, c) =>
      c.copy(inputXcnv = x)
    } validate { x =>
      if (x.exists) success else failure("Input XCNV not found")
    } text {
      "Input XCNV file"
    }
    opt[File]('O', "Output") required () valueName "<bed>" action { (x, c) =>
      c.copy(outputBed = x)
    } text {
      "Output BED file"
    }
    opt[String]('S', "Sample") required () action { (x, c) =>
      c.copy(sample = x)
    } text {
      "The sample which to select"
    }
  }

}
