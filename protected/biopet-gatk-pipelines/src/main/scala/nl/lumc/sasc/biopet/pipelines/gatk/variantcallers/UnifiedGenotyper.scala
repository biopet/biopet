package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Default mode for UnifiedGenotyper */
class UnifiedGenotyper(val root: Configurable) extends Variantcaller {
  val name = "unifiedgenotyper"
  protected def defaultPrio = 20

  def biopetScript() {
    val ug = new nl.lumc.sasc.biopet.extensions.gatk.broad.UnifiedGenotyper(this)
    ug.input_file = inputBams.values.toList
    ug.out = outputFile
    add(ug)
  }
}
