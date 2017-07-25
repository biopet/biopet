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

import nl.lumc.sasc.biopet.utils.annotation.Feature
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, FastaUtils, ToolCommand}

import scala.io.Source

/**
  * Created by pjvanthof on 10/12/2016.
  */
object ValidateAnnotation extends ToolCommand {
  case class Args(refflatFile: Option[File] = None,
                  reference: File = null,
                  failOnError: Boolean = true,
                  gtfFiles: List[File] = Nil)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('r', "refflatFile") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(refflatFile = Some(x))
    } text "Refflat file to check"
    opt[File]('g', "gtfFile") valueName "<file>" action { (x, c) =>
      c.copy(gtfFiles = x :: c.gtfFiles)
    } text "Gtf files to check"
    opt[File]('R', "reference") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(reference = x)
    } text "Reference fasta to check vcf file against"
    opt[Unit]("disableFail") maxOccurs 1 valueName "<file>" action { (_, c) =>
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

      val refflatLines = cmdArgs.refflatFile.map(Source.fromFile(_).getLines().toList.sorted)

      for (line <- refflatLines.getOrElse(Nil)) {
        val contig = line.split("\t")(2)
        require(dict.getSequence(contig) != null,
                s"Contig '$contig' found in refflat but not found on reference")
      }

      cmdArgs.gtfFiles.distinct.foreach { file =>
        refflatLines match {
          case Some(lines) =>
            val tempRefflat = File.createTempFile("temp.", ".refflat")
            tempRefflat.deleteOnExit()
            GtfToRefflat.gtfToRefflat(file, tempRefflat, Some(cmdArgs.reference))

            val tempRefflatLines = Source.fromFile(tempRefflat).getLines().toList.sorted
            for ((line1, line2) <- lines.zip(tempRefflatLines)) {
              require(line1 == line2, "Refflat and gtf contain different information")
            }
          case _ =>
            Source
              .fromFile(file)
              .getLines()
              .filter(!_.startsWith("#"))
              .map(Feature.fromLine)
              .foreach { feature =>
                require(
                  dict.getSequence(feature.contig) != null,
                  s"Contig '${feature.contig}' found in gtf/gff but not found on reference: $file")
              }
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
