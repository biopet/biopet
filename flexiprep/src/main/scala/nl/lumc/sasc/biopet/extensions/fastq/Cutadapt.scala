package nl.lumc.sasc.biopet.extensions.fastq

import java.io.File
import scala.io.Source._
import scala.sys.process._

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.function.Ln

class Cutadapt(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Input fastq file")
  var fastq_input: File = _

  @Input(doc = "Fastq contams file", required = false)
  var contams_file: File = _

  @Output(doc = "Output fastq file")
  var fastq_output: File = _

  @Output(doc = "Output statistics file")
  var stats_output: File = _

  executable = config("exe", default = "cutadapt")
  override def versionCommand = executable + " --version"
  override val versionRegex = """(.*)""".r

  var default_clip_mode: String = config("default_clip_mode", default = "3")
  var opt_adapter: Set[String] = Set() + config("adapter")
  var opt_anywhere: Set[String] = Set() + config("anywhere")
  var opt_front: Set[String] = Set() + config("front")

  var opt_discard: Boolean = config("discard")
  var opt_minimum_length: String = config("minimum_length", 1)
  var opt_maximum_length: String = config("maximum_length")

  override def beforeCmd() {
    getContamsFromFile
  }

  def cmdLine = {
    if (!opt_adapter.isEmpty || !opt_anywhere.isEmpty || !opt_front.isEmpty) {
      analysisName = getClass.getName
      required(executable) +
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
    } else {
      analysisName = getClass.getSimpleName + "-ln"
      val lnOut = new Ln(this)
      lnOut.in = new java.io.File(required(fastq_input))
      lnOut.out = new java.io.File(required(fastq_output))
      lnOut.relative = true
      lnOut.cmd
    }
  }

  def getContamsFromFile {
    if (contams_file != null) {
      if (contams_file.exists()) {
        for (line <- fromFile(contams_file).getLines) {
          var s: String = line.substring(line.lastIndexOf("\t") + 1, line.size)
          if (default_clip_mode == "3") opt_adapter += s
          else if (default_clip_mode == "5") opt_front += s
          else if (default_clip_mode == "both") opt_anywhere += s
          else {
            opt_adapter += s
            logger.warn("Option default_clip_mode should be '3', '5' or 'both', falling back to default: '3'")
          }
          logger.info("Adapter: " + s + " found in: " + fastq_input)
        }
      } else logger.warn("File : " + contams_file + " does not exist")
    }
  }
  
  def getSummary: Json = {
    return jNull
  }
}

object Cutadapt {
  def mergeSummarys(jsons:List[Json]): Json = {
    return jNull
  }
}
