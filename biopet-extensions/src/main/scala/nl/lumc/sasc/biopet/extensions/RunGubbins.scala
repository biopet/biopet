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

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Extension for gubbins
 * See; https://github.com/sanger-pathogens/gubbins
 * No version known
 */
class RunGubbins(val parent: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Contaminants", required = false)
  var startingTree: Option[File] = config("starting_tree")

  @Input(doc = "Fasta file", shortName = "FQ")
  var fastafile: File = _

  @Argument(required = true)
  var outputDirectory: File = null

  executable = config("exe", default = "run_gubbins.py")
  var outgroup: Option[String] = config("outgroup")
  var filterPercentage: Option[String] = config("filter_percentage")
  var treeBuilder: Option[String] = config("tree_builder")
  var iterations: Option[Int] = config("iterations")
  var minSnps: Option[Int] = config("min_snps")
  var convergeMethod: Option[String] = config("converge_method")
  var useTimeStamp: Boolean = config("use_time_stamp", default = false)
  var prefix: Option[String] = config("prefix")
  var verbose: Boolean = config("verbose", default = false)
  var noCleanup: Boolean = config("no_cleanup", default = false)

  @Output
  var outputFiles: List[File] = Nil

  /** Set correct output files */
  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(outputDirectory != null)
    jobLocalDir = outputDirectory
    if (prefix.isEmpty) prefix = Some(fastafile.getName)
    val out: List[String] = List(".recombination_predictions.embl",
      ".recombination_predictions.gff",
      ".branch_base_reconstruction.embl",
      ".summary_of_snp_distribution.vcf",
      ".per_branch_statistics.csv",
      ".filtered_polymorphic_sites.fasta",
      ".filtered_polymorphic_sites.phylip",
      ".final_tree.tre")
    for (t <- out) outputFiles ::= new File(outputDirectory + File.separator + prefix.getOrElse("gubbins") + t)
  }

  /** Return command to execute */
  def cmdLine = required("cd", outputDirectory) + " && " + required(executable) +
    optional("--outgroup", outgroup) +
    optional("--starting_tree", startingTree) +
    optional("--filter_percentage", filterPercentage) +
    optional("--tree_builder", treeBuilder) +
    optional("--iterations", iterations) +
    optional("--min_snps", minSnps) +
    optional("--converge_method", convergeMethod) +
    conditional(useTimeStamp, "--use_time_stamp") +
    optional("--prefix", prefix) +
    conditional(verbose, "--verbose") +
    conditional(noCleanup, "--no_cleanup") +
    required(fastafile)
}
