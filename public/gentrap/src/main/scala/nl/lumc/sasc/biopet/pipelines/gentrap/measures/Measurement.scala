package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.tools.MergeTables
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
trait Measurement extends SummaryQScript with Reference { qscript: QScript =>
  protected var bamFiles: Map[String, File] = Map()

  def addBamfile(id: String, file: File): Unit = {
    require(!bamFiles.contains(id), s"'$id' already exist")
    bamFiles += id -> file
  }

  def name: String = this.getClass.getSimpleName.toLowerCase

  lazy val countFiles: Map[String, File] = bamFiles.map { case (id, bamFile) => bamToCountFile(id, bamFile) }

  def mergedCountFile = new File(outputDir, s"$name.merged.tsv")

  case class MergeArgs(idCols: List[Int],
                       valCol: Int,
                       numHeaderLines: Int = 0,
                       fallback: String = "-")

  def mergeArgs: MergeArgs

  /** Init for pipeline */
  def init(): Unit = {
    require(bamFiles.nonEmpty)
  }

  /** Pipeline itself */
  def biopetScript(): Unit = {
    add(MergeTables(this, countFiles.values.toList, mergedCountFile,
      mergeArgs.idCols, mergeArgs.valCol, mergeArgs.numHeaderLines, mergeArgs.fallback))

    //TODO: Heatmap
  }

  def bamToCountFile(id: String, bamFile: File): (String, File)

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map()

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, s"$name.summary.json")
}
