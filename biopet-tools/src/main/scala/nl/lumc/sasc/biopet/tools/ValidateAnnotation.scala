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

import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{FastaUtils, ToolCommand}
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import picard.annotation.GeneAnnotationReader

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by pjvanthof on 10/12/2016.
  */
object ValidateAnnotation extends ToolCommand {
  case class Args(refflatFile: File = null, reference: File = null, failOnError: Boolean = true, gtfFile: Option[File] = None)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('r', "refflatFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(refflatFile = x)
    } text "Refflat file to check"
    opt[File]('g', "gtfFile") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(gtfFile = Some(x))
    } text "Gtf file to check"
    opt[File]('R', "reference") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(reference = x)
    } text "Reference fasta to check vcf file against"
    opt[Unit]("disableFail") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(failOnError = false)
    } text "Do not fail on error. The tool will still exit when encountering an error, but will do so with exit code 0"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val dict = FastaUtils.getCachedDict(cmdArgs.reference)

    try {

      val refflatLines = Source.fromFile(cmdArgs.refflatFile).getLines().toList.sorted

      for (line <- refflatLines) {
        val contig = line.split("\t")(2)
        require(dict.getSequence(contig) != null, s"Contig '$contig' found in refflat but not found on reference")
      }

      cmdArgs.gtfFile.foreach { file =>
        val tempRefflat = File.createTempFile("temp.", ".refflat")
        tempRefflat.deleteOnExit()
        GtfToRefflat.gtfToRefflat(file, tempRefflat, Some(cmdArgs.reference))

        val tempRefflatLines = Source.fromFile(tempRefflat).getLines().toList.sorted

        for ((line1, line2) <- refflatLines.zip(tempRefflatLines)) {
          require(line1 == line2, "Refflat and gtf contain different information")
        }
      }
    } catch {
      case e: IllegalArgumentException =>
        if (cmdArgs.failOnError) throw e
        else logger.error(e.getMessage)
    }

    logger.info("Done")
  }
}
