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
package nl.lumc.sasc.biopet.extensions.clever

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import org.broadinstitute.gatk.queue.QScript

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
    cleverpipeline.init()
    cleverpipeline.biopetScript()
    cleverpipeline
  }

}