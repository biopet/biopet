package nl.lumc.sasc.biopet.core.summary

import java.io.{ FileInputStream, PrintWriter, File }
import java.security.MessageDigest

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.function.{ QFunction, InProcessFunction }
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.collection.mutable
import scala.io.Source

/**
 * Created by pjvan_thof on 2/14/15.
 */
class WriteSummary(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  require(root.isInstanceOf[SummaryQScript], "root is not a SummaryQScript")

  val summaryQScript = root.asInstanceOf[SummaryQScript]

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  @Output(doc = "Summary output", required = true)
  var out: File = summaryQScript.summaryFile

  var md5sum: Boolean = config("summary_md5", default = true)
  //TODO: add more checksums types

  override def freezeFieldValues(): Unit = {
    for (q <- summaryQScript.summaryQScripts) deps :+= q.summaryFile
    for ((_, l) <- summaryQScript.summarizables; s <- l) s match {
      case f: QFunction => deps :+= f.firstOutput
      case _            =>
    }
    super.freezeFieldValues()
  }

  def run(): Unit = {
    val map = (for (
      ((name, sampleId, libraryId), summarizables) <- summaryQScript.summarizables;
      summarizable <- summarizables
    ) yield {
      val map = Map(name -> parseSummarizable(summarizable))

      (sampleId match {
        case Some(sampleId) => Map("samples" -> Map(sampleId -> (libraryId match {
          case Some(libraryId) => Map("libraries" -> Map(libraryId -> map))
          case _               => map
        })))
        case _ => map
      }, (v1: Any, v2: Any, key: String) => summarizable.resolveSummaryConflict(v1, v2, key))
    }).foldRight(Map[String, Any]())((a, b) => ConfigUtils.mergeMaps(a._1, b, a._2))

    val combinedMap = (for (qscript <- summaryQScript.summaryQScripts) yield {
      ConfigUtils.fileToConfigMap(qscript.summaryFile)
    }).foldRight(map)((a, b) => ConfigUtils.mergeMaps(a, b))

    val writer = new PrintWriter(out)
    writer.println(ConfigUtils.mapToJson(combinedMap).spaces4)
    writer.close()
  }

  def parseSummarizable(summarizable: Summarizable): Map[String, Map[String, Any]] = {
    Map("data" -> summarizable.summaryData, "files" -> parseFiles(summarizable.summaryFiles))
  }

  def parseFiles(files: Map[String, File]): Map[String, Map[String, Any]] = {
    for ((key, file) <- files) yield {
      val map: mutable.Map[String, Any] = mutable.Map()
      map += "path" -> file.getAbsolutePath
      if (md5sum) map += "md5" -> parseChechsum(SummaryQScript.md5sumCache(file))
      key -> map.toMap
    }
  }

  def parseChechsum(checksumFile: File): String = {
    Source.fromFile(checksumFile).getLines().toList.head.split(" ")(0)
  }
}
