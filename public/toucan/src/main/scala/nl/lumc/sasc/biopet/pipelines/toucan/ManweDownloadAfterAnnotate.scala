package nl.lumc.sasc.biopet.pipelines.toucan

import java.io.File

import nl.lumc.sasc.biopet.extensions.manwe.{ ManweAnnotateVcf, ManweDataSourcesDownload }
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.io.Source

/**
 * Created by ahbbollen on 9-10-15.
 */
class ManweDownloadAfterAnnotate(root: Configurable,
                                 annotate: ManweAnnotateVcf) extends ManweDataSourcesDownload(root) {

  override def beforeGraph: Unit = {
    super.beforeGraph
    require(annotate != null, "Annotate should be defined")
    this.deps :+= annotate.jobOutputFile
  }

  override def beforeCmd: Unit = {
    super.beforeCmd

    this.uri = getUri
  }

  def getUriFromFile(f: File): String = {
    val r = if (f.exists()) {
      val reader = Source.fromFile(f)
      val it = reader.getLines()
      if (it.isEmpty) {
        throw new IllegalArgumentException("Empty manwe stderr file")
      }
      it.filter(_.contains("Annotated VCF file")).toList.head.split(" ").last
    } else {
      ""
    }
    r
  }

  def getUri: String = {
    val err: Option[File] = Some(annotate.jobOutputFile)
    uri = err match {
      case None => ""
      case Some(s) => s match {
        case null  => ""
        case other => getUriFromFile(other)
      }
      case _ => ""
    }

    uri
  }

  override def subCommand = {
    required("data-sources") + required("download") + getUri
  }

}
