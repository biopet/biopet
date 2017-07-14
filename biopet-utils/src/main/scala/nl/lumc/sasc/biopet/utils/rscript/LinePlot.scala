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
package nl.lumc.sasc.biopet.utils.rscript

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable

/**
  * Extension for en general line plot with R
  *
  * Created by pjvan_thof on 4/29/15.
  */
class LinePlot(val parent: Configurable) extends Rscript {
  protected var script: File = config("script", default = "plotXY.R")

  var input: File = _

  var output: File = _

  var width: Option[Int] = config("width")
  var height: Option[Int] = config("height")
  var xlabel: Option[String] = config("xlabel")
  var ylabel: Option[String] = config("ylabel")
  var llabel: Option[String] = config("llabel")
  var title: Option[String] = config("title")
  var hideLegend: Boolean = config("hide_legend", default = false)
  var removeZero: Boolean = config("removeZero", default = false)

  // whether to use log scale for x and y axis
  var xLog10: Boolean = false
  var yLog10: Boolean = false

  var xLog10AxisTicks: Seq[String] = Seq.empty
  var xLog10AxisLabels: Seq[String] = Seq.empty

  override def cmd: Seq[String] =
    super.cmd ++
      Seq("--input", input.getAbsolutePath) ++
      Seq("--output", output.getAbsolutePath) ++
      width.map(x => Seq("--width", x.toString)).getOrElse(Seq()) ++
      height.map(x => Seq("--height", x.toString)).getOrElse(Seq()) ++
      xlabel.map(Seq("--xlabel", _)).getOrElse(Seq()) ++
      ylabel.map(Seq("--ylabel", _)).getOrElse(Seq()) ++
      llabel.map(Seq("--llabel", _)).getOrElse(Seq()) ++
      title.map(Seq("--title", _)).getOrElse(Seq()) ++
      (if (hideLegend) Seq("--hideLegend", "true") else Seq()) ++
      (if (removeZero) Seq("--removeZero", "true") else Seq()) ++
      (if (xLog10) Seq("--xLog10", "true") else Seq()) ++
      (if (yLog10) Seq("--yLog10", "true") else Seq()) ++
      (if (xLog10AxisTicks.nonEmpty) xLog10AxisTicks.+:("--xLog10Breaks") else Seq()) ++
      (if (xLog10AxisLabels.nonEmpty) xLog10AxisLabels.+:("--xLog10Labels") else Seq())
}

object LinePlot {
  def apply(inputTsv: File,
            outputFile: File,
            root: Configurable = null,
            xlabel: Option[String] = None,
            ylabel: Option[String] = None,
            width: Int = 1200,
            hideLegend: Boolean = false,
            removeZero: Boolean = false,
            title: Option[String] = None): LinePlot = {
    val plot = new LinePlot(root)
    plot.input = inputTsv
    plot.output = outputFile
    plot.xlabel = xlabel
    plot.ylabel = ylabel
    plot.width = Some(width)
    plot.hideLegend = hideLegend
    plot.removeZero = removeZero
    plot.title = title
    plot
  }
}
