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
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

import scala.collection.immutable.Nil

class CollectInsertSizeMetrics(val root: Configurable) extends Picard with Summarizable {
  javaMainClass = "picard.analysis.CollectInsertSizeMetrics"

  @Input(doc = "The input SAM or BAM files to analyze.  Must be coordinate sorted.", required = true)
  var input: File = _

  @Output(doc = "The output file to write statistics to", required = true)
  var output: File = _

  @Output(doc = "Output histogram", required = true)
  def outputHistogram: File = new File(output + ".pdf")

  @Argument(doc = "Reference file", required = false)
  var reference: File = config("reference")

  @Argument(doc = "DEVIATIONS", required = false)
  var deviations: Option[Double] = config("deviations")

  @Argument(doc = "MINIMUM_PCT", required = false)
  var minPct: Option[Double] = config("minpct")

  @Argument(doc = "ASSUME_SORTED", required = false)
  var assumeSorted: Boolean = config("assumesorted", default = true)

  @Argument(doc = "STOP_AFTER", required = false)
  var stopAfter: Option[Long] = config("stopAfter")

  @Argument(doc = "METRIC_ACCUMULATION_LEVEL", required = false)
  var metricAccumulationLevel: List[String] = config("metricaccumulationlevel", default = Nil)

  @Argument(doc = "HISTOGRAM_WIDTH", required = false)
  var histogramWidth: Option[Int] = config("histogramWidth")

  override def beforeGraph {
    //if (outputHistogram == null) outputHistogram = new File(output + ".pdf")
    //require(reference.exists)
  }

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    optional("HISTOGRAM_FILE=", outputHistogram, spaceSeparated = false) +
    required("REFERENCE_SEQUENCE=", reference, spaceSeparated = false) +
    optional("DEVIATIONS=", deviations, spaceSeparated = false) +
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
    optional("STOP_AFTER=", stopAfter, spaceSeparated = false) +
    optional("HISTOGRAM_WIDTH=", histogramWidth, spaceSeparated = false) +
    conditional(assumeSorted, "ASSUME_SORTED=TRUE")

  def summaryFiles: Map[String, File] = Map("output_histogram" -> outputHistogram)

  def summaryStats: Map[String, Any] = {
    val (header, content) = Picard.getMetrics(output)
    (for (i <- 0 to header.size if i < content.head.size)
      yield (header(i).toLowerCase -> content.head(i))).toMap
  }
}

object CollectInsertSizeMetrics {
  def apply(root: Configurable, input: File, outputDir: File): CollectInsertSizeMetrics = {
    val collectInsertSizeMetrics = new CollectInsertSizeMetrics(root)
    collectInsertSizeMetrics.input = input
    collectInsertSizeMetrics.output = new File(outputDir, input.getName.stripSuffix(".bam") + ".insertsizemetrics")
    return collectInsertSizeMetrics
  }
}