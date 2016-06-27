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
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Seqstat function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class SeqStat(val root: Configurable) extends ToolCommandFunction with Summarizable {
  def toolObject = nl.lumc.sasc.biopet.tools.SeqStat

  @Input(doc = "Input FASTQ", shortName = "input", required = true)
  var input: File = null

  @Output(doc = "Output JSON", shortName = "output", required = true)
  var output: File = null

  override def defaultCoreMemory = 4.0

  override def cmdLine = super.cmdLine + required("-i", input) + required("-o", output)

  def summaryStats: Map[String, Any] = {
    val map = ConfigUtils.fileToConfigMap(output)

    ConfigUtils.any2map(map.getOrElse("stats", Map()))
  }

  def summaryFiles: Map[String, File] = Map()

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Array[_], v2: Array[_])           => v1.zip(v2).map(v => resolveSummaryConflict(v._1, v._2, key))
      case (v1: List[_], v2: List[_])             => v1.zip(v2).map(v => resolveSummaryConflict(v._1, v._2, key))
      case (v1: Int, v2: Int) if key == "len_min" => if (v1 < v2) v1 else v2
      case (v1: Int, v2: Int) if key == "len_max" => if (v1 > v2) v1 else v2
      case (v1: Int, v2: Int)                     => v1 + v2
      case (v1: Long, v2: Long)                   => v1 + v2
      case _                                      => v1
    }
  }
}

object SeqStat {
  def apply(root: Configurable, input: File, output: File): SeqStat = {
    val seqstat = new SeqStat(root)
    seqstat.input = input
    seqstat.output = new File(output, input.getName.substring(0, input.getName.lastIndexOf(".")) + ".seqstats.json")
    seqstat
  }

  def apply(root: Configurable, fastqfile: File, outDir: String): SeqStat = {
    val seqstat = new SeqStat(root)
    seqstat.input = fastqfile
    seqstat.output = new File(outDir, fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    seqstat
  }
}