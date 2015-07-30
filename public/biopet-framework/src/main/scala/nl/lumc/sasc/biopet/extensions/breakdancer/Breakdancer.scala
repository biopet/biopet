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
package nl.lumc.sasc.biopet.extensions.breakdancer

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript, PipelineCommand }
import org.broadinstitute.gatk.queue.QScript

/// Breakdancer is actually a mini pipeline executing binaries from the breakdancer package
class Breakdancer(val root: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Argument(doc = "Work directory")
  var workDir: File = _

  var deps: List[File] = Nil

  @Output(doc = "Breakdancer config")
  lazy val configfile: File = {
    new File(workDir, input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.cfg")
  }
  @Output(doc = "Breakdancer raw output")
  lazy val outputraw: File = {
    new File(workDir, input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.tsv")
  }
  @Output(doc = "Breakdancer VCF output")
  lazy val outputvcf: File = {
    new File(workDir, input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".breakdancer.vcf")
  }

  override def init(): Unit = {
  }

  def biopetScript() {
    // read config and set all parameters for the pipeline
    logger.info("Starting Breakdancer configuration")

    val bdcfg = BreakdancerConfig(this, input, this.configfile)
    bdcfg.deps = this.deps
    outputFiles += ("cfg" -> bdcfg.output)
    add(bdcfg)

    val breakdancer = BreakdancerCaller(this, bdcfg.output, this.outputraw)
    add(breakdancer)
    outputFiles += ("tsv" -> breakdancer.output)

    val bdvcf = BreakdancerVCF(this, breakdancer.output, this.outputvcf)
    add(bdvcf)
    outputFiles += ("vcf" -> bdvcf.output)
  }
}

object Breakdancer extends PipelineCommand {
  def apply(root: Configurable, input: File, runDir: File): Breakdancer = {
    val breakdancer = new Breakdancer(root)
    breakdancer.input = input
    breakdancer.workDir = runDir
    breakdancer.init()
    breakdancer.biopetScript()
    breakdancer
  }
}