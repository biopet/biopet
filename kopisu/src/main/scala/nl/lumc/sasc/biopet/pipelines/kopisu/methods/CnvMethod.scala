package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.Reference
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 10/05/16.
 */
trait CnvMethod extends QScript with SummaryQScript with Reference {

  /** Name of mode, this should also be used in the config */
  def name: String

  var namePrefix: String = name

  var inputBams: Map[String, File] = Map.empty

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, s"$name.summary.json")

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = inputBams.map(x => s"inputbam_${x._1}" -> x._2)

  def init() = {}
}
