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
  override def defaultCoreMemory = 5.0
  def versionCommand = executable + " --version"
  def versionRegex = """Version: (.*)""".r

  @Input(required = false)
  var parameter_fp: Option[File] = config("parameter_fp")

  @Input(required = false)
  var reference_fp: Option[File] = config("reference_fp")

  @Input(required = false)
  var taxonomy_fp: Option[File] = config("taxonomy_fp")

  var assign_taxonomy: Boolean = config("assign_taxonomy", default = false)
  var force: Boolean = config("force", default = false)
  var print_only: Boolean = config("print_only", default = false)
  var suppress_taxonomy_assignment: Boolean = config("suppress_taxonomy_assignment", default = false)

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    jobOutputFile = new File(outputDir, "std.log")
  }

  def cmdLine = executable + required("-f") +
    required("-i", inputFasta) +
    required("-o", outputDir) +
    optional("--reference_fp", reference_fp) +
    optional("--parameter_fp", parameter_fp) +
    optional("--taxonomy_fp", taxonomy_fp) +
    conditional(assign_taxonomy, "--assign_taxonomy") +
    conditional(force, "--force") +
    conditional(print_only, "--print_only") +
    conditional(suppress_taxonomy_assignment, "--suppress_taxonomy_assignment") +
    (if (threads > 1) required("-a") + required("-O", threads) else "")

}
