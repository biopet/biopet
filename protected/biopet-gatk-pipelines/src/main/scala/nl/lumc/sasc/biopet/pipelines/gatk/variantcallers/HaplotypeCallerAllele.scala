package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad

/** Allele mode for Haplotypecaller */
class HaplotypeCallerAllele(val root: Configurable) extends Variantcaller {
  val name = "haplotypecaller_allele"
  protected def defaultPrio = 5

  def biopetScript() {
    val hc = broad.HaplotypeCaller(this, inputBams.values.toList, outputFile)
    hc.alleles = config("input_alleles")
    hc.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
    add(hc)
  }
}
