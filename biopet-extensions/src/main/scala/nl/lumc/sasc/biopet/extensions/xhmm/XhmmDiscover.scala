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
package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

import scala.io.Source

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmDiscover(val parent: Configurable) extends Xhmm with Summarizable {

  @Input
  var inputMatrix: File = _

  @Output
  var outputXcnv: File = _

  @Input
  var r: File = _

  @Output
  private var _outputXcnvAuxFile: File = _

  def outputXcnvAuxFile = new File(outputXcnv.getAbsolutePath + ".aux")

  var xhmmAnalysisName: String = _

  override def beforeGraph() = {
    super.beforeGraph()
    if (outputXcnv == null) {
      throw new IllegalStateException("Must set output file")
    }
    _outputXcnvAuxFile = outputXcnvAuxFile
  }

  def cmdLine = {
    executable + required("--discover") +
      required("-r", inputMatrix) +
      required("-R", r) +
      required("-c", outputXcnv) +
      required("-a", outputXcnvAuxFile) +
      required("-p", discoverParamsFile) +
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
    samples.map { x =>
      val sampleLines = lines.filter(_.sample == x)
      x -> Map(
        "DEL" -> sampleLines.count(_.cnvType == "DEL"),
        "DUP" -> sampleLines.count(_.cnvType == "DUP"))
    }.toMap
  }

}
