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
      if (summaryLines.contains(TOO_FEW_SNPS)) None
      else if (summaryLines.exists(ASSESSMENT_HEADER.findFirstIn(_).isDefined)) None
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
        Some(chunk)
      }
    } else None
  }
}
