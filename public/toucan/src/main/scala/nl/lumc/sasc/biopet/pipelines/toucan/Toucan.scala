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
package nl.lumc.sasc.biopet.pipelines.toucan

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand, Reference }
import nl.lumc.sasc.biopet.extensions.VariantEffectPredictor
import nl.lumc.sasc.biopet.tools.VepNormalizer
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.QScript

/**
 * Pipeline to annotate a vcf file with VEP
 *
 * Created by ahbbollen on 15-1-15.
 */
class Toucan(val root: Configurable) extends QScript with BiopetQScript with Reference {
  def this() = this(null)

  @Input(doc = "Input VCF file", shortName = "Input", required = true)
  var inputVCF: File = _

  def init(): Unit = {

  }

  override def defaults = ConfigUtils.mergeMaps(Map(
    "varianteffectpredictor" -> Map("everything" -> true)
  ), super.defaults)

  //defaults ++= Map("varianteffectpredictor" -> Map("everything" -> true))

  def biopetScript(): Unit = {
    val vep = new VariantEffectPredictor(this)
    vep.input = inputVCF
    vep.output = new File(outputDir, inputVCF.getName.stripSuffix(".gz").stripSuffix(".vcf") + ".vep.vcf")
    vep.isIntermediate = true
    add(vep)

    val normalizer = new VepNormalizer(this)
    normalizer.inputVCF = vep.output
    normalizer.outputVCF = swapExt(vep.output, ".vcf", ".normalized.vcf.gz")
    add(normalizer)
  }

}

object Toucan extends PipelineCommand