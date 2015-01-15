package nl.lumc.sasc.biopet.pipelines.toucan

import nl.lumc.sasc.biopet.core.{ PipelineCommand, BiopetQScript }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.VariantEffectPredictor
import nl.lumc.sasc.biopet.tools.VEPNormalizer
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }

/**
 * Created by ahbbollen on 15-1-15.
 */
class Toucan(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "Input VCF file", shortName = "Input", required = true)
  var inputVCF: File = _

  def init(): Unit = {

  }

  defaults ++= Map("varianteffectpredictor" -> Map("everything" -> true))

  def biopetScript(): Unit = {
    val vep = new VariantEffectPredictor(this)
    vep.input = inputVCF
    vep.output = outputDir + inputVCF.getName.stripSuffix(".gz").stripSuffix(".vcf") + ".vep.vcf"
    vep.isIntermediate = true
    add(vep)

    val normalizer = new VEPNormalizer(this)
    normalizer.inputVCF = vep.output
    normalizer.outputVCF = swapExt(vep.output, ".vcf", ".normalized.vcf.gz")
    add(normalizer)
  }

}

object Toucan extends PipelineCommand