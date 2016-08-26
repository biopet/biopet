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
package nl.lumc.sasc.biopet.pipelines.gwastest.impute

import java.io.File

import nl.lumc.sasc.biopet.utils.Logging

import Spec.SpecDecoderOps

import scala.io.Source

/**
 * Created by pjvan_thof on 3/25/16.
 */
object ImputeOutput {

  case class Chunk(chromosome: String, summary: File, warnings: File,
                   gens: File, gensInfo: File, gensInfoBySample: File)

  def expandChunk(chromosome: String, basename: String) = Chunk(
    chromosome,
    new File(basename + ".gens_summary"),
    new File(basename + ".gens_warnings"),
    new File(basename + ".gens"),
    new File(basename + ".gens_info"),
    new File(basename + ".gens_info_by_sample")
  )

  def readSpecsFile(specsFile: File, validate: Boolean = true): List[Chunk] = {
    val content = Source.fromFile(specsFile).mkString
    val chunks = content.decode[List[Spec.ImputeOutput]]
      .map(x => expandChunk(x.chromosome, specsFile.getParent + File.separator + x.name.split(File.separator).last))
    chunks.flatMap(validateChunk(_, validate))
  }

  val ASSESSMENT_HEADER = "-{32}\\n Imputation accuracy assessment \\n-{32}".r
  val TOO_FEW_SNPS = "There are no SNPs in the imputation interval, so " +
    "there is nothing for IMPUTE2 to analyze; the program will quit now."
  val NO_TYPE_2 = "ERROR: There are no type 2 SNPs after applying the command-line settings for this run, which makes it impossible to perform imputation. One possible reason is that you have specified an analysis interval (-int) that contains reference panel SNPs but not inference panel SNPs -- e.g., this can happen at the ends of chromosomes. Another possibility is that your genotypes and the reference panel are mapped to different genome builds, which can lead the same SNPs to be assigned different positions in different panels. If you need help fixing this error, please contact the authors."
  val NO_ANALYSIS = "Your current command-line settings imply that there will not be any SNPs in the output file, so IMPUTE2 will not perform any analysis or print output files."
  val CORRECT_LOG = " Imputation accuracy assessment "

  def validateChunk(chunk: Chunk, raiseErrors: Boolean = true): Option[Chunk] = {

    def addError(msg: String) = if (raiseErrors) Logging.addError(msg) else Logging.logger.warn(msg)

    if (!chunk.summary.exists()) {
      Logging.addError(s"Summary file '${chunk.summary}' does not exist, please check Impute output")
      None
    } else if (!chunk.warnings.exists()) {
      addError(s"Warnings file '${chunk.warnings}' does not exist, please check Impute output")
      None
    } else if (chunk.summary.canRead()) {
      val summaryReader = Source.fromFile(chunk.summary)
      val summaryLines = summaryReader.getLines().toList
      summaryReader.close()
      if (summaryLines.contains(NO_ANALYSIS)) None
      else if (summaryLines.contains(TOO_FEW_SNPS)) None
      else if (summaryLines.contains(NO_TYPE_2)) {
        Logging.logger.warn(s"No Type 2 SNPs found, skipping this chunk: '${chunk.summary}'")
        None
      } else if (summaryLines.exists(ASSESSMENT_HEADER.findFirstIn(_).isDefined)) None
      else if (!chunk.gens.exists()) {
        addError(s"Gens file '${chunk.gens}' does not exist, please check Impute output")
        None
      } else if (!chunk.gensInfo.exists()) {
        addError(s"GensInfo file '${chunk.gensInfo}' does not exist, please check Impute output")
        None
      } else if (!chunk.gensInfoBySample.exists()) {
        addError(s"GensInfoBySample file '${chunk.gensInfoBySample}' does not exist, please check Impute output")
        None
      } else {
        if (!summaryLines.contains(CORRECT_LOG)) {
          Logging.logger.warn(s"Impute says it did not run but the gens files are there, pipeline will still continue")
          Logging.logger.warn(s"      Please check: ${chunk.summary}")
        }
        Some(chunk)
      }
    } else None
  }
}
