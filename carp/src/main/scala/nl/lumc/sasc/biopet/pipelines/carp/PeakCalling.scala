package nl.lumc.sasc.biopet.pipelines.carp

import java.io.File

import nl.lumc.sasc.biopet.core.{PipelineCommand, Reference}
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.macs2.Macs2CallPeak
import nl.lumc.sasc.biopet.utils.{BamUtils, ConfigUtils}
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import org.broadinstitute.gatk.queue.QScript

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvanthof on 29/05/2017.
  */
class PeakCalling(val parent: Configurable) extends QScript with SummaryQScript with Reference {

  def this() = this(null)

  @Input(doc = "Bam files (should be deduped bams)", shortName = "BAM", required = true)
  protected[carp] var inputBamsArg: List[File] = Nil

  var inputBams: Map[String, File] = Map()

  /** Must return a map with used settings for this pipeline */
  override def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  override def summaryFiles: Map[String, File] = Map()

  var paired: Boolean = config("paired", default = false)

  var controls: Map[String, List[String]] = Map()

  def sampleDir(sampleName: String): File = new File(outputDir, sampleName)

  /** Init for pipeline */
  override def init(): Unit = {
    if (controls.isEmpty) controls = config("controls", default = Map()).asMap.map(x => x._1 -> ConfigUtils.any2stringList(x._2))
    if (inputBamsArg.nonEmpty) {
      inputBams = BamUtils.sampleBamMap(inputBamsArg)

      val db = SummaryDb.openSqliteSummary(summaryDbFile)
      for (sampleName <- inputBams.keys) {
        if (Await.result(db.getSampleId(summaryRunId, sampleName), Duration.Inf).isEmpty) {
          db.createSample(sampleName, summaryRunId)
        }
      }
    }
  }

  /** Pipeline itself */
  override def biopetScript(): Unit = {
    for ((sampleName, bamFile) <- inputBams) {
      val macs2 = new Macs2CallPeak(this)
      macs2.treatment = bamFile
      macs2.name = Some(sampleName)
      macs2.outputdir = new File(sampleDir(sampleName), "single_sample_calls")
      macs2.fileformat = if (paired) Some("BAMPE") else Some("BAM")
      add(macs2)

      for (control <- controls.getOrElse(sampleName, Nil)) {
        val macs2 = new Macs2CallPeak(this)
        macs2.treatment = bamFile
        macs2.control = inputBams.getOrElse(control, throw new IllegalStateException(s"Control '$control' is not found"))
        macs2.name = Some(sampleName + "_VS_" + control)
        macs2.fileformat = if (paired) Some("BAMPE") else Some("BAM")
        macs2.outputdir = new File(sampleDir(sampleName), sampleName + "_VS_" + control)
        add(macs2)
      }

    }
  }
}

object PeakCalling extends PipelineCommand