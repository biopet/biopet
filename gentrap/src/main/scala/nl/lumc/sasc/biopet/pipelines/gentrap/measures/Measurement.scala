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
package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.tools.MergeTables
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.PlotHeatmap
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
trait Measurement extends SummaryQScript with Reference { qscript: QScript =>
  protected var bamFiles: Map[String, File] = Map()

  /**
   * Method to add a bamFile to the pipeline
   *
   * @param id Unique id used for this bam file, most likely to be a sampleName
   * @param file Location of the bam file
   */
  def addBamfile(id: String, file: File): Unit = {
    require(!bamFiles.contains(id), s"'$id' already exist")
    bamFiles += id -> file
  }

  /** Name of job, this is used as prefix for most of the files */
  def name: String = this.getClass.getSimpleName.toLowerCase

  /** Class to store args for MergeTables */
  case class MergeArgs(idCols: List[Int], valCol: Int, numHeaderLines: Int = 0, fallback: String = "-")

  /** This should contain the args for MergeTables */
  def mergeArgs: MergeArgs

  /** Init for pipeline */
  def init(): Unit = {
    require(bamFiles.nonEmpty)
  }

  lazy val mergeCountFiles: Boolean = config("merge_count_files", default = true)

  private var extraSummaryFiles: Map[String, File] = Map()

  def addMergeTableJob(countFiles: List[File],
                       outputFile: File,
                       name: String,
                       fileExtension: String,
                       args: MergeArgs = mergeArgs): Unit = {
    if (mergeCountFiles) {
      add(MergeTables(this, countFiles, outputFile,
        args.idCols, args.valCol, args.numHeaderLines, args.fallback, fileExtension = Some(fileExtension)))
      extraSummaryFiles += s"${name}_table" -> outputFile
    }
  }

  def addHeatmapJob(countTable: File, outputFile: File, name: String, countType: Option[String] = None): Unit = {
    if (mergeCountFiles) {
      val job = new PlotHeatmap(qscript)
      job.input = countTable
      job.output = outputFile
      job.countType = countType
      add(job)
      extraSummaryFiles += s"${name}_heatmap" -> outputFile
    }
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = extraSummaryFiles ++ bamFiles.map { case (id, file) => s"input_bam_$id" -> file }

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, s"$name.summary.json")
}
