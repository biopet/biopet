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

import java.io.{File, PrintWriter}
import java.sql.Date

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.extensions.{CheckChecksum, Md5sum}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import org.broadinstitute.gatk.queue.QScript
import nl.lumc.sasc.biopet.LastCommitHash

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

/**
  * This trait is used for qscript / pipelines that will produce a summary
  *
  * Created by pjvan_thof on 2/14/15.
  */
trait SummaryQScript extends BiopetQScript { qscript: QScript =>

  /** Key is sample/library, None is sample or library is not applicable */
  private[summary] var summarizables
    : Map[(String, Option[String], Option[String]), List[Summarizable]] = Map()

  /** Qscripts summaries that need to be merge into this summary */
  private[summary] var summaryQScripts: List[SummaryQScript] = Nil

  /** Name of the pipeline in the summary */
  var summaryName: String = configNamespace

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any]

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File]

  def summaryDbFile: File = root match {
    case s: SummaryQScript => new File(s.outputDir, s"${s.summaryName}.summary.db")
    case _ => throw new IllegalStateException("Root should be a SummaryQScript")
  }

  /**
    * Add a module to summary for this pipeline
    *
    * @param summarizable summarizable to add to summary for this pipeline
    * @param name Name of module
    * @param sampleId Id of sample
    * @param libraryId Id of libary
    * @param forceSingle If true it replaces summarizable instead of adding to it
    */
  def addSummarizable(summarizable: Summarizable,
                      name: String,
                      sampleId: Option[String] = None,
                      libraryId: Option[String] = None,
                      forceSingle: Boolean = false): Unit = {
    val (sId, lId) = this match {
      case tag: SampleLibraryTag => (tag.sampleId, tag.libId)
      case _ => (sampleId, libraryId)
    }
    if (lId.isDefined) require(sId.isDefined) // Library always require a sample
    if (forceSingle) summarizables = summarizables.filterNot(_._1 == (name, sId, lId))
    summarizables += (name, sId, lId) -> (summarizable :: summarizables.getOrElse((name, sId, lId),
                                                                                  Nil))
  }

  /** Add an other qscript to merge in output summary */
  def addSummaryQScript(summaryQScript: SummaryQScript): Unit = {
    summaryQScripts :+= summaryQScript
  }

  private var addedJobs = false

  final lazy val summaryRunId: Int = {
    if (runIdFile.exists() && summaryDbFile.exists()) {
      val reader = Source.fromFile(runIdFile)
      val id = reader.getLines().next().toInt
      reader.close()

      // Checking if run exist in database
      Await.result(
        SummaryDb.openSqliteSummary(summaryDbFile).getRuns(runId = Some(id)).map(_.headOption),
        Duration.Inf) match {
        case Some(_) => id
        case _ =>
          logger.warn(
            s"Run id found in '$runIdFile' does not exist in summary, creating a new run")
          createRun()
      }
    } else createRun()
  }

  private def runIdFile = root match {
    case s: SummaryQScript => new File(s.outputDir, s".log/summary.runid")
    case _ => throw new IllegalStateException("Root should be a SummaryQscript")
  }

  private def createRun(): Int = {
    val db = SummaryDb.openSqliteSummary(summaryDbFile)
    val dir = root match {
      case q: BiopetQScript => q.outputDir
      case _ => throw new IllegalStateException("Root should be a BiopetQscript")
    }
    val name = root match {
      case q: SummaryQScript => q.summaryName
      case _ => throw new IllegalStateException("Root should be a SummaryQScript")
    }
    val id = Await.result(db.createRun(name,
                                       dir.getAbsolutePath,
                                       nl.lumc.sasc.biopet.Version,
                                       LastCommitHash,
                                       new Date(System.currentTimeMillis())),
                          Duration.Inf)
    runIdFile.getParentFile.mkdir()
    val writer = new PrintWriter(runIdFile)
    writer.println(id)
    writer.close()
    id
  }

  /** Add jobs to qscript to execute summary, also add checksum jobs */
  def addSummaryJobs(): Unit = {
    if (addedJobs)
      throw new IllegalStateException("Summary jobs for this QScript are already executed")
    val writeSummary = new WriteSummary(this)

    def addChecksum(file: File): Unit = {
      if (writeSummary.md5sum) {
        if (!SummaryQScript.md5sumCache.contains(file)) {
          val md5sum = new Md5sum(this) {
            override def configNamespace = "md5sum"

            override def cmdLine: String =
              super.cmdLine + " || " +
                required("echo") + required("error_on_capture  " + input.toString) + " > " + required(
                output)
          }
          md5sum.input = file
          md5sum.output =
            if (file.getAbsolutePath.startsWith(outputDir.getAbsolutePath))
              new File(file.getParentFile, file.getName + ".md5")
            else {
              // Need to not write a md5 file outside the outputDir
              new File(outputDir, ".md5" + file.getAbsolutePath + ".md5")
            }
          md5sum.jobOutputFile =
            new File(md5sum.output.getParentFile, s".${file.getName}.md5.md5sum.out")

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
        case _ =>
      }
    }

    //Automatic checksums
    for ((_, summarizableList) <- summarizables; summarizable <- summarizableList;
         (_, file) <- summarizable.summaryFiles) {
      addChecksum(file)
      summarizable match {
        case f: BiopetJavaCommandLineFunction => if (f.jarFile != null) addChecksum(f.jarFile)
        case _ =>
      }
    }

    qscript match {
      case q: MultiSampleQScript =>
        // Global level
        for ((_, file) <- qscript.summaryFiles) addChecksum(file)

        for ((_, sample) <- q.samples) {
          // Sample level
          for ((_, file) <- sample.summaryFiles) addChecksum(file)
          for ((_, lib) <- sample.libraries) {
            // Library level
            for ((_, file) <- lib.summaryFiles) addChecksum(file)
          }
        }
      case q => for ((_, file) <- q.summaryFiles) addChecksum(file)
    }

    for (inputFile <- inputFiles) {
      inputFile.md5 match {
        case Some(checksum) =>
          val checkMd5 = new CheckChecksum
          checkMd5.inputFile = inputFile.file
          if (!SummaryQScript.md5sumCache.contains(inputFile.file))
            addChecksum(inputFile.file)
          checkMd5.checksumFile = SummaryQScript.md5sumCache(inputFile.file)
          checkMd5.checksum = checksum
          checkMd5.jobOutputFile = new File(checkMd5.checksumFile.getParentFile,
                                            checkMd5.checksumFile.getName + ".check.out")
          add(checkMd5)
        case _ =>
      }
    }

    for ((_, file) <- this.summaryFiles)
      addChecksum(file)

    this match {
      case q: MultiSampleQScript
          if q.onlySamples.nonEmpty && !q.samples.forall(x => q.onlySamples.contains(x._1)) =>
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
