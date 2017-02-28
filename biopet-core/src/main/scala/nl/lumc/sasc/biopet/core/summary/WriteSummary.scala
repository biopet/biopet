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

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.LastCommitHash
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import org.broadinstitute.gatk.queue.function.{ InProcessFunction, QFunction }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.mutable
import scala.io.Source
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This will collect and write the summary
 *
 * Created by pjvan_thof on 2/14/15.
 */
class WriteSummary(val parent: SummaryQScript) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  /** To access qscript for this summary */
  val qscript = parent

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  var md5sum: Boolean = config("summary_md5", default = true)
  //TODO: add more checksums types

  override def freezeFieldValues(): Unit = {
    init()
    super.freezeFieldValues()
  }

  def init(): Unit = {
    qscript.summarizables.foreach(_._2.foreach(_.addToQscriptSummary(qscript)))

    val db = SummaryDb.openSqliteSummary(qscript.summaryDbFile)
    if (qscript == root) { // This initialize the database
      qscript match {
        case s: MultiSampleQScript => s.initSummaryDb
        case t: SampleLibraryTag => t.sampleId.foreach {
          case sampleName =>
            val sampleId = Await.result(db.getSamples(name = Some(sampleName), runId = Some(qscript.summaryRunId)).map(_.headOption.map(_.id)), Duration.Inf).getOrElse {
              Await.result(db.createSample(sampleName, qscript.summaryRunId), Duration.Inf)
            }
            t.libId.foreach { libName =>
              val libId = Await.result(db.getSamples(name = Some(libName), runId = Some(qscript.summaryRunId), sampleId = Some(sampleId)).map(_.headOption.map(_.id)), Duration.Inf).getOrElse {
                Await.result(db.createLibrary(libName, qscript.summaryRunId, sampleId), Duration.Inf)
              }
            }
        }
        case _ => qscript.summaryRunId
      }
    }
    val pipelineId = Await.result(db.createPipeline(qscript.summaryName, qscript.summaryRunId), Duration.Inf)
    qscript.summarizables.map(x => Await.result(db.createModule(x._1._1, qscript.summaryRunId, pipelineId), Duration.Inf))

    for ((_, l) <- qscript.summarizables; s <- l) {
      deps :::= s.summaryDeps
      s match {
        case f: QFunction if qscript.functions.contains(f) => try {
          deps :+= f.firstOutput
        } catch {
          case e: NullPointerException => logger.debug("Queue values are not initialized")
        }
        case _ =>
      }
    }

    jobOutputFile = new File(qscript.outputDir, s".${qscript.summaryName}.summary.out")
  }

  def createFile(db: SummaryDb, runId: Int, pipelineId: Int, moduleId: Option[Int], sampleId: Option[Int], libId: Option[Int], key: String, file: File, outputDir: File) = {
    val path = file.getAbsolutePath.replace(s"^${outputDir.getAbsolutePath}", ".")
    val md5 = WriteSummary.parseChecksum(SummaryQScript.md5sumCache(file))
    val size = if (file.exists()) file.length() else 0L
    val link = if (file.exists()) java.nio.file.Files.isSymbolicLink(file.toPath) else false
    db.createOrUpdateFile(qscript.summaryRunId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)
  }

  /** Function to create summary */
  def run(): Unit = {
    val db = SummaryDb.openSqliteSummary(qscript.summaryDbFile)

    val outputDir = new File(Await.result(db.getRuns(runId = Some(qscript.summaryRunId)).map(_.head.outputDir), Duration.Inf))

    val pipelineId = Await.result(db.getPipelines(name = Some(qscript.summaryName), runId = Some(qscript.summaryRunId)).map(_.head.id), Duration.Inf)

    for (((name, sampleName, libName), summarizables) <- qscript.summarizables.par) {
      require(summarizables.nonEmpty)
      val stats = ConfigUtils.anyToJson(if (summarizables.size == 1) summarizables.head.summaryStats
      else {
        val s = summarizables.map(_.summaryStats)
        s.tail.foldLeft(Map("stats" -> s.head))((a, b) =>
          ConfigUtils.mergeMaps(a, Map("stats" -> b), summarizables.head.resolveSummaryConflict))("stats")
      })
      val moduleId = Await.result(db.getModules(name = Some(name), runId = Some(qscript.summaryRunId), pipelineId = Some(pipelineId))
        .map(_.head.id), Duration.Inf)
      val sampleId = sampleName.map(name => Await.result(db.getSamples(runId = Some(qscript.summaryRunId), name = Some(name)).map(_.head.id), Duration.Inf))
      val libId = libName.map(name => Await.result(db.getLibraries(runId = Some(qscript.summaryRunId), name = Some(name),
        sampleId = sampleId).map(_.head.id), Duration.Inf))
      db.createOrUpdateStat(qscript.summaryRunId, pipelineId, Some(moduleId),
        sampleId, libId, stats.nospaces)

      for ((key, file) <- summarizables.head.summaryFiles.par)
        Await.result(createFile(db, qscript.summaryRunId, pipelineId, Some(moduleId), sampleId, libId, key, file, outputDir), Duration.Inf)
    }

    qscript match {
      case tag: SampleLibraryTag =>
        val sampleId = tag.sampleId.flatMap(name => Await.result(db.getSampleId(qscript.summaryRunId, name), Duration.Inf))
        val libId = tag.libId.flatMap(name => sampleId.flatMap(sampleId => Await.result(db.getLibraryId(qscript.summaryRunId, sampleId, name), Duration.Inf)))
        for ((key, file) <- qscript.summaryFiles.par)
          Await.result(createFile(db, qscript.summaryRunId, pipelineId, None, sampleId, libId, key, file, outputDir), Duration.Inf)
        db.createOrUpdateSetting(qscript.summaryRunId, pipelineId, None, sampleId, libId, ConfigUtils.mapToJson(tag.summarySettings).nospaces)
      case q: MultiSampleQScript =>
        // Global level
        for ((key, file) <- qscript.summaryFiles.par)
          Await.result(createFile(db, q.summaryRunId, pipelineId, None, None, None, key, file, outputDir), Duration.Inf)
        db.createOrUpdateSetting(qscript.summaryRunId, pipelineId, None, None, None, ConfigUtils.mapToJson(q.summarySettings).nospaces)

        for ((sampleName, sample) <- q.samples) {
          // Sample level
          val sampleId = Await.result(db.getSampleId(qscript.summaryRunId, sampleName), Duration.Inf).getOrElse(throw new IllegalStateException("Sample should already exist in database"))
          for ((key, file) <- sample.summaryFiles.par)
            Await.result(createFile(db, q.summaryRunId, pipelineId, Some(sampleId), None, None, key, file, outputDir), Duration.Inf)
          db.createOrUpdateSetting(qscript.summaryRunId, pipelineId, None, Some(sampleId), None, ConfigUtils.mapToJson(sample.summarySettings).nospaces)

          for ((libName, lib) <- sample.libraries) {
            // Library level
            val libId = Await.result(db.getLibraryId(qscript.summaryRunId, sampleId, libName), Duration.Inf).getOrElse(throw new IllegalStateException("Library should already exist in database"))
            for ((key, file) <- lib.summaryFiles.par)
              Await.result(createFile(db, q.summaryRunId, pipelineId, Some(sampleId), Some(libId), None, key, file, outputDir), Duration.Inf)
            db.createOrUpdateSetting(qscript.summaryRunId, pipelineId, None, Some(sampleId), Some(libId), ConfigUtils.mapToJson(lib.summarySettings).nospaces)
          }
        }
      case q =>
        for ((key, file) <- q.summaryFiles.par)
          Await.result(createFile(db, qscript.summaryRunId, pipelineId, None, None, None, key, file, outputDir), Duration.Inf)
        db.createOrUpdateSetting(qscript.summaryRunId, pipelineId, None, None, None, ConfigUtils.mapToJson(q.summarySettings).nospaces)
    }

    (for (f <- qscript.functions.par) yield f match {
      case f: BiopetJavaCommandLineFunction with Version =>
        Some(db.createOrUpdateExecutable(qscript.summaryRunId, f.configNamespace, f.getVersion, f.getJavaVersion,
          javaMd5 = BiopetCommandLineFunction.executableMd5Cache.get(f.executable), jarPath = Option(f.jarFile).map(_.getAbsolutePath)))
      case f: BiopetCommandLineFunction with Version =>
        Some(db.createOrUpdateExecutable(qscript.summaryRunId, f.configNamespace, f.getVersion, Option(f.executable)))
      case f: Configurable with Version =>
        Some(db.createOrUpdateExecutable(qscript.summaryRunId, f.configNamespace, f.getVersion))
      case _ => None
    }).flatten.foreach(Await.ready(_, Duration.Inf))
  }

  def prefixSampleLibrary(map: Map[String, Any], sampleId: Option[String], libraryId: Option[String]): Map[String, Any] = {
    sampleId match {
      case Some(s) => Map("samples" -> Map(s -> (libraryId match {
        case Some(l) => Map("libraries" -> Map(l -> map))
        case _       => map
      })))
      case _ => map
    }
  }

  /** Convert summarizable to a summary map */
  def parseSummarizable(summarizable: Summarizable, name: String) = {
    val stats = summarizable.summaryStats
    val files = parseFiles(summarizable.summaryFiles)

    Map("stats" -> Map(name -> stats)) ++
      (if (files.isEmpty) Map[String, Any]() else Map("files" -> Map(name -> files)))
  }

  /** Parse files map to summary map */
  def parseFiles(files: Map[String, File]): Map[String, Map[String, Any]] = {
    for ((key, file) <- files) yield key -> parseFile(file)
  }

  /** parse single file summary map */
  def parseFile(file: File): Map[String, Any] = {
    val map: mutable.Map[String, Any] = mutable.Map()
    map += "path" -> file.getAbsolutePath
    if (md5sum) map += "md5" -> WriteSummary.parseChecksum(SummaryQScript.md5sumCache(file))
    map.toMap
  }
}

object WriteSummary {
  /** Retrive checksum from file */
  def parseChecksum(checksumFile: File): String = {
    Source.fromFile(checksumFile).getLines().toList.head.split(" ")(0)
  }
}
