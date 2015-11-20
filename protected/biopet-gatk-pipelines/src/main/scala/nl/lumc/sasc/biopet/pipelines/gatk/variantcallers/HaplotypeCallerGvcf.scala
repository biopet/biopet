package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.extensions.gatk.broad.GenotypeGVCFs
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Gvcf mode for haplotypecaller */
class HaplotypeCallerGvcf(val root: Configurable) extends Variantcaller {
  val name = "haplotypecaller_gvcf"
  protected def defaultPrio = 5

  def biopetScript() {
    val gvcfFiles = for ((sample, inputBam) <- inputBams) yield {
      val hc = new nl.lumc.sasc.biopet.extensions.gatk.broad.HaplotypeCaller(this)
      hc.input_file = List(inputBam)
      hc.out = new File(outputDir, sample + ".gvcf.vcf.gz")
      hc.useGvcf()
      add(hc)
      hc.out
    }

    val genotypeGVCFs = GenotypeGVCFs(this, gvcfFiles.toList, outputFile)
    add(genotypeGVCFs)
  }
}
