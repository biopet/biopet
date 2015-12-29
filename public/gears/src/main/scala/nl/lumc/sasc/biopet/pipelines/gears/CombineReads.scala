package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Flash
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 29/12/15.
  */
class CombineReads(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  @Input(doc = "R1 reads in FastQ format", shortName = "R1", required = false)
  var fastqR1: File = _

  @Input(doc = "R2 reads in FastQ format", shortName = "R2", required = false)
  var fastqR2: File = _

  /** Init for pipeline */
  def init(): Unit = {
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val flash = new Flash(this)
    flash.outputDirectory = new File(outputDir, "flash")
    flash.fastqR1 = fastqR1
    flash.fastqR2 = fastqR2
    add(flash)
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map()

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, "combine_reads.summary.json")
}
