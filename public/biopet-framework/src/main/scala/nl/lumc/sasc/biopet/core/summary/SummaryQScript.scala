package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunctionTrait, BiopetCommandLineFunction, SampleLibraryTag, BiopetQScript }
import nl.lumc.sasc.biopet.extensions.Md5sum

import scala.collection.mutable

/**
 * Created by pjvan_thof on 2/14/15.
 */
trait SummaryQScript extends BiopetQScript {

  /** Key is sample/library, None is sample or library is not applicable */
  private[summary] var summarizables: Map[(String, Option[String], Option[String]), List[Summarizable]] = Map()

  /** Qscripts summaries that need to be merge into this summary */
  private[summary] var summaryQScripts: List[SummaryQScript] = Nil

  /** Name of the pipeline in the summary */
  var summaryName = configName

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any]

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File]

  /** Name of summary output file */
  def summaryFile: File

  /**
   * Add a module to summary for this pipeline
   *
   * Auto detect sample and library from pipeline
   *
   * @param summarizable summarizable to add to summary for this pipeline
   * @param name Name of module
   */
  def addSummarizable(summarizable: Summarizable, name: String): Unit = {
    this match {
      case tag: SampleLibraryTag => addSummarizable(summarizable, name, tag.sampleId, tag.libId)
      case _                     => addSummarizable(summarizable, name, None, None)
    }
  }

  /**
   * Add a module to summary for this pipeline
   *
   * @param summarizable summarizable to add to summary for this pipeline
   * @param name Name of module
   * @param sampleId
   */
  def addSummarizable(summarizable: Summarizable, name: String, sampleId: Option[String]): Unit = {
    addSummarizable(summarizable, name, sampleId, None)
  }

  /**
   * Add a module to summary for this pipeline
   *
   * @param summarizable summarizable to add to summary for this pipeline
   * @param name Name of module
   * @param sampleId
   * @param libraryId
   */
  def addSummarizable(summarizable: Summarizable, name: String, sampleId: Option[String], libraryId: Option[String]): Unit = {
    if (libraryId.isDefined) require(sampleId.isDefined) // Library always require a sample
    summarizables += (name, sampleId, libraryId) -> (summarizable :: summarizables.getOrElse((name, sampleId, libraryId), Nil))
  }

  /**
   * Add an other qscript to merge in output summary
   * @param summaryQScript
   */
  def addSummaryQScript(summaryQScript: SummaryQScript): Unit = {
    summaryQScripts :+= summaryQScript
  }

  /** Add jobs to qscript to execute summary, also add checksum jobs */
  def addSummaryJobs: Unit = {
    val writeSummary = new WriteSummary(this)

    def addChecksum(file: File): Unit = {
      if (writeSummary.md5sum && !SummaryQScript.md5sumCache.contains(file)) {
        val md5sum = new Md5sum(this) {
          override def cmdLine: String = super.cmdLine + " || " +
            required("echo") + required("error_on_capture  " + input.toString) + " > " + required(output)
        }
        md5sum.input = file
        md5sum.output = new File(file.getParentFile, file.getName + ".md5")

        // Need to not write a md5 file outside the outputDir
        if (!file.getAbsolutePath.startsWith(outputDir.getAbsolutePath))
          md5sum.output = new File(outputDir, ".md5" + file.getAbsolutePath + ".md5")

        writeSummary.deps :+= md5sum.output
        SummaryQScript.md5sumCache += file -> md5sum.output
        add(md5sum)
      }
      //TODO: add more checksums types
    }

    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList) {
      summarizable match {
        case f: BiopetCommandLineFunctionTrait => f.beforeGraph
        case _                                 =>
      }
    }

    //Automatic checksums
    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList; (_, file) <- summarizable.summaryFiles)
      addChecksum(file)

    for ((_, file) <- this.summaryFiles)
      addChecksum(file)

    add(writeSummary)
  }
}

object SummaryQScript {
  import scala.collection.mutable.Map

  /** Cache to have no duplicate jobs */
  protected[summary] val md5sumCache: Map[File, File] = Map()
}