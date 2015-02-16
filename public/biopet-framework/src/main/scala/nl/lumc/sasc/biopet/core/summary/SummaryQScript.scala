package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.{ SampleLibraryTag, BiopetQScript }
import nl.lumc.sasc.biopet.extensions.Md5sum

/**
 * Created by pjvan_thof on 2/14/15.
 */
trait SummaryQScript extends BiopetQScript with Summarizable {

  /** Key is sample/library, None is sample or library is not applicable */
  private[summary] var summarizables: Map[(String, Option[String], Option[String]), List[Summarizable]] = Map()
  private[summary] var summaryQScripts: List[SummaryQScript] = Nil

  var summaryName = configName

  def summaryFile: File

  def addSummarizable(summarizable: Summarizable, name: String): Unit = {
    this match {
      case tag: SampleLibraryTag => addSummarizable(summarizable, name, tag.sampleId, tag.libId)
      case _                     => addSummarizable(summarizable, name, None, None)
    }
  }

  def addSummarizable(summarizable: Summarizable, name: String, sampleId: Option[String]): Unit = {
    addSummarizable(summarizable, name, sampleId, None)
  }

  def addSummarizable(summarizable: Summarizable, name: String, sampleId: Option[String], libraryId: Option[String]): Unit = {
    if (libraryId.isDefined) require(sampleId.isDefined) // Library always require a sample
    summarizables += (name, sampleId, libraryId) -> (summarizable :: summarizables.getOrElse((name, sampleId, libraryId), Nil))
  }

  def addSummaryQScript(summaryQScript: SummaryQScript): Unit = {
    summaryQScripts :+= summaryQScript
  }

  def addSummaryJobs: Unit = {
    val writeSummary = new WriteSummary(this)

    addSummarizable(this, summaryName)

    //Automatic checksums
    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList; (_, file) <- summarizable.summaryFiles) {
      if (writeSummary.md5sum && !SummaryQScript.md5sumCache.contains(file)) {
        val md5sum = Md5sum(this, file)
        writeSummary.deps :+= md5sum.output
        SummaryQScript.md5sumCache += file -> md5sum.output
        add(md5sum)
      }
      //TODO: add more checksums types
    }

    add(writeSummary)
  }
}

object SummaryQScript {
  import scala.collection.mutable.Map
  protected[summary] val md5sumCache: Map[File, File] = Map()
}