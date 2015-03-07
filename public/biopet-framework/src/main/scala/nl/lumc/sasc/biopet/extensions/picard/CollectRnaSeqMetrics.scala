/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import picard.analysis.directed.RnaSeqMetricsCollector.StrandSpecificity

/**
 * Wrapper for the Picard CollectRnaSeqMetrics tool
 */
class CollectRnaSeqMetrics(val root: Configurable) extends Picard with Summarizable {

  javaMainClass = "picard.analysis.CollectRnaSeqMetrics"

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = null

  @Input(doc = "Gene annotations in refFlat form", required = true)
  var refFlat: File = null

  @Output(doc = "Output metrics file", required = true)
  var output: File = null

  @Argument(doc = "Location of rRNA sequences in interval list format", required = false)
  var ribosomalIntervals: Option[File] = config("ribosomal_intervals")

  @Argument(doc = "Strand specificity", required = false)
  var strandSpecificity: Option[String] = config("strand_specificity")

  @Argument(doc = "Minimum length of transcripts to use for coverage-based values calculation", required = false)
  var minimumLength: Option[Int] = config("minimum_length")

  @Argument(doc = "PDF output of the coverage chart", required = false)
  var chartOutput: Option[File] = config("chart_output")

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

  override def beforeGraph: Unit = {
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
      "ribosomal_intervals" -> ribosomalIntervals
    // TODO: this is disabled now since the file *may* not exist (e.g. when gene coverage is low)
    //       and it breaks the summary md5 creation
    //"output_chart" -> chartOutput
    ).collect { case (key, Some(value)) => key -> value }

  def summaryStats: Map[String, Any] = Picard.getMetrics(output) match {
    case None => Map()
    case Some((header, content)) =>
      header
        .zip(content)
        .map { case (h, c) => h.toLowerCase -> c.head }
        .toMap
  }

  override def commandLine = super.commandLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("REF_FLAT=", refFlat, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    required("RIBOSOMAL_INTERVALS=", ribosomalIntervals, spaceSeparated = false) +
    required("STRAND_SPECIFICITY=", strandSpecificity, spaceSeparated = false) +
    required("MINIMUM_LENGTH=", minimumLength, spaceSeparated = false) +
    required("CHART_OUTPUT=", chartOutput, spaceSeparated = false) +
    repeat("IGNORE_SEQUENCE=", ignoreSequence, spaceSeparated = false) +
    required("RRNA_FRAGMENT_PERCENTAGE=", rRNAFragmentPercentage, spaceSeparated = false) +
    repeat("METRIC_ACCUMULATION_LEVEL=", metricAccumulationLevel, spaceSeparated = false) +
    required("REFERENCE_SEQUENCE=", referenceSequence, spaceSeparated = false) +
    required("ASSUME_SORTED=", assumeSorted, spaceSeparated = false) +
    required("STOP_AFTER=", stopAfter, spaceSeparated = false)
}
