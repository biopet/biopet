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
package nl.lumc.sasc.biopet.pipelines.kopisu.methods

import java.io.File

import nl.lumc.sasc.biopet.extensions.freec.{ FreeC, FreeCAssessSignificancePlot, FreeCBAFPlot, FreeCCNVPlot }
import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by pjvanthof on 10/05/16.
 */
class FreecMethod(val parent: Configurable) extends CnvMethod {
  def name = "freec"

  var snpFile: Option[File] = config("snp_file", freeVar = false)

  def biopetScript: Unit = {
    inputBams.foreach {
      case (sampleName, bamFile) =>

        val sampleOutput = new File(outputDir, sampleName)

        val freec = new FreeC(this)
        freec.input = bamFile
        freec.inputFormat = Some("BAM")
        freec.outputPath = sampleOutput
        freec.snpFile = snpFile
        add(freec)

        /*
      * These scripts will wait for FreeC to Finish
      *
      * R-scripts to plot FreeC results
      * */
        val fcAssessSignificancePlot = new FreeCAssessSignificancePlot(this)
        fcAssessSignificancePlot.cnv = freec.cnvOutput
        fcAssessSignificancePlot.ratios = freec.ratioOutput
        fcAssessSignificancePlot.output = new File(sampleOutput, sampleName + ".freec_significant_calls.txt")
        add(fcAssessSignificancePlot)
        addOutput(sampleName, fcAssessSignificancePlot.output)

        val fcCnvPlot = new FreeCCNVPlot(this)
        fcCnvPlot.input = freec.ratioOutput
        fcCnvPlot.output = new File(sampleOutput, sampleName + ".freec_cnv")
        add(fcCnvPlot)

        snpFile.foreach { _ =>
          val fcBAFPlot = new FreeCBAFPlot(this)
          fcBAFPlot.input = freec.bafOutput
          fcBAFPlot.output = new File(sampleOutput, sampleName + ".freec_baf")
          add(fcBAFPlot)
        }
    }

    addSummaryJobs()
  }

  override def summaryFiles = super.summaryFiles ++ snpFile.map("snp_file" -> _)
}
