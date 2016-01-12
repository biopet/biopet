package nl.lumc.sasc.biopet.pipelines.toucan

import java.io.File

import nl.lumc.sasc.biopet.extensions.manwe.{ ManweAnnotateVcf, ManweSamplesActivate, ManweSamplesImport }
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.io.Source

/**
 * Created by ahbbollen on 9-10-15.
 * Wrapper for manwe activate after importing and annotating
 */
class ManweActivateAfterAnnotImport(root: Configurable,
                                    annotate: ManweAnnotateVcf,
                                    imported: ManweSamplesImport) extends ManweSamplesActivate(root) {

  override def beforeGraph: Unit = {
    super.beforeGraph
    require(annotate != null, "Annotate should be defined")
    require(imported != null, "Imported should be defined")
    this.deps :+= annotate.jobOutputFile
    this.deps :+= imported.jobOutputFile
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
      it.filter(_.contains("Added sample")).toList.head.split(" ").last
    } else {
      ""
    }
    r
  }

  def getUri: String = {
    val err: Option[File] = Some(imported.jobOutputFile)
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
    required("samples") + required("activate") + getUri
  }

}
