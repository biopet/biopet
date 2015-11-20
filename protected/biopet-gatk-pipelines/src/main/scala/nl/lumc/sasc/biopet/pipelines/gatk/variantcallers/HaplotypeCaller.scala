package nl.lumc.sasc.biopet.pipelines.gatk.variantcallers

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Default mode for the haplotypecaller */
class HaplotypeCaller(val root: Configurable) extends Variantcaller {
  val name = "haplotypecaller"
  protected def defaultPrio = 1

  def biopetScript() {
    val hc = new nl.lumc.sasc.biopet.extensions.gatk.broad.HaplotypeCaller(this)
    hc.input_file = inputBams.values.toList
    hc.out = outputFile
    add(hc)
  }
}

