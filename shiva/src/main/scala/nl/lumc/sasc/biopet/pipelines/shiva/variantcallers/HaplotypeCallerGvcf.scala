/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import nl.lumc.sasc.biopet.extensions.gatk.broad
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Gvcf mode for haplotypecaller */
class HaplotypeCallerGvcf(val root: Configurable) extends Variantcaller {
  val name = "haplotypecaller_gvcf"
  protected def defaultPrio = 5

  def biopetScript() {
    val gvcfFiles = for ((sample, inputBam) <- inputBams) yield {
      val hc = broad.HaplotypeCaller.gvcf(this, inputBam, new File(outputDir, sample + ".gvcf.vcf.gz"))
      add(hc)
      hc.out
    }

    val genotypeGVCFs = broad.GenotypeGVCFs(this, gvcfFiles.toList, outputFile)
    add(genotypeGVCFs)
  }
}
