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
  var useTimeStamp: Boolean = config("use_time_stamp")
  var prefix: String = config("prefix")
  var verbose: Boolean = config("verbose")
  var noCleanup: Boolean = config("no_cleanup")

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
