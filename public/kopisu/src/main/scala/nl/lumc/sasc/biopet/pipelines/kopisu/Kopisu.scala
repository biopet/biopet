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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.pipelines.kopisu

import nl.lumc.sasc.biopet.core.{BiopetQScript, PipelineCommand, Reference}
import nl.lumc.sasc.biopet.extensions.freec.{FreeC, FreeCAssessSignificancePlot, FreeCBAFPlot, FreeCCNVPlot}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.language.reflectiveCalls

class Kopisu(val root: Configurable) extends QScript with BiopetQScript with Reference {
  qscript =>
  def this() = this(null)

  @Input(doc = "Input bam file", required = true)
  var bamFile: File = _

  @Argument(doc = "Prefix name of output file", required = false)
  var outputName: String = _

  def init(): Unit = {
    if (outputName == null) outputName = bamFile.getName.stripSuffix(".bam")
  }

  // This script is in fact FreeC only.
  def biopetScript() {
    val freec = new FreeC(this)
    freec.input = bamFile
    freec.inputFormat = Some("BAM")
    freec.outputPath = new File(outputDir, "cnv")
    add(freec)

    /*
    * These scripts will wait for FreeC to Finish
    *
    * R-scripts to plot FreeC results
    * */
    val fcAssessSignificancePlot = new FreeCAssessSignificancePlot(this)
    fcAssessSignificancePlot.cnv = freec.cnvOutput
    fcAssessSignificancePlot.ratios = freec.ratioOutput
    fcAssessSignificancePlot.output = new File(outputDir, outputName + ".freec_significant_calls.txt")
    add(fcAssessSignificancePlot)

    val fcCnvPlot = new FreeCCNVPlot(this)
    fcCnvPlot.input = freec.ratioOutput
    fcCnvPlot.output = new File(outputDir, outputName + ".freec_cnv.png")
    add(fcCnvPlot)

    val fcBAFPlot = new FreeCBAFPlot(this)
    fcBAFPlot.input = freec.bafOutput
    fcBAFPlot.output = new File(outputDir, outputName + ".freec_baf.png")
    add(fcBAFPlot)
  }
}

object Kopisu extends PipelineCommand
