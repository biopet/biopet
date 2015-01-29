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

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class RunGubbins(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Contaminants", required = false)
  var startingTree: File = config("starting_tree")

  @Input(doc = "Fasta file", shortName = "FQ")
  var fastafile: File = _

  @Output(doc = "Output", shortName = "out")
  var outputFiles: List[File] = Nil

  @Argument(required = true)
  var outputDirectory: String = _

  executable = config("exe", default = "run_gubbins.py")
  var outgroup: String = config("outgroup")
  var filterPercentage: String = config("filter_percentage")
  var treeBuilder: String = config("tree_builder")
  var iterations: Option[Int] = config("iterations")
  var minSnps: Option[Int] = config("min_snps")
  var convergeMethod: String = config("converge_method")
  var useTimeStamp: Boolean = config("use_time_stamp", default = false)
  var prefix: String = config("prefix")
  var verbose: Boolean = config("verbose", default = false)
  var noCleanup: Boolean = config("no_cleanup", default = false)

  override def afterGraph: Unit = {
    super.afterGraph
    jobLocalDir = new File(outputDirectory)
    if (prefix == null) prefix = fastafile.getName
    val out: List[String] = List(".recombination_predictions.embl",
      ".recombination_predictions.gff",
      ".branch_base_reconstruction.embl",
      ".summary_of_snp_distribution.vcf",
      ".per_branch_statistics.csv",
      ".filtered_polymorphic_sites.fasta",
      ".filtered_polymorphic_sites.phylip",
      ".final_tree.tre")
    for (t <- out) outputFiles ::= new File(outputDirectory + File.separator + prefix + t)
  }

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
