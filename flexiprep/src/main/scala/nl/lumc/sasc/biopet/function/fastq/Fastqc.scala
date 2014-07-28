package nl.lumc.sasc.biopet.function.fastq

import java.io.File
import scala.sys.process._

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
      conditional(extract, "--extract") +
      conditional(quiet, "--quiet") +
      required("-o", output.getParent()) +
      required(fastqfile)
  }
}

object Fastqc {
  def apply(root:Configurable, fastqfile: File, outDir: String): Fastqc = {
    val fastqcCommand = new Fastqc(root)
    fastqcCommand.fastqfile = fastqfile
    var filename: String = fastqfile.getName()
    if (filename.endsWith(".gz")) filename = filename.substring(0, filename.size - 3)
    if (filename.endsWith(".gzip")) filename = filename.substring(0, filename.size - 5)
    if (filename.endsWith(".fastq")) filename = filename.substring(0, filename.size - 6)
    //if (filename.endsWith(".fq")) filename = filename.substring(0,filename.size - 3)
    fastqcCommand.jobOutputFile = new File(outDir + "/" + filename + "_fastqc.zip")
    fastqcCommand.afterGraph
    return fastqcCommand
  }
}