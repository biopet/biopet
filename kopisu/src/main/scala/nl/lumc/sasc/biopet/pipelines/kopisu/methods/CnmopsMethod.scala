package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import nl.lumc.sasc.biopet.extensions.Cnmops
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by wyleung on 2-6-16.
 */
class CnmopsMethod(val root: Configurable) extends CnvMethod {
  def name = "cnmops"

  def biopetScript: Unit = {

    val cnmops = new Cnmops(this)
    cnmops.input = inputBams.flatMap {
      case (sampleName, bamFile) => Some(bamFile)
      case _                     => None
    }.toList
    cnmops.outputDir = outputDir
    add(cnmops)

    addSummaryJobs()
  }
}
