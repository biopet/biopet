package nl.lumc.sasc.biopet.extensions

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._

class Fastqc(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Contaminants", required = false)
  var contaminants: File = _

  @Input(doc = "Fastq file", shortName = "FQ")
  var fastqfile: File = _

  @Output(doc = "Output", shortName = "out")
  var output: File = _

  executable = config("exe", default = "fastqc")
  var java_exe: String = config("exe", default = "java", submodule = "java")
  var kmers: Option[Int] = config("kmers")
  var quiet: Boolean = config("quiet")
  var noextract: Boolean = config("noextract")
  var nogroup: Boolean = config("nogroup")
  var extract: Boolean = config("extract", default = true)

  override val versionRegex = """FastQC (.*)""".r
  override def versionCommand = executable + " --version"
  override val defaultThreads = 4

  override def afterGraph {
    this.checkExecutable
    if (contaminants == null) {
      val fastqcDir = executable.substring(0, executable.lastIndexOf("/"))
      contaminants = new File(fastqcDir + "/Contaminants/contaminant_list.txt")
    }
  }

  def cmdLine = required(executable) +
    optional("--java", java_exe) +
    optional("--threads", threads) +
    optional("--contaminants", contaminants) +
    optional("--kmers", kmers) +
    conditional(nogroup, "--nogroup") +
    conditional(noextract, "--noextract") +
    conditional(extract, "--extract") +
    conditional(quiet, "--quiet") +
    required("-o", output.getParent()) +
    required(fastqfile)
}
