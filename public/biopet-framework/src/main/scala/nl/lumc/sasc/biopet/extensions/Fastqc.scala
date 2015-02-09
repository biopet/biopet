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

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

class Fastqc(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Contaminants", required = false)
  var contaminants: Option[File] = None

  @Input(doc = "Adapters", required = false)
  var adapters: Option[File] = None

  @Input(doc = "Fastq file", shortName = "FQ")
  var fastqfile: File = null

  @Output(doc = "Output", shortName = "out")
  var output: File = null

  executable = config("exe", default = "fastqc")
  var java_exe: String = config("exe", default = "java", submodule = "java", freeVar = false)
  var kmers: Option[Int] = config("kmers")
  var quiet: Boolean = config("quiet", default = false)
  var noextract: Boolean = config("noextract", default = false)
  var nogroup: Boolean = config("nogroup", default = false)
  var extract: Boolean = config("extract", default = true)

  override val versionRegex = """FastQC (.*)""".r
  override def versionCommand = executable + " --version"
  override val defaultThreads = 4

  override def afterGraph {
    this.checkExecutable

    val fastqcDir = executable.substring(0, executable.lastIndexOf("/"))

    contaminants = contaminants match {
      // user-defined contaminants file take precedence
      case userDefinedValue @ Some(_) => userDefinedValue
      // otherwise, use default contaminants file (depending on FastQC version)
      case None =>
        val defaultContams = getVersion match {
          case "v0.11.2" => new File(fastqcDir + "/Configuration/contaminant_list.txt")
          case _         => new File(fastqcDir + "/Contaminants/contaminant_list.txt")
        }
        config("contaminants", default = defaultContams)
    }

    adapters = adapters match {
      // user-defined contaminants file take precedence
      case userDefinedValue @ Some(_) => userDefinedValue
      // otherwise, check if adapters are already present (depending on FastQC version)
      case None =>
        val defaultAdapters = getVersion match {
          case "v0.11.2" => Some(new File(fastqcDir + "/Configuration/adapter_list.txt"))
          case _         => None
        }
        defaultAdapters.collect { case adp => config("adapters", default = adp) }
    }
  }

  def cmdLine = required(executable) +
    optional("--java", java_exe) +
    optional("--threads", threads) +
    optional("--contaminants", contaminants) +
    optional("--adapters", adapters) +
    optional("--kmers", kmers) +
    conditional(nogroup, "--nogroup") +
    conditional(noextract, "--noextract") +
    conditional(extract, "--extract") +
    conditional(quiet, "--quiet") +
    required("-o", output.getParent) +
    required(fastqfile)
}
