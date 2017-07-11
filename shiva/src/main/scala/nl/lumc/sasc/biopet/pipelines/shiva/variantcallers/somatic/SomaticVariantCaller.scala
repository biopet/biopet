package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic

import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.Variantcaller

trait SomaticVariantCaller extends Variantcaller {

  var tnPairs: List[TumorNormalPair] = _

}

case class TumorNormalPair(tumorSample: String, normalSample: String)