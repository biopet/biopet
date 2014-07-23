package nl.lumc.sasc.biopet.function.fastq

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File
import scala.sys.process._

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

  override val versionRegex = """FastQC (.*)""".r
  override val defaultThreads = 4

  override def afterGraph {
    this.checkExecutable
    val fastqcDir = executable.substring(0, executable.lastIndexOf("/"))
    if (contaminants == null) contaminants = new File(fastqcDir + "/Contaminants/contaminant_list.txt")
  }

  override def versionCommand = executable + " --version"

  def cmdLine = {
    required(executable) +
      optional("--java", java_exe) +
      optional("--threads", threads) +
      optional("--contaminants", contaminants) +
      optional("--kmers", kmers) +
      conditional(nogroup, "--nogroup") +
      conditional(noextract, "--noextract") +
      conditional(quiet, "--quiet") +
      required("-o", output.getParent()) +
      required(fastqfile) +
      required(" > ", output, escape = false)
  }
}