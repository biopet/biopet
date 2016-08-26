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
package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.extensions.{ CheckChecksum, Md5sum }
import org.broadinstitute.gatk.queue.QScript

import scala.collection.mutable

/**
 * This trait is used for qscript / pipelines that will produce a summary
 *
 * Created by pjvan_thof on 2/14/15.
 */
trait SummaryQScript extends BiopetQScript { qscript: QScript =>

  /** Key is sample/library, None is sample or library is not applicable */
  private[summary] var summarizables: Map[(String, Option[String], Option[String]), List[Summarizable]] = Map()

  /** Qscripts summaries that need to be merge into this summary */
  private[summary] var summaryQScripts: List[SummaryQScript] = Nil

  /** Name of the pipeline in the summary */
  var summaryName = configNamespace

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
   * @param sampleId Id of sample
   */
  def addSummarizable(summarizable: Summarizable, name: String, sampleId: Option[String]): Unit = {
    addSummarizable(summarizable, name, sampleId, None)
  }

  /**
   * Add a module to summary for this pipeline
   *
   * @param summarizable summarizable to add to summary for this pipeline
   * @param name Name of module
   * @param sampleId Id of sample
   * @param libraryId Id of libary
   */
  def addSummarizable(summarizable: Summarizable, name: String, sampleId: Option[String], libraryId: Option[String]): Unit = {
    if (libraryId.isDefined) require(sampleId.isDefined) // Library always require a sample
    summarizables += (name, sampleId, libraryId) -> (summarizable :: summarizables.getOrElse((name, sampleId, libraryId), Nil))
  }

  /** Add an other qscript to merge in output summary */
  def addSummaryQScript(summaryQScript: SummaryQScript): Unit = {
    summaryQScripts :+= summaryQScript
  }

  private var addedJobs = false

  /** Add jobs to qscript to execute summary, also add checksum jobs */
  def addSummaryJobs(): Unit = {
    if (addedJobs) throw new IllegalStateException("Summary jobs for this QScript are already executed")
    val writeSummary = new WriteSummary(this)

    def addChecksum(file: File): Unit = {
      if (writeSummary.md5sum) {
        if (!SummaryQScript.md5sumCache.contains(file)) {
          val md5sum = new Md5sum(this) {
            override def configNamespace = "md5sum"

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
        } else writeSummary.deps :+= SummaryQScript.md5sumCache(file)
      }
      //TODO: add more checksums types
    }

    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList) {
      summarizable match {
        case f: BiopetCommandLineFunction => f.beforeGraph()
        case _                            =>
      }
    }

    //Automatic checksums
    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList; (_, file) <- summarizable.summaryFiles) {
      addChecksum(file)
      summarizable match {
        case f: BiopetJavaCommandLineFunction => if (f.jarFile != null) addChecksum(f.jarFile)
        case _                                =>
      }
    }

    for (inputFile <- inputFiles) {
      inputFile.md5 match {
        case Some(checksum) => {
          val checkMd5 = new CheckChecksum
          checkMd5.inputFile = inputFile.file
          require(SummaryQScript.md5sumCache.contains(inputFile.file),
            s"Md5 job is not executed, checksum file can't be found for: ${inputFile.file}")
          checkMd5.checksumFile = SummaryQScript.md5sumCache(inputFile.file)
          checkMd5.checksum = checksum
          checkMd5.jobOutputFile = new File(checkMd5.checksumFile.getParentFile, checkMd5.checksumFile.getName + ".check.out")
          add(checkMd5)
        }
        case _ =>
      }
    }

    for ((_, file) <- this.summaryFiles)
      addChecksum(file)

    this match {
      case q: MultiSampleQScript if q.onlySamples.nonEmpty && !q.samples.forall(x => q.onlySamples.contains(x._1)) =>
        logger.info("Write summary is skipped because sample flag is used")
      case _ => add(writeSummary)
    }

    addedJobs = true
  }
}

object SummaryQScript {

  /** Cache to have no duplicate jobs */
  protected[summary] val md5sumCache: mutable.Map[File, File] = mutable.Map()
}