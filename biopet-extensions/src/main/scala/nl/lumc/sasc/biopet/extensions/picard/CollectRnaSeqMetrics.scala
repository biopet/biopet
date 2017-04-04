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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity

/**
 * Wrapper for the Picard CollectRnaSeqMetrics tool
 */
class CollectRnaSeqMetrics(val parent: Configurable) extends Picard with Summarizable {

  javaMainClass = new picard.analysis.CollectRnaSeqMetrics().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = null

  @Input(doc = "Gene annotations in refFlat form", required = true)
  var refFlat: File = null

  @Input(doc = "Location of rRNA sequences in interval list format", required = false)
  var ribosomalIntervals: Option[File] = None

  @Output(doc = "Output metrics file", required = true)
  var output: File = null

  @Output(doc = "PDF output of the coverage chart", required = false)
  var chartOutput: Option[File] = config("chart_output")

  @Argument(doc = "Strand specificity", required = false)
  var strandSpecificity: Option[String] = config("strand_specificity")

  @Argument(doc = "Minimum length of transcripts to use for coverage-based values calculation", required = false)
  var minimumLength: Option[Int] = config("minimum_length")

  @Argument(doc = "Sequences to ignore for mapped reads", required = false)
  var ignoreSequence: Option[String] = config("ignore_sequence")

  @Argument(doc = "Minimum overlap percentage a fragment must have to be considered rRNA", required = false)
  var rRNAFragmentPercentage: Option[Double] = config("rrna_fragment_percentage")

  @Argument(doc = "Metric accumulation level", required = false)
  var metricAccumulationLevel: Option[String] = config("metric_accumulation_level")

  @Argument(doc = "Reference FASTA sequence", required = false)
  var referenceSequence: Option[File] = config("reference_sequence")

  @Argument(doc = "Assume alignment file is sorted by position", required = false)
  var assumeSorted: Boolean = config("assume_sorted", default = false)

  @Argument(doc = "Stop after processing N reads", required = false)
  var stopAfter: Option[Long] = config("stop_after")

  override def beforeGraph(): Unit = {
    if (refFlat == null) refFlat = config("refFlat")
    val validFlags = StrandSpecificity.values.map(_.toString).toSet
    strandSpecificity match {
      case Some(s) => require(validFlags.contains(s),
        s"Invalid Picard CollectRnaSeqMetrics strand specificity flag: $s. Valid values are " + validFlags.mkString(", "))
      case None => ;
    }
  }

  def summaryFiles: Map[String, File] = Map(
    "metrics" -> output,
    "annotation" -> refFlat
  ) ++ Map(
      "ribosomal_intervals" -> ribosomalIntervals,
      "output_chart" -> chartOutput
    ).collect { case (key, Some(value)) => key -> value }

  def summaryStats = Map(
    "metrics" -> Picard.getMetrics(output).getOrElse(Map()),
    "histogram" -> Picard.getHistogram(output).getOrElse(Map()))

  override def cmdLine = super.cmdLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("REF_FLAT=", refFlat, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("RIBOSOMAL_INTERVALS=", ribosomalIntervals, spaceSeparated = false) +
    required("STRAND_SPECIFICITY=", strandSpecificity, spaceSeparated = false) +
    required("MINIMUM_LENGTH=", minimumLength, spaceSeparated = false) +
    required("CHART_OUTPUT=", chartOutput, spaceSeparated = false) +
    optional("IGNORE_SEQUENCE=", ignoreSequence, spaceSeparated = false) +
    required("RRNA_FRAGMENT_PERCENTAGE=", rRNAFragmentPercentage, spaceSeparated = false) +
    optional("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
    required("REFERENCE_SEQUENCE=", referenceSequence, spaceSeparated = false) +
    required("ASSUME_SORTED=", assumeSorted, spaceSeparated = false) +
    required("STOP_AFTER=", stopAfter, spaceSeparated = false)
}
