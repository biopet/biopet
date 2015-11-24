package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.extensions.breakdancer.Breakdancer
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caler Breakdancer */
class Breakdancer(val root: Configurable) extends SvCaller {
  def name = "breakdancer"

  def biopetScript() {
    //TODO: move minipipeline of breakdancer to here
    for ((sample, bamFile) <- inputBams) {
      val breakdancerDir = new File(outputDir, sample)
      val breakdancer = Breakdancer(this, bamFile, breakdancerDir)
      addAll(breakdancer.functions)
    }
  }
}
