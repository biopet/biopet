package nl.lumc.sasc.biopet.pipelines.tarmac

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by Sander Bollen on 23-3-17.
  */
class Tarmac(val root: Configurable) extends QScript with SummaryQScript with Reference {
  qscript =>
  def this() = this(null)

  def init() = {

  }

  def biopetScript() = {

  }


  def summarySettings: Map[String, Any] = Map()
  def summaryFiles: Map[String, File] = Map()

  def summaryFile: File = new File(outputDir, "tarmac.summary.json")
}
