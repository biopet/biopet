package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.extensions.delly.Delly
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caler delly */
class Delly(val root: Configurable) extends SvCaller {
  def name = "delly"

  def biopetScript() {
    //TODO: Move mini delly pipeline to here
    for ((sample, bamFile) <- inputBams) {
      val dellyDir = new File(outputDir, sample)
      val delly = Delly(this, bamFile, dellyDir)
      delly.outputName = sample
      addAll(delly.functions)
    }
  }
}
