/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import nl.lumc.sasc.biopet.extensions.gatk.broad
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Allele mode for GenotyperAllele */
class UnifiedGenotyperAllele(val root: Configurable) extends Variantcaller {
  val name = "unifiedgenotyper_allele"
  protected def defaultPrio = 9

  def biopetScript() {
    val ug = broad.UnifiedGenotyper(this, inputBams.values.toList, outputFile)
    ug.alleles = config("input_alleles")
    ug.genotyping_mode = Some("GENOTYPE_GIVEN_ALLELES")
    add(ug)
  }
}
