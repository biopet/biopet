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

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.extensions.qiime._
import nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSample
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.{ Elem, PrettyPrinter }

/**
 * Created by pjvan_thof on 12/4/15.
 */
class GearsQiimeClosed(val parent: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {

  var fastqInput: File = _

  override def defaults = Map(
    "splitlibrariesfastq" -> Map(
      "barcode_type" -> "not-barcoded"
    )
  )

  def init() = {
    require(fastqInput != null)
    require(sampleId.isDefined)
  }

  private var _otuMap: File = _
  def otuMap = _otuMap

  private var _otuTable: File = _
  def otuTable = _otuTable

  def biopetScript() = {

    val splitLib = new SplitLibrariesFastq(this)
    splitLib.input :+= fastqInput
    splitLib.outputDir = new File(outputDir, "split_libraries_fastq")
    sampleId.foreach(splitLib.sampleIds :+= _.replaceAll("_", "-"))
    splitLib.isIntermediate = true
    add(splitLib)

    val closedReference = new PickClosedReferenceOtus(this)
    closedReference.inputFasta = addDownsample(splitLib.outputSeqs, new File(splitLib.outputDir, s"${sampleId.get}.downsample.fna"))
    closedReference.outputDir = new File(outputDir, "pick_closed_reference_otus")
    add(closedReference)
    _otuMap = closedReference.otuMap
    _otuTable = closedReference.otuTable

    addSummaryJobs()
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map("otu_table" -> otuTable, "otu_map" -> otuMap)

  val downSample: Option[Double] = config("downsample")

  def addDownsample(input: File, output: File): File = {
    downSample match {
      case Some(x) =>
        val seqtk = new SeqtkSample(this)
        seqtk.input = input
        seqtk.sample = x
        seqtk.output = output
        add(seqtk)
        output
      case _ => input
    }
  }
}

object GearsQiimeClosed {
  def qiimeBiomToKrona(inputFile: File, outputFile: File): Unit = {
    val biom = ConfigUtils.fileToConfigMap(inputFile)

    val samples = biom("columns").asInstanceOf[List[Map[String, Any]]].toArray.map(_("id"))

    val sortedSamples = samples.toList.map(_.toString).sorted

    case class TaxNode(name: String, level: String) {
      val childs: ListBuffer[TaxNode] = ListBuffer()

      val counts: mutable.Map[String, Long] = mutable.Map()
      def totalCount(sample: String): Long = counts.getOrElse(sample, 0L) + childs.map(_.totalCount(sample)).sum

      def node: Elem = {
        val sizes = sortedSamples.map { sample => <val>{ totalCount(sample) }</val> }
        val size = <size>{ sizes }</size>

        val node = <node name={ name }>{ size }</node>

        node.copy(child = node.child ++ childs.map(_.node))
      }
    }

    val root = TaxNode("root", "-")

    val taxs = biom("rows").asInstanceOf[List[Map[String, Any]]].toArray.map { row =>
      val taxonomy = row("metadata").asInstanceOf[Map[String, Any]]("taxonomy")
        .asInstanceOf[List[String]].filter(!_.endsWith("__"))
      taxonomy.foldLeft(root) { (a, b) =>
        val n = b.split("__", 2)
        val level = n(0)
        val name = if (level == "Unassigned") "Unassigned" else n(1)
        a.childs.find(_ == TaxNode(name, level)) match {
          case Some(node) => node
          case _ =>
            val node = TaxNode(name, level)
            a.childs += node
            node
        }
      }
    }

    biom("data").asInstanceOf[List[List[Any]]].map { data =>
      val row = data(0).asInstanceOf[Long]
      val column = data(1).asInstanceOf[Long]
      val value = data(2).asInstanceOf[Long]
      val sample = samples(column.toInt).toString
      taxs(row.toInt).counts += sample -> (value + taxs(row.toInt).counts.getOrElse(sample, 0L))
      value
    }.sum

    val xml = <krona>
                <attributes magnitude="size">
                  <attribute display="size">size</attribute>
                </attributes>
                <datasets>
                  { sortedSamples.map { sample => <dataset>{ sample }</dataset> } }
                </datasets>
              </krona>

    val writer = new PrintWriter(outputFile)
    val prettyXml = new PrettyPrinter(80, 2)
    writer.println(prettyXml.format(xml.copy(child = xml.child :+ root.node)))
    writer.close()
  }
}
