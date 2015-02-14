package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.extensions.Md5sum

/**
 * Created by pjvan_thof on 2/14/15.
 */
trait SummaryQScript extends BiopetQScript {

  /** Key is sample/library, None is sample or library is not applicable */
  private[summary] var summarizables: Map[(Option[String], Option[String]), List[Summarizable]] = Map()
  private[summary] var summaryQScripts: List[SummaryQScript] = Nil

  def summaryFile: File

  def addSummarizable(summarizable: Summarizable,
                      sampleId: Option[String] = None,
                      libraryId: Option[String] = None): Unit = {
    require(libraryId.isDefined == sampleId.isDefined) // Library always require a sample
    summarizables += (sampleId, libraryId) -> (summarizable :: summarizables.getOrElse((sampleId, libraryId), Nil))
  }

  def addSummaryQScript(summaryQScript: SummaryQScript): Unit = {
    summaryQScripts :+= summaryQScript
  }

  def addSummaryJobs: Unit = {
    val writeSummary = new WriteSummary(this)

    //Automatic checksums
    val keepChecksums: Boolean = config("keep_checksums_files", default = false)

    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList; (_, file) <- summarizable.summaryFiles) {
      if (writeSummary.md5sum) {
        val md5sum = Md5sum(this, file)
        md5sum.isIntermediate = !keepChecksums
        writeSummary.deps :+= md5sum.output
        add(md5sum)
      }
      //TODO: add more checksums types
    }

    add(writeSummary)
  }
}
