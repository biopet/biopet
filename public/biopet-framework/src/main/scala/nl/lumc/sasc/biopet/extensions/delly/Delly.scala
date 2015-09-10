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
package nl.lumc.sasc.biopet.extensions.delly

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.Ln
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk.CatVariants

class Delly(val root: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  @Input(doc = "Input file (bam)")
  var input: File = _

  var workDir: File = _

  @Output(doc = "Delly result VCF")
  var outputVcf: File = _

  var outputName: String = _

  // select the analysis types DEL,DUP,INV,TRA
  var del: Boolean = config("DEL", default = true)
  var dup: Boolean = config("DUP", default = true)
  var inv: Boolean = config("INV", default = true)
  var tra: Boolean = config("TRA", default = true)

  override def init(): Unit = {
    if (outputName == null) outputName = input.getName.stripSuffix(".bam")
    if (outputVcf == null) outputVcf = new File(workDir, outputName + ".delly.vcf")
  }

  def biopetScript() {
    // write the pipeline here
    logger.info("Configuring Delly pipeline")
    var outputFiles: Map[String, File] = Map()
    var vcfFiles: Map[String, File] = Map()

    /// start delly and then copy the vcf into the root directory "<sample>.delly/"
    if (del) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "DEL"
      delly.outputvcf = new File(workDir, outputName + ".delly.del.vcf")
      add(delly)
      vcfFiles += ("DEL" -> delly.outputvcf)
    }
    if (dup) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "DUP"
      delly.outputvcf = new File(workDir, outputName + ".delly.dup.vcf")
      add(delly)
      vcfFiles += ("DUP" -> delly.outputvcf)
    }
    if (inv) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "INV"
      delly.outputvcf = new File(workDir, outputName + ".delly.inv.vcf")
      add(delly)
      vcfFiles += ("INV" -> delly.outputvcf)
    }
    if (tra) {
      val delly = new DellyCaller(this)
      delly.input = input
      delly.analysistype = "TRA"
      delly.outputvcf = new File(workDir, outputName + ".delly.tra.vcf")
      //      vcfFiles += ("TRA" -> delly.outputvcf)
      add(delly)
    }
    // we need to merge the vcf's
    val finalVCF = if (vcfFiles.size > 1) {
      // do merging
      // CatVariants is a $org.broadinstitute.gatk.utils.commandline.CommandLineProgram$;
      //TODO: convert to biopet extension
      val variants = new CatVariants()
      variants.variant = vcfFiles.values.toList
      variants.outputFile = this.outputVcf
      variants.reference = referenceFasta()
      // add the job
      //add(variants)
      Some(outputVcf)
    } else if (vcfFiles.size == 1) {
      // TODO: pretify this
      val ln = Ln(this, vcfFiles.head._2, this.outputVcf, relative = true)
      //add(ln)
      Some(ln.output)
    } else None

    finalVCF.foreach(file => outputFiles += ("vcf" -> file))
  }
}

object Delly extends PipelineCommand {
  override val pipeline = "/nl/lumc/sasc/biopet/extensions/svcallers/Delly/Delly.class"

  def apply(root: Configurable, input: File, workDir: File): Delly = {
    val dellyPipeline = new Delly(root)
    dellyPipeline.input = input
    dellyPipeline.workDir = workDir
    dellyPipeline.init()
    dellyPipeline.biopetScript()
    dellyPipeline
  }

}