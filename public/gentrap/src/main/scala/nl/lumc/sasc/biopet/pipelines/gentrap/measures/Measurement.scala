package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import java.io.File

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
   * @param id Uniqe id used for this bam file, most likly to be a sampleName
   * @param file Location of the bam file
   */
  def addBamfile(id: String, file: File): Unit = {
    require(!bamFiles.contains(id), s"'$id' already exist")
    bamFiles += id -> file
  }

  /** Name of job, this is used as prefix for most of the files */
  def name: String = this.getClass.getSimpleName.toLowerCase

  /** Locations of single bam count tables */
  lazy val countFiles: Map[String, File] = bamFiles.map { case (id, bamFile) => bamToCountFile(id, bamFile) }

  /** Location of merge count table */
  def mergedCountFile = new File(outputDir, s"$name.merged.tsv")

  /** Location of heatmap */
  def heatpMap = new File(outputDir, s"$name.heatmap.png")

  /** Class to store args for MergeTables */
  case class MergeArgs(idCols: List[Int], valCol: Int, numHeaderLines: Int = 0, fallback: String = "-")

  /** This should contain the args for MergeTables */
  def mergeArgs: MergeArgs

  /** Init for pipeline */
  def init(): Unit = {
    require(bamFiles.nonEmpty)
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    add(MergeTables(this, countFiles.values.toList, mergedCountFile,
      mergeArgs.idCols, mergeArgs.valCol, mergeArgs.numHeaderLines, mergeArgs.fallback))

    val job = new PlotHeatmap(qscript)
    job.input = mergedCountFile
    job.output = heatpMap
    job.countType = Some(name)
    add(job)
  }

  /** This function should add the count table for each bamFile */
  def bamToCountFile(id: String, bamFile: File): (String, File)

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map("merged_table" -> mergedCountFile, "heatmap" -> heatpMap) ++
    bamFiles.map { case (id, file) => s"input_bam_$id" -> file }

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, s"$name.summary.json")
}
