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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.core.{ Reference, ToolCommandFunction }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

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
      case _            => new File(outputDir, name)
    }
  }

  @Output
  private var outputFiles: List[File] = Nil

  def bamstatsSummary: File = new File(outputDir, "bamstats.summary.json")

  override def beforeGraph() {
    super.beforeGraph()
    deps :+= new File(bamFile.getAbsolutePath.replaceAll(".bam$", ".bai"))
    outputFiles :+= bamstatsSummary
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

  def summaryStats: Map[String, Any] = ConfigUtils.fileToConfigMap(bamstatsSummary)

  override def summaryDeps: List[File] = bamstatsSummary :: super.summaryDeps
}
