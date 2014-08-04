package nl.lumc.sasc.biopet.extensions


import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import java.io.File
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

class Cutadapt(val root: Configurable) extends BiopetCommandLineFunction {
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
  if (configContains("adapter")) for (adapter <- config("adapter").getList) opt_adapter += adapter.toString
  var opt_anywhere: Set[String] = Set()
  if (configContains("anywhere")) for (adapter <- config("anywhere").getList) opt_anywhere += adapter.toString
  var opt_front: Set[String] = Set()
  if (configContains("front")) for (adapter <- config("front").getList) opt_front += adapter.toString

  var opt_discard: Boolean = config("discard")
  var opt_minimum_length: String = config("minimum_length", 1)
  var opt_maximum_length: String = config("maximum_length")

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
}
