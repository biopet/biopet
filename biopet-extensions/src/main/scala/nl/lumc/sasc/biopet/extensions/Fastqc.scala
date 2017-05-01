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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.SemanticVersion
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Extension for fastqc
  * Based on version 0.10.1 and 0.11.2
  */
class Fastqc(val parent: Configurable) extends BiopetCommandLineFunction with Version {

  @Input(doc = "Contaminants", required = false)
  var contaminants: Option[File] = None

  @Input(doc = "Adapters", required = false)
  var adapters: Option[File] = None

  @Input(doc = "Fastq file", shortName = "FQ")
  var fastqfile: File = null

  @Output(doc = "Output", shortName = "out", required = true)
  var output: File = null

  executable = config("exe", default = "fastqc")
  var javaExe: String = config("exe", default = "java", namespace = "java", freeVar = false)
  var kmers: Option[Int] = config("kmers")
  var quiet: Boolean = config("quiet", default = false)
  var noextract: Boolean = config("noextract", default = false)
  var nogroup: Boolean = config("nogroup", default = false)
  var extract: Boolean = config("extract", default = true)

  def versionRegex = """FastQC (.*)""".r
  def versionCommand = executable + " --version"
  override def defaultThreads = 4

  /** Sets contaminants and adapters when not yet set */
  override def beforeGraph() {
    this.jobOutputFile = new File(output.getParentFile, ".fastqc.out")
    this.preProcessExecutable()

    val fastqcDir = new File(executable).getParent

    contaminants = contaminants match {
      // user-defined contaminants file take precedence
      case userDefinedValue @ Some(_) => userDefinedValue
      // otherwise, use default contaminants file (depending on FastQC version)
      case None =>
        val defaultContams = getVersion.flatMap(SemanticVersion.getSemanticVersion) match {
          case Some(v) if v >= SemanticVersion(0, 11, 0) =>
            new File(fastqcDir + "/Configuration/contaminant_list.txt")
          case _ => new File(fastqcDir + "/Contaminants/contaminant_list.txt")
        }
        config("contaminants", default = defaultContams)
    }

    adapters = adapters match {
      // user-defined contaminants file take precedence
      case userDefinedValue @ Some(_) => userDefinedValue
      // otherwise, check if adapters are already present (depending on FastQC version)
      case None =>
        val defaultAdapters = getVersion.flatMap(SemanticVersion.getSemanticVersion) match {
          case Some(v) if v >= SemanticVersion(0, 11, 0) =>
            Option(new File(fastqcDir + "/Configuration/adapter_list.txt"))
          case _ => None
        }
        defaultAdapters.collect { case adp => config("adapters", default = adp) }
    }
  }

  /** return commandline to execute */
  def cmdLine =
    required(executable) +
      optional("--java", javaExe) +
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
