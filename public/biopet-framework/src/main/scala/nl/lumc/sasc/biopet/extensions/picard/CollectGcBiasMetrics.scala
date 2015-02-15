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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

class CollectGcBiasMetrics(val root: Configurable) extends Picard {
  javaMainClass = "picard.analysis.CollectGcBiasMetrics"

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: Seq[File] = Nil

  @Output(doc = "The output file to write statistics to", required = true)
  var output: File = _

  @Output(doc = "Output chart", required = false)
  var outputChart: File = _

  @Output(doc = "Output summary", required = false)
  var outputSummary: File = _

  @Argument(doc = "Reference file", required = false)
  var reference: File = config("reference")

  @Argument(doc = "Window size", required = false)
  var windowSize: Option[Int] = config("windowsize")

  @Argument(doc = "MINIMUM_GENOME_FRACTION", required = false)
  var minGenomeFraction: Option[Double] = config("mingenomefraction")

  @Argument(doc = "ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumesorted", default = true)

  @Argument(doc = "IS_BISULFITE_SEQUENCED", required = false)
  var isBisulfiteSequinced: Option[Boolean] = config("isbisulfitesequinced")

  override def beforeGraph {
    if (outputChart == null) outputChart = new File(output + ".pdf")
    //require(reference.exists)
  }

  override def commandLine = super.commandLine +
    repeat("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    optional("CHART_OUTPUT=", outputChart, spaceSeparated = false) +
    required("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
    optional("SUMMARY_OUTPUT=", outputSummary, spaceSeparated = false) +
    optional("WINDOW_SIZE=", windowSize, spaceSeparated = false) +
    optional("MINIMUM_GENOME_FRACTION=", minGenomeFraction, spaceSeparated = false) +
    conditional(assumeSorted, "ASSUME_SORTED=TRUE") +
    conditional(isBisulfiteSequinced.getOrElse(false), "IS_BISULFITE_SEQUENCED=TRUE")
}

object CollectGcBiasMetrics {
  def apply(root: Configurable, input: File, outputDir: File): CollectGcBiasMetrics = {
    val collectGcBiasMetrics = new CollectGcBiasMetrics(root)
    collectGcBiasMetrics.input :+= input
    collectGcBiasMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".gcbiasmetrics")
    return collectGcBiasMetrics
  }
}
