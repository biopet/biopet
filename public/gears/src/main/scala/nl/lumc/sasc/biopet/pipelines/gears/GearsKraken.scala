package nl.lumc.sasc.biopet.pipelines.gears

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.kraken.{ KrakenReport, Kraken }
import nl.lumc.sasc.biopet.extensions.tools.KrakenReportToJson
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.mutable
import scala.xml.{ PrettyPrinter, Node }

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

object GearsKraken {

  def convertKrakenJsonToKronaXml(files: Map[String, File], outputFile: File): Unit = {
    val summaries = files.map { case (k, v) => k -> ConfigUtils.fileToConfigMap(v) }
    convertKrakenSummariesToKronaXml(summaries, outputFile)
  }

  def convertKrakenSummariesToKronaXml(summaries: Map[String, Map[String, Any]], outputFile: File): Unit = {

    val samples = summaries.keys.toList.sorted

    val taxs: mutable.Map[String, Any] = mutable.Map()

    def addTax(map: Map[String, Any], path: List[String] = Nil): Unit = {
      val name = map("name").toString
      val x = path.foldLeft(taxs)((a, b) => if (a.contains(b)) a(b).asInstanceOf[mutable.Map[String, Any]] else {
        a += b -> mutable.Map[String, Any]()
        a(b).asInstanceOf[mutable.Map[String, Any]]
      })

      if (!x.contains(name)) x += name -> mutable.Map[String, Any]()

      map("children").asInstanceOf[List[Any]].foreach(x => addTax(x.asInstanceOf[Map[String, Any]], path ::: name :: Nil))
    }

    summaries.foreach { x => addTax(x._2("classified").asInstanceOf[Map[String, Any]]) }

    def getValue(sample: String, path: List[String], key: String) = {
      path.foldLeft(summaries(sample)("classified").asInstanceOf[Map[String, Any]]) { (b, a) =>
        b.getOrElse("children", List[Map[String, Any]]())
          .asInstanceOf[List[Map[String, Any]]]
          .find(_.getOrElse("name", "") == a).getOrElse(Map[String, Any]())
      }.get(key)
    }

    def createNodes(map: mutable.Map[String, Any], path: List[String] = Nil): Seq[Node] = {
      map.map {
        case (k, v) =>
          val node = <node name={ k }></node>
          val sizes = samples.map { sample =>
            if (k == "root") {
              val unclassified = summaries(sample)("unclassified").asInstanceOf[Map[String, Any]]("size").asInstanceOf[Long]
              <val>
                {getValue(sample, (path ::: k :: Nil).tail, "size").getOrElse(0).toString.toLong + unclassified}
              </val>
            } else {
              <val>
                {getValue(sample, (path ::: k :: Nil).tail, "size").getOrElse(0)}
              </val>
            }
          }
          val size = <size>{ sizes }</size>
          node.copy(child = size ++ createNodes(v.asInstanceOf[mutable.Map[String, Any]], path ::: k :: Nil))
      }.toSeq
    }

    val xml = <krona>
                <attributes magnitude="size">
                  <attribute display="size">size</attribute>
                </attributes>
                <datasets>
                  { samples.map { sample => <dataset>{ sample }</dataset> } }
                </datasets>
              </krona>

    val writer = new PrintWriter(outputFile)
    val prettyXml = new PrettyPrinter(80, 2)
    writer.println(prettyXml.format(xml.copy(child = xml.child ++ createNodes(taxs))))
    writer.close()
  }
}