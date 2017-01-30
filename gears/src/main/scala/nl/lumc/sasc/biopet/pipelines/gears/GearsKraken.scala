/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.gears

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.kraken.{ KrakenReport, Kraken }
import nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSeq
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
    require(outputName != null)
  }

  lazy val krakenConvertToFasta: Boolean = config("kraken_discard_quality", default = false)

  protected def fastqToFasta(input: File): File = {
    val seqtk = new SeqtkSeq(this)
    seqtk.input = input
    seqtk.output = new File(outputDir, input.getName + ".fasta")
    seqtk.A = true
    seqtk.isIntermediate = true
    add(seqtk)
    seqtk.output
  }

  def biopetScript(): Unit = {
    // start kraken

    val (fqR1, fqR2) = if (krakenConvertToFasta)
      (fastqToFasta(fastqR1), fastqR2.map(fastqToFasta))
    else (fastqR1, fastqR2)

    val krakenAnalysis = new Kraken(this)
    krakenAnalysis.input = fqR1 :: fqR2.toList
    krakenAnalysis.output = new File(outputDir, s"$outputName.krkn.raw")

    krakenAnalysis.paired = fastqR2.isDefined

    krakenAnalysis.classifiedOut = Some(new File(outputDir, s"$outputName.krkn.classified.fastq"))
    krakenAnalysis.unclassifiedOut = Some(new File(outputDir, s"$outputName.krkn.unclassified.fastq"))
    add(krakenAnalysis)

    outputFiles += ("kraken_output_raw" -> krakenAnalysis.output)
    outputFiles += ("kraken_classified_out" -> krakenAnalysis.classifiedOut.getOrElse(""))
    outputFiles += ("kraken_unclassified_out" -> krakenAnalysis.unclassifiedOut.getOrElse(""))

    // create kraken summary file
    val krakenReport = new KrakenReport(this)
    krakenReport.input = krakenAnalysis.output
    krakenReport.showZeros = true
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
  def summarySettings: Map[String, Any] = Map()

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = outputFiles + ("input_R1" -> fastqR1) ++ (fastqR2 match {
    case Some(file) => Map("input_R2" -> file)
    case _          => Map()
  })
}

object GearsKraken {

  def convertKrakenJsonToKronaXml(files: Map[String, File], outputFile: File): Unit = {
    val summaries = files.map { case (k, v) => k -> ConfigUtils.fileToConfigMap(v) }
    convertKrakenSummariesToKronaXml(summaries, outputFile)
  }

  def convertKrakenSummariesToKronaXml(summaries: Map[String, Map[String, Any]], outputFile: File, totalReads: Option[Map[String, Long]] = None): Unit = {

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
                { totalReads.flatMap(_.get(sample)).getOrElse(getValue(sample, (path ::: k :: Nil).tail, "size").getOrElse(0).toString.toLong + unclassified) }
              </val>
            } else {
              <val>
                { getValue(sample, (path ::: k :: Nil).tail, "size").getOrElse(0) }
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