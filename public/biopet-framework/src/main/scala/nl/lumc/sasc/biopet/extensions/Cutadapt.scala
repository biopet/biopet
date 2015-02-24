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
package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

import scala.collection.mutable
import scala.io.Source

class Cutadapt(val root: Configurable) extends BiopetCommandLineFunction with Summarizable {
  @Input(doc = "Input fastq file")
  var fastq_input: File = _

  @Output(doc = "Output fastq file")
  var fastq_output: File = _

  @Output(doc = "Output statistics file")
  var stats_output: File = _

  executable = config("exe", default = "cutadapt")
  override def versionCommand = executable + " --version"
  override val versionRegex = """(.*)""".r

  var default_clip_mode: String = config("default_clip_mode", default = "3")
  var opt_adapter: Set[String] = Set()
  if (config.contains("adapter")) for (adapter <- config("adapter").asList) opt_adapter += adapter.toString
  var opt_anywhere: Set[String] = Set()
  if (config.contains("anywhere")) for (adapter <- config("anywhere").asList) opt_anywhere += adapter.toString
  var opt_front: Set[String] = Set()
  if (config.contains("front")) for (adapter <- config("front").asList) opt_front += adapter.toString

  var opt_discard: Boolean = config("discard", default = false)
  var opt_minimum_length: Int = config("minimum_length", 1)
  var opt_maximum_length: Option[Int] = config("maximum_length")

  def cmdLine = required(executable) +
    // options
    repeat("-a", opt_adapter) +
    repeat("-b", opt_anywhere) +
    repeat("-g", opt_front) +
    conditional(opt_discard, "--discard") +
    optional("-m", opt_minimum_length) +
    optional("-M", opt_maximum_length) +
    // input / output
    required(fastq_input) +
    required("--output", fastq_output) +
    " > " + required(stats_output)

  def summaryStats: Map[String, Any] = {
    val trimR = """.*Trimmed reads: *(\d*) .*""".r
    val tooShortR = """.*Too short reads: *(\d*) .*""".r
    val tooLongR = """.*Too long reads: *(\d*) .*""".r
    val adapterR = """Adapter '([C|T|A|G]*)'.*trimmed (\d*) times.""".r

    val stats: mutable.Map[String, Int] = mutable.Map("trimmed" -> 0, "tooshort" -> 0, "toolong" -> 0)
    val adapter_stats: mutable.Map[String, Int] = mutable.Map()

    if (stats_output.exists) for (line <- Source.fromFile(stats_output).getLines) {
      line match {
        case trimR(m)                 => stats += ("trimmed" -> m.toInt)
        case tooShortR(m)             => stats += ("tooshort" -> m.toInt)
        case tooLongR(m)              => stats += ("toolong" -> m.toInt)
        case adapterR(adapter, count) => adapter_stats += (adapter -> count.toInt)
        case _                        =>
      }
    }

    Map("num_reads_affected" -> stats("trimmed"),
      "num_reads_discarded_too_short" -> stats("tooshort"),
      "num_reads_discarded_too_long" -> stats("toolong"),
      "adapters" -> adapter_stats.toMap
    )
  }

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Int, v2: Int) => v1 + v2
      case _                  => v1
    }
  }

  def summaryFiles: Map[String, File] = Map()
}
