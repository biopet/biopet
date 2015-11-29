package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad

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
