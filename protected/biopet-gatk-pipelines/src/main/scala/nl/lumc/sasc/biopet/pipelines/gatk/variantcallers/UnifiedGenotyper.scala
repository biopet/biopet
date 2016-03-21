/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.gatk.broad

/** Default mode for UnifiedGenotyper */
class UnifiedGenotyper(val root: Configurable) extends Variantcaller {
  val name = "unifiedgenotyper"
  protected def defaultPrio = 20

  def biopetScript() {
    val ug = broad.UnifiedGenotyper(this, inputBams.values.toList, outputFile)
    add(ug)
  }
}
