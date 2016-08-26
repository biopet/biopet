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
import org.broadinstitute.gatk.queue.function.{ InProcessFunction, QFunction }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.mutable
import scala.io.Source

/**
 * This will collect and write the summary
 *
 * Created by pjvan_thof on 2/14/15.
 */
class WriteSummary(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  require(root.isInstanceOf[SummaryQScript], "root is not a SummaryQScript")

  /** To access qscript for this summary */
  val qscript = root.asInstanceOf[SummaryQScript]

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  @Output(doc = "Summary output", required = true)
  var out: File = qscript.summaryFile

  var md5sum: Boolean = config("summary_md5", default = true)
  //TODO: add more checksums types

  override def freezeFieldValues(): Unit = {
    for (q <- qscript.summaryQScripts) deps :+= q.summaryFile
    for ((_, l) <- qscript.summarizables; s <- l) s match {
      case f: QFunction => deps :+= f.firstOutput
      case _            =>
    }

    jobOutputFile = new File(out.getParentFile, ".%s.%s.out".format(out.getName, analysisName))

    super.freezeFieldValues()
  }

  /** Function to create summary */
  def run(): Unit = {
    for (((name, sampleId, libraryId), summarizables) <- qscript.summarizables; summarizable <- summarizables) {
      summarizable.addToQscriptSummary(qscript, name)
    }

    val pipelineMap = {
      val files = parseFiles(qscript.summaryFiles)
      val settings = qscript.summarySettings
      val executables: Map[String, Any] = {

        def fetchVersion(f: QFunction): Option[(String, Any)] = {
          f match {
            case f: BiopetJavaCommandLineFunction with Version =>
              Some(f.configNamespace -> Map("version" -> f.getVersion.getOrElse(None),
                "java_md5" -> BiopetCommandLineFunction.executableMd5Cache.getOrElse(f.executable, None),
                "java_version" -> f.getJavaVersion,
                "jar_path" -> f.jarFile))
            case f: BiopetCommandLineFunction with Version =>
              Some(f.configNamespace -> Map("version" -> f.getVersion.getOrElse(None),
                "md5" -> BiopetCommandLineFunction.executableMd5Cache.getOrElse(f.executable, None),
                "path" -> f.executable))
            case f: Configurable with Version =>
              Some(f.configNamespace -> Map("version" -> f.getVersion.getOrElse(None)))
            case _ => None
          }
        }

        (
          qscript.functions.flatMap(fetchVersion) ++
          qscript.functions
          .flatMap {
            case f: BiopetCommandLineFunction => f.pipesJobs
            case _                            => Nil
          }.flatMap(fetchVersion(_))
        ).toMap
      }

      val map = Map(qscript.summaryName -> Map(
        "settings" -> settings,
        "files" -> Map("pipeline" -> files),
        "executables" -> executables.toMap)
      )

      qscript match {
        case tag: SampleLibraryTag => prefixSampleLibrary(map, tag.sampleId, tag.libId)
        case q: MultiSampleQScript =>
          ConfigUtils.mergeMaps(
            Map("samples" -> q.samples.map {
              case (sampleName, sample) =>
                sampleName -> Map(
                  qscript.summaryName -> Map(
                    "settings" -> sample.summarySettings,
                    "tags" -> sample.sampleTags),
                  "libraries" -> sample.libraries.map {
                    case (libName, lib) =>
                      libName -> Map(
                        qscript.summaryName -> Map(
                          "settings" -> lib.summarySettings,
                          "tags" -> lib.libTags)
                      )
                  }
                )
            }), map)
        case _ => map
      }
    }

    val jobsMap = (for (
      ((name, sampleId, libraryId), summarizables) <- qscript.summarizables;
      summarizable <- summarizables
    ) yield {
      val map = Map(qscript.summaryName -> parseSummarizable(summarizable, name))

      (prefixSampleLibrary(map, sampleId, libraryId),
        (v1: Any, v2: Any, key: String) => summarizable.resolveSummaryConflict(v1, v2, key))
    }).foldRight(pipelineMap)((a, b) => ConfigUtils.mergeMaps(a._1, b, a._2))

    val combinedMap = (for (qscript <- qscript.summaryQScripts) yield {
      ConfigUtils.fileToConfigMap(qscript.summaryFile)
    }).foldRight(jobsMap)((a, b) => ConfigUtils.mergeMaps(a, b)) ++
      Map("meta" -> Map(
        "last_commit_hash" -> LastCommitHash,
        "pipeline_version" -> nl.lumc.sasc.biopet.Version,
        "pipeline_name" -> qscript.summaryName,
        "output_dir" -> qscript.outputDir,
        "run_name" -> config("run_name", default = qSettings.runName).asString,
        "summary_creation" -> System.currentTimeMillis()
      ))

    val writer = new PrintWriter(out)
    writer.println(ConfigUtils.mapToJson(combinedMap).nospaces)
    writer.close()
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
