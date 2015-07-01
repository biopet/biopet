/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad.{ ApplyRecalibration, VariantAnnotator, VariantRecalibrator }
import org.broadinstitute.gatk.queue.QScript

class GatkVariantRecalibration(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "input vcf file", shortName = "I")
  var inputVcf: File = _

  @Input(doc = "input vcf file", shortName = "BAM", required = false)
  var bamFiles: List[File] = Nil

  @Output(doc = "output vcf file", shortName = "out")
  var outputVcf: File = _

  def init() {
    require(inputVcf != null, "Missing Output directory on gatk module")
  }

  def biopetScript() {
    var vcfFile: File = if (!bamFiles.isEmpty) addVariantAnnotator(inputVcf, bamFiles, outputDir) else inputVcf
    vcfFile = addSnpVariantRecalibrator(vcfFile, outputDir)
    vcfFile = addIndelVariantRecalibrator(vcfFile, outputDir)
  }

  def addSnpVariantRecalibrator(inputVcf: File, dir: File): File = {
    val snpRecal = VariantRecalibrator(this, inputVcf, swapExt(dir, inputVcf, ".vcf", ".indel.recal"),
      swapExt(dir, inputVcf, ".vcf", ".indel.tranches"), indel = false)
    if (!snpRecal.resource.isEmpty) {
      add(snpRecal)

      val snpApply = ApplyRecalibration(this, inputVcf, swapExt(dir, inputVcf, ".vcf", ".indel.recal.vcf"),
        snpRecal.recal_file, snpRecal.tranches_file, indel = false)
      add(snpApply)

      return snpApply.out
    } else {
      logger.warn("Skipped snp Recalibration, resource is missing")
      return inputVcf
    }
  }

  def addIndelVariantRecalibrator(inputVcf: File, dir: File): File = {
    val indelRecal = VariantRecalibrator(this, inputVcf, swapExt(dir, inputVcf, ".vcf", ".indel.recal"),
      swapExt(dir, inputVcf, ".vcf", ".indel.tranches"), indel = true)
    if (!indelRecal.resource.isEmpty) {
      add(indelRecal)

      val indelApply = ApplyRecalibration(this, inputVcf, swapExt(dir, inputVcf, ".vcf", ".indel.recal.vcf"),
        indelRecal.recal_file, indelRecal.tranches_file, indel = true)
      add(indelApply)

      return indelApply.out
    } else {
      logger.warn("Skipped indel Recalibration, resource is missing")
      return inputVcf
    }
  }

  def addVariantAnnotator(inputvcf: File, bamfiles: List[File], dir: File): File = {
    val variantAnnotator = VariantAnnotator(this, inputvcf, bamfiles, swapExt(dir, inputvcf, ".vcf", ".anotated.vcf"))
    add(variantAnnotator)
    return variantAnnotator.out
  }
}

object GatkVariantRecalibration extends PipelineCommand
