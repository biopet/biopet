package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.centrifuge.{ Centrifuge, CentrifugeKreport }
import nl.lumc.sasc.biopet.extensions.tools.KrakenReportToJson
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 19/09/16.
 */
class GearsCentrifuge(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {

  var fastqR1: File = _

  var fastqR2: Option[File] = None

  var outputName: String = _

  override def fixedValues = Map("centrifugekreport" -> Map("only_unique" -> false))
  override def defaults = Map("centrifugekreport" -> Map("show_zeros" -> true))

  def init(): Unit = {
    require(fastqR1 != null)
    if (outputName == null) outputName = fastqR1.getName
      .stripSuffix(".gz")
      .stripSuffix(".fq")
      .stripSuffix(".fastq")
  }

  def biopetScript(): Unit = {
    val centrifuge = new Centrifuge(this)
    centrifuge.inputR1 = fastqR1
    centrifuge.inputR2 = fastqR2
    centrifuge.output = new File(outputDir, s"$outputName.centrifuge")
    centrifuge.report = Some(new File(outputDir, s"$outputName.centrifuge.report"))
    add(centrifuge)

    makeKreport(List(centrifuge.output), "centrifuge", unique = false)
    makeKreport(List(centrifuge.output), "centrifuge_unique", unique = true)

    addSummaryJobs()
  }

  protected def makeKreport(inputFiles: List[File], name: String, unique: Boolean): Unit = {
    val centrifugeKreport = new CentrifugeKreport(this)
    centrifugeKreport.centrifugeOutputFiles = inputFiles
    centrifugeKreport.output = new File(outputDir, s"$outputName.$name.kreport")
    centrifugeKreport.onlyUnique = unique
    add(centrifugeKreport)

    val krakenReportJSON = new KrakenReportToJson(this)
    krakenReportJSON.inputReport = centrifugeKreport.output
    krakenReportJSON.output = new File(outputDir, s"$outputName.$name.krkn.json")
    krakenReportJSON.skipNames = config("skipNames", default = false)
    add(krakenReportJSON)
    addSummarizable(krakenReportJSON, s"${name}_report")
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, sampleId.getOrElse("sampleName_unknown") + ".centrifuge.summary.json")

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map()

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = outputFiles + ("input_R1" -> fastqR1) ++ (fastqR2 match {
    case Some(file) => Map("input_R2" -> file)
    case _          => Map()
  })

}
