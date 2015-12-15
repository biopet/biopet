package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import nl.lumc.sasc.biopet.extensions.clever.CleverCaller
import nl.lumc.sasc.biopet.utils.config.Configurable

/** Script for sv caler Clever */
class Clever(val root: Configurable) extends SvCaller {
  def name = "clever"

  def biopetScript() {
    //TODO: check double directories
    for ((sample, bamFile) <- inputBams) {
      val cleverDir = new File(outputDir, sample)
      val clever = CleverCaller(this, bamFile, cleverDir)
      add(clever)
    }
  }
}
