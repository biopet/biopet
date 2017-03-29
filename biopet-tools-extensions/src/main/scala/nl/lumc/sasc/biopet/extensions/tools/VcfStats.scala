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

import nl.lumc.sasc.biopet.core.summary.{ Summarizable }
import nl.lumc.sasc.biopet.core.{ Reference, ToolCommandFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.{ ConfigUtils }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.io.Source

/**
 * This tool will generate statistics from a vcf file
 *
 * Created by pjvan_thof on 1/10/15.
 */
class VcfStats(val parent: Configurable) extends ToolCommandFunction with Summarizable with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.vcfstats.VcfStats

  mainFunction = false

  @Input(doc = "Input fastq", shortName = "I", required = true)
  var input: File = _

  @Input
  protected var index: File = null

  @Output
  protected var statsFile: File = null

  override def defaultCoreMemory = 3.0
  override def defaultThreads = 3

  protected var outputDir: File = _

  var infoTags: List[String] = Nil
  var genotypeTags: List[String] = Nil
  var allInfoTags = false
  var allGenotypeTags = false
  var reference: File = _
  var intervals: Option[File] = None

  override def beforeGraph(): Unit = {
    reference = referenceFasta()
    index = new File(input.getAbsolutePath + ".tbi")
  }

  /** Set output dir and a output file */
  def setOutputDir(dir: File): Unit = {
    outputDir = dir
    statsFile = new File(dir, "stats.json")
    jobOutputFile = new File(dir, ".vcfstats.out")
  }

  /** Creates command to execute extension */
  override def cmdLine = super.cmdLine +
    required("-I", input) +
    required("-o", outputDir) +
    repeat("--infoTag", infoTags) +
    repeat("--genotypeTag", genotypeTags) +
    conditional(allInfoTags, "--allInfoTags") +
    conditional(allGenotypeTags, "--allGenotypeTags") +
    required("-R", reference) +
    optional("--intervals", intervals)

  /** Returns general stats to the summary */
  def summaryStats: Map[String, Any] = ConfigUtils.fileToConfigMap(statsFile)

  /** return only general files to summary */
  def summaryFiles: Map[String, File] = Map(
    "stats" -> statsFile
  )
}
