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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.io.Source
import scala.util.matching.Regex

/**
  * FastqSync function class for usage in Biopet pipelines
  *
  * @param parent Configuration object for the pipeline
  */
class FastqSync(val parent: Configurable) extends ToolCommandFunction with Summarizable {

  def toolObject = nl.lumc.sasc.biopet.tools.FastqSync

  /** Original FASTQ file (read 1 or 2) */
  @Input(required = true)
  var refFastq1: File = _

  /** Original FASTQ file (read 1 or 2) */
  @Input(required = true)
  var refFastq2: File = _

  /** "Input read 1 FASTQ file" */
  @Input(required = true)
  var inputFastq1: File = _

  /** Input read 2 FASTQ file */
  @Input(required = true)
  var inputFastq2: File = _

  /** Output read 1 FASTQ file */
  @Output(required = true)
  var outputFastq1: File = _

  /** Output read 2 FASTQ file */
  @Output(required = true)
  var outputFastq2: File = _

  /** Sync statistics */
  @Output(required = true)
  var outputStats: File = _

  override def defaultCoreMemory = 4.0

  override def cmdLine: String =
    super.cmdLine +
      required("-r", refFastq1) +
      required("--ref2", refFastq2) +
      required("-i", inputFastq1) +
      required("-j", inputFastq2) +
      required("-o", outputFastq1) +
      required("-p", outputFastq2) + " > " +
      required(outputStats)

  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = {
    val regex = new Regex(
      """Filtered (\d*) reads from first read file.
                            |Filtered (\d*) reads from second read file.
                            |Synced files contain (\d*) reads.""".stripMargin,
      "R1",
      "R2",
      "RL"
    )

    val (countFilteredR1, countFilteredR2, countRLeft) =
      if (outputStats.exists) {
        val text = Source
          .fromFile(outputStats)
          .getLines()
          .mkString("\n")
        regex.findFirstMatchIn(text) match {
          case None => (0, 0, 0)
          case Some(rmatch) =>
            (rmatch.group("R1").toInt, rmatch.group("R2").toInt, rmatch.group("RL").toInt)
        }
      } else (0, 0, 0)

    Map("num_reads_discarded_R1" -> countFilteredR1,
        "num_reads_discarded_R2" -> countFilteredR2,
        "num_reads_kept" -> countRLeft)
  }

  override def summaryDeps: List[File] = outputStats :: super.summaryDeps

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Int, v2: Int) => v1 + v2
      case _ => v1
    }
  }
}
