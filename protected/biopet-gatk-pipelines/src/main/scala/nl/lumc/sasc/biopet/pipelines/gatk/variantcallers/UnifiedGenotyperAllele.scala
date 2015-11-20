package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Allele mode for GenotyperAllele */
class UnifiedGenotyperAllele(val root: Configurable) extends Variantcaller {
  val name = "unifiedgenotyper_allele"
  protected def defaultPrio = 9

  def biopetScript() {
    val ug = new nl.lumc.sasc.biopet.extensions.gatk.broad.UnifiedGenotyper(this)
    ug.input_file = inputBams.values.toList
    ug.out = outputFile
    ug.alleles = config("input_alleles")
    ug.genotyping_mode = org.broadinstitute.gatk.tools.walkers.genotyper.GenotypingOutputMode.GENOTYPE_GIVEN_ALLELES
    add(ug)
  }
}
