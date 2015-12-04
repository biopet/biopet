package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.kraken.{ KrakenReport, Kraken }
import nl.lumc.sasc.biopet.extensions.tools.KrakenReportToJson
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 04/12/15.
 */
class GearsKraken(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {

  var fastqR1: File = _

  var fastqR2: Option[File] = None

  var outputName: String = _

  def init(): Unit = {
    require(fastqR1 != null)
    if (outputName == null) outputName = fastqR1.getName
      .stripSuffix(".gz")
      .stripSuffix(".fq")
      .stripSuffix(".fastq")
  }

  def biopetScript(): Unit = {
    // start kraken
    val krakenAnalysis = new Kraken(this)
    krakenAnalysis.input = fastqR1 :: fastqR2.toList
    krakenAnalysis.output = new File(outputDir, s"$outputName.krkn.raw")

    krakenAnalysis.paired = fastqR2.isDefined

    krakenAnalysis.classified_out = Some(new File(outputDir, s"$outputName.krkn.classified.fastq"))
    krakenAnalysis.unclassified_out = Some(new File(outputDir, s"$outputName.krkn.unclassified.fastq"))
    add(krakenAnalysis)

    outputFiles += ("kraken_output_raw" -> krakenAnalysis.output)
    outputFiles += ("kraken_classified_out" -> krakenAnalysis.classified_out.getOrElse(""))
    outputFiles += ("kraken_unclassified_out" -> krakenAnalysis.unclassified_out.getOrElse(""))

    // create kraken summary file
    val krakenReport = new KrakenReport(this)
    krakenReport.input = krakenAnalysis.output
    krakenReport.show_zeros = true
    krakenReport.output = new File(outputDir, s"$outputName.krkn.full")
    add(krakenReport)

    outputFiles += ("kraken_report_input" -> krakenReport.input)
    outputFiles += ("kraken_report_output" -> krakenReport.output)

    val krakenReportJSON = new KrakenReportToJson(this)
    krakenReportJSON.inputReport = krakenReport.output
    krakenReportJSON.output = new File(outputDir, s"$outputName.krkn.json")
    krakenReportJSON.skipNames = config("skipNames", default = false)
    add(krakenReportJSON)
    addSummarizable(krakenReportJSON, "krakenreport")

    outputFiles += ("kraken_report_json_input" -> krakenReportJSON.inputReport)
    outputFiles += ("kraken_report_json_output" -> krakenReportJSON.output)

    addSummaryJobs()
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, sampleId.getOrElse("sampleName_unknown") + ".kraken.summary.json")

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map.empty

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = outputFiles + ("input_R1" -> fastqR1) ++ (fastqR2 match {
    case Some(file) => Map("input_R1" -> file)
    case _          => Map()
  })
}
