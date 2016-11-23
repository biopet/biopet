package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

import scala.io.Source

/**
  * Created by Sander Bollen on 23-11-16.
  */
class XhmmDiscover(val root: Configurable) extends Xhmm with Summarizable {

  @Input
  var inputMatrix: File = _

  @Output
  var outputXcnv: File = _

  @Input
  var r: File = _

  @Argument
  var discoverParamsFile: File = config("discover_params", namespace = "xhmm")

  private lazy val outputXcnvAuxFile: File = {
    new File(outputXcnv.getAbsolutePath + ".aux")
  }

  @Argument
  var xhmmAnalysisName: String = _

  def cmdLine = {
    executable + required("--discover") +
      required("-r", inputMatrix) +
      required("-R", r) +
      required("-c", outputXcnv) +
      required("-a", outputXcnvAuxFile) +
      required("-s", xhmmAnalysisName)
  }

  case class XcnvLine(sample: String, cnvType: String, location: String)

  def summaryFiles: Map[String, File] = Map()

  def summaryStats: Map[String, Any] = {
    val lines = Source.fromFile(outputXcnv).
      getLines().
      filter(p => !p.startsWith("SAMPLE")).
      map(x => x.split("\t")).
      map(x => XcnvLine(x(0), x(1), x(2))).toList

    val samples = lines.map(_.sample)
    samples.map{x =>
      val sampleLines = lines.filter(_.sample == x)
      x -> Map(
        "DEL" -> sampleLines.count(_.cnvType == "DEL"),
        "DUP" -> sampleLines.count(_.cnvType == "DUP"))
    }.toMap
  }

}
