package nl.lumc.sasc.biopet.extensions.svcallers

import java.io.File

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.PipelineCommand

class Clever(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference")
  var reference: File = _

  @Argument(doc = "Work directory")
  var workdir: String = _

  @Argument(doc = "Current working directory")
  var cwd: String = _

  override def init() {
  }

  def biopetScript() {
    // write the pipeline here
    logger.info("Starting Clever Pipeline")

    /// start clever and then copy the vcf into the root directory "<sample>.clever/"
    val clever = CleverCaller(this, input, reference, cwd, workdir)
    outputFiles += ("clever_vcf" -> clever.outputvcf)
    add(clever)
  }
}

object Clever extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/extensions/svcallers/Clever/Clever.class"

  def apply(root: Configurable, input: File, runDir: String): Clever = {
    val cleverpipeline = new Clever(root)
    cleverpipeline.input = input
    cleverpipeline.workdir = runDir
    cleverpipeline.init
    cleverpipeline.biopetScript
    return cleverpipeline
  }

}