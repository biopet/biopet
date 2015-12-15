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

