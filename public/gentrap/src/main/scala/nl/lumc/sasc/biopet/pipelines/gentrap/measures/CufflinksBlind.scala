package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 1/12/16.
  */
class CufflinksBlind(val root: Configurable) extends QScript with Measurement {
  /** Init for pipeline */
  def init(): Unit = ???

  /** Pipeline itself */
  def biopetScript(): Unit = ???

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map()

  /** Name of summary output file */
  def summaryFile: File = new File("CufflinksBlind.summary.json")
}
