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

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ Reference, BiopetQScript, PipelineCommand }

import nl.lumc.sasc.biopet.extensions.freec.{ FreeC, FreeCCNVPlot, FreeCBAFPlot, FreeCAssessSignificancePlot }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import org.broadinstitute.gatk.queue.QScript

class Kopisu(val root: Configurable) extends QScript with BiopetQScript with Reference {
  qscript =>
  def this() = this(null)

  @Input(doc = "Input bam file", required = true)
  var bamFile: File = _

  var outputName: String = _

  def init(): Unit = {
    if (outputName == null) outputName = bamFile.getName.stripSuffix(".bam")
  }

  // This script is in fact FreeC only.
  def biopetScript() {
    // This script starts from a BAM alignment file and creates the pileup file

    // below is FreeC specific

    val sampileup = new SamtoolsMpileup(this)
    sampileup.input = List(bamFile)
    sampileup.output = new File(outputDir, outputName + ".pileup.gz")

    //TODO: need piping support
    val smilepig = new CommandLineFunction {
      @Input
      var input = List(bamFile)

      @Output
      var output = new File(outputDir, outputName + ".pileup.gz")

      val sampileup = new SamtoolsMpileup(qscript)
      sampileup.input ::= bamFile

      analysisName = "smilepig"
      nCoresRequest = 2
      isIntermediate = true
      //TODO: pigz must be a extension
      def commandLine: String = sampileup.cmdPipe + " | pigz -9 -p 4 -c > " + output.getAbsolutePath
    }
    add(smilepig)

    val FreeC = new FreeC(this)
    FreeC.input = smilepig.output
    FreeC.outputPath = new File(outputDir, "CNV")
    add(FreeC)

    /*
    * These scripts will wait for FreeC to Finish
    *
    * R-scripts to plot FreeC results
    * */
    val FCAssessSignificancePlot = new FreeCAssessSignificancePlot(this)
    FCAssessSignificancePlot.cnv = FreeC.CNVoutput
    FCAssessSignificancePlot.ratios = FreeC.RatioOutput
    FCAssessSignificancePlot.output = new File(outputDir, outputName + ".freec_significant_calls.txt")
    add(FCAssessSignificancePlot)

    val FCCnvPlot = new FreeCCNVPlot(this)
    FCCnvPlot.input = FreeC.RatioOutput
    FCCnvPlot.output = new File(outputDir, outputName + ".freec_cnv.png")
    add(FCCnvPlot)

    val FCBAFPlot = new FreeCBAFPlot(this)
    FCBAFPlot.input = FreeC.BAFoutput
    FCBAFPlot.output = new File(outputDir, outputName + ".freec_baf.png")
    add(FCBAFPlot)
  }
}

object Kopisu extends PipelineCommand
