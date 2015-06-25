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
package nl.lumc.sasc.biopet.extensions.pindel

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.queue.QScript
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

/// Pindel is actually a mini pipeline executing binaries from the pindel package
class Pindel(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference Fasta file")
  var reference: File = _

  @Argument(doc = "Work directory")
  var workdir: String = _

  //  @Output(doc = "Pindel VCF output")
  //  lazy val outputvcf: File = {
  //    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".pindel.vcf")
  //  }

  @Output(doc = "Pindel config")
  lazy val configfile: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".pindel.cfg")
  }
  @Output(doc = "Pindel raw output")
  lazy val outputvcf: File = {
    new File(workdir + "/" + input.getName.substring(0, input.getName.lastIndexOf(".bam")) + ".pindel.vcf")
  }

  override def init() {
  }

  def biopetScript() {
    // read config and set all parameters for the pipeline
    logger.info("Starting Pindel configuration")

    val cfg = PindelConfig(this, input, this.configfile)
    outputFiles += ("pindel_cfg" -> cfg.output)
    add(cfg)

    val output: File = this.outputvcf
    val pindel = PindelCaller(this, cfg.output, output)
    add(pindel)
    outputFiles += ("pindel_tsv" -> pindel.output)

    //    val output_vcf: File = this.outputvcf
    // convert this tsv to vcf using the python script

  }

  //  private def swapExtension(inputFile: String) = inputFile.substring(0, inputFile.lastIndexOf(".bam")) + ".pindel.tsv"
}

object Pindel extends PipelineCommand {
  def apply(root: Configurable, input: File, reference: File, runDir: String): Pindel = {
    val pindel = new Pindel(root)
    pindel.input = input
    pindel.reference = reference
    pindel.workdir = runDir
    // run the following for activating the pipeline steps
    pindel.init
    pindel.biopetScript
    return pindel
  }
}