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
package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 12/4/15.
  */
class PickClosedReferenceOtus(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Version {
  executable = config("exe", default = "pick_closed_reference_otus.py")

  @Input(required = true)
  var inputFasta: File = _

  var outputDir: File = _

  override def defaultThreads = 1
  override def defaultCoreMemory = 20.0
  def versionCommand: String = executable + " --version"
  def versionRegex: List[Regex] = """Version: (.*)""".r :: Nil

  @Input(required = false)
  var parameterFp: Option[File] = config("parameter_fp")

  @Input(required = false)
  var referenceFp: Option[File] = config("reference_fp")

  @Input(required = false)
  var taxonomyFp: Option[File] = config("taxonomy_fp")

  var assignTaxonomy: Boolean = config("assign_taxonomy", default = false)
  var force: Boolean = config("force", default = false)
  var printOnly: Boolean = config("print_only", default = false)
  var suppressTaxonomyAssignment: Boolean = config("suppress_taxonomy_assignment", default = false)

  def otuTable = new File(outputDir, "otu_table.biom")
  def otuMap =
    new File(outputDir,
             "uclust_ref_picked_otus" + File.separator +
               inputFasta.getName
                 .stripSuffix(".fna")
                 .stripSuffix(".fasta")
                 .stripSuffix(".fa") + "_otus.txt")

  @Output
  private var outputFiles: List[File] = Nil

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    jobOutputFile = new File(outputDir, ".std.out")
    outputFiles ::= otuTable
    outputFiles ::= otuMap
  }

  def cmdLine: String =
    executable + required("-f") +
      required("-i", inputFasta) +
      required("-o", outputDir) +
      optional("--reference_fp", referenceFp) +
      optional("--parameter_fp", parameterFp) +
      optional("--taxonomy_fp", taxonomyFp) +
      conditional(assignTaxonomy, "--assign_taxonomy") +
      conditional(force, "--force") +
      conditional(printOnly, "--print_only") +
      conditional(suppressTaxonomyAssignment, "--suppress_taxonomy_assignment") +
      (if (threads > 1) required("-a") + required("-O", threads) else "")

}
