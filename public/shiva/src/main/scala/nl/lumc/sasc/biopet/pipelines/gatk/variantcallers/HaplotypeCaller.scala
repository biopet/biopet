/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad

/** Default mode for the haplotypecaller */
class HaplotypeCaller(val root: Configurable) extends Variantcaller {
  val name = "haplotypecaller"
  protected def defaultPrio = 1

  def biopetScript() {
    val hc = broad.HaplotypeCaller(this, inputBams.values.toList, outputFile)
    add(hc)
  }
}

