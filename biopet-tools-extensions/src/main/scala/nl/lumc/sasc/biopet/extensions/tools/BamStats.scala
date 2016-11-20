package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.core.{Reference, ToolCommandFunction}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * Created by pjvanthof on 18/11/2016.
  */
class BamStats(val root: Configurable) extends ToolCommandFunction with Reference with Summarizable {
  def toolObject = nl.lumc.sasc.biopet.tools.bamstats.BamStats

  @Input(required = true)
  var reference: File = _

  @Input(required = true)
  var bamFile: File = _

  var outputDir: File = _

  var binSize: Option[Int] = config("bin_size")
  var threadBinSize: Option[Int] = config("thread_bin_size")

  override def defaultThreads = 3
  override def defaultCoreMemory = 5.0
  override def dictRequired = true

  def getOutputFile(name: String, contig: Option[String] = None): File = {
    contig match {
      case Some(contig) => new File(outputDir, "contigs" + File.separator + contig + File.separator + name)
      case _ => new File(outputDir, name)
    }
  }

  def flagstatSummaryFile(contig: Option[String] = None): File = getOutputFile("flagstats.summary.json", contig)
  def mappingQualityFile(contig: Option[String] = None): File = getOutputFile("mapping_quality.tsv", contig)
  def clipingFile(contig: Option[String] = None): File = getOutputFile("clipping.tsv", contig)

  override def beforeGraph() {
    super.beforeGraph()
    jobOutputFile = new File(outputDir, ".bamstats.out")
    if (reference == null) reference = referenceFasta()
  }

  /** Creates command to execute extension */
  override def cmdLine = super.cmdLine +
    required("-b", bamFile) +
    required("-o", outputDir) +
    required("-R", reference) +
    optional("--binSize", binSize) +
    optional("--threadBinSize", threadBinSize)


  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = Map(
    "flagstats" -> ConfigUtils.fileToConfigMap(flagstatSummaryFile()),
    "flagstats_per_contig" -> referenceDict.getSequences.map {
      c => c.getSequenceName -> ConfigUtils.fileToConfigMap(flagstatSummaryFile(Some(c.getSequenceName)))
    }.toMap,
    "mapping_quality" -> BamStats.tsvToMap(mappingQualityFile()),
    "clipping" -> BamStats.tsvToMap(clipingFile())
  )
}

object BamStats {
  def tsvToMap(tsvFile: File): Map[String, Array[Int]] = {
    val reader = Source.fromFile(tsvFile)
    val it = reader.getLines()
    val header = it.next().split("\t")
    val arrays = header.zipWithIndex.map(x => x._2 -> (x._1 -> Array[Int]()))
    for (line <- it) {
      val values = line.split("\t")
      require(values.size == header.size, s"Line does not have the number of field as header: $line")
      for (array <- arrays) {
        array._2._2 :+ values(array._1)
      }
    }
    reader.close()
    arrays.map(_._2).toMap
  }
}