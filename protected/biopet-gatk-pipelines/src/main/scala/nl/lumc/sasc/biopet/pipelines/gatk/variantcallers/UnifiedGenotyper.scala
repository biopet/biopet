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
