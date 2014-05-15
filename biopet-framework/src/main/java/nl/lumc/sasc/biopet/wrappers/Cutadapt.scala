package nl.lumc.sasc.biopet.wrappers

import nl.lumc.sasc.biopet.core._
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
import java.io.File
import scala.io.Source._
import scala.sys.process._

class Cutadapt(private var globalConfig: Config) extends CommandLineFunction {
  def this() = this(new Config(Map()))
  analysisName = "cutadapt"
  var config: Config = globalConfig.getAsConfig("cutadapt")
  
  @Input(doc="Cutadapt exe", required=false)
  var cutadapt_exe: File = new File(config.getAsString("exe","/usr/local/bin/cutadapt"))
  @Input(doc="Input fastq file") var fastq_input: File = _ 
  @Input(doc="Fastq contams file", required=false) var contams_file: File = _
  @Output(doc="Output fastq file") var fastq_output: File = _
  
  var opt_adapter: Set[String] = config.getAsListOfStrings("adapter", Nil).to[Set]
  var opt_anywhere: Set[String] = config.getAsListOfStrings("anywhere", Nil).to[Set]
  var opt_front: Set[String] = config.getAsListOfStrings("front", Nil).to[Set]
  
  var opt_discard: Boolean = config.getAsBoolean("discard",false)
  var opt_minimum_length: String = config.getAsInt("minimum_length", 1).toString
  var opt_maximum_length: String = if (config.contains("maximum_length")) config.getAsInt("maximum_length").toString else null 
  
  def commandLine = {
    this.addJobReportBinding("version", getVersion)
    this.getContamsFromFile
    if (!opt_adapter.isEmpty || !opt_anywhere.isEmpty || !opt_front.isEmpty) {
      required(cutadapt_exe) +
      // options
      repeat("-a", opt_adapter) + 
      repeat("-b", opt_anywhere) + 
      repeat("-g", opt_front) + 
      conditional(opt_discard, "--discard") +
      optional("-m", opt_minimum_length) + 
      optional("-M", opt_maximum_length) + 
      // input / output
      required(fastq_input) +
      " > " + required(fastq_output)
    } else {
      "ln -sf " + 
      required(fastq_input) +
      required(fastq_output)
    }
  }
  
  def getContamsFromFile {
    if (contams_file != null) {
      if (contams_file.exists()) {
        for (line <- fromFile(contams_file).getLines) {
          var s: String = line.substring(line.lastIndexOf("\t")+1, line.size)
          opt_adapter += s
          logger.info("Adapter: " + s + " found in: " + fastq_input)
        }
      } else logger.warn("File : " + contams_file + " does not exist")
    }
  }
  
  private var version: String = _
  def getVersion : String = {
    if (version == null) {
      val v: String = (cutadapt_exe + " --version").!!.replace("\n", "")
      if (!v.isEmpty) version = v
    }
    return version
  }
}