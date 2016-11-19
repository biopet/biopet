package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.core.{Reference, ToolCommandFunction}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

import scala.collection.JavaConversions._

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

  def flagstatSummaryFile(implicit contig: Option[String] = None): File = {
    contig match {
      case Some(contig) => new File(outputDir, "contigs" + File.separator + contig + File.separator + "flagstats.summary.json")
      case _ => new File(outputDir, "flagstats.summary.json")
    }
  }

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
    "flagstats" -> ConfigUtils.fileToConfigMap(flagstatSummaryFile),
    "flagstats_per_contig" -> referenceDict.getSequences.map {
      c => c.getSequenceName -> ConfigUtils.fileToConfigMap(flagstatSummaryFile(Some(c.getSequenceName)))
    }.toMap
  )
}
