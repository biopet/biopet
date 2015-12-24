package nl.lumc.sasc.biopet.pipelines.gears

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.extensions.Flash
import nl.lumc.sasc.biopet.extensions.qiime._
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.{ PrettyPrinter, Elem }

/**
 * Created by pjvan_thof on 12/4/15.
 */
class GearsQiimeClosed(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {

  var fastqR1: File = _

  var fastqR2: Option[File] = None

  override def defaults = Map(
    "splitlibrariesfastq" -> Map(
      "barcode_type" -> "not-barcoded"
    )
  )

  def init() = {
    require(fastqR1 != null)
  }

  private var _otuMap: File = _
  def otuMap = _otuMap

  private var _otuTable: File = _
  def otuTable = _otuTable

  def biopetScript() = {

    val fastqFile = fastqR2 match {
      case Some(r2) =>
        val flash = new Flash(this)
        flash.outputDirectory = new File(outputDir, "combine_reads_flash")
        flash.fastqR1 = fastqR1
        flash.fastqR2 = r2
        add(flash)
        flash.combinedFastq
      case _ => fastqR1
    }

    val splitLib = new SplitLibrariesFastq(this)
    splitLib.input :+= fastqFile
    splitLib.outputDir = new File(outputDir, "split_libraries_fastq")
    sampleId.foreach(splitLib.sample_ids :+= _)
    add(splitLib)

    val closedReference = new PickClosedReferenceOtus(this)
    closedReference.inputFasta = splitLib.outputSeqs
    closedReference.outputDir = new File(outputDir, "pick_closed_reference_otus")
    add(closedReference)
    _otuMap = closedReference.otuMap
    _otuTable = closedReference.otuTable
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map("otu_table" -> otuTable,"otu_map" -> otuMap)

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, "summary.closed_reference.json")
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

      override def toString() = s"$level:$name:$counts"

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
        val name = n(1)
        val bla = a.childs.find(_ == TaxNode(name, level))
        bla match {
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
