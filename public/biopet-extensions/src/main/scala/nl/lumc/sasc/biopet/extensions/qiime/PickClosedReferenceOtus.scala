package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
 * Created by pjvan_thof on 12/4/15.
 */
class PickClosedReferenceOtus(val root: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "pick_closed_reference_otus.py")

  @Input(required = true)
  var inputFasta: File = _

  var outputDir: File = null

  override def defaultThreads = 2
  override def defaultCoreMemory = 10.0
  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

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
  def otuMap = new File(outputDir, "uclust_ref_picked_otus" + File.separator + "seqs_otus.txt")

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    jobOutputFile = new File(outputDir, ".std.out")
    outputFiles ::= otuTable
    outputFiles ::= otuMap
  }

  def cmdLine = executable + required("-f") +
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
