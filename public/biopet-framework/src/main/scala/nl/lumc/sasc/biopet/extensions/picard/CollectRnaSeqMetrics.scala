/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

/**
 * Wrapper for the Picard CollectRnaSeqMetrics tool
 */
class CollectRnaSeqMetrics(val root: Configurable) extends Picard {

  javaMainClass = "picard.analysis.CollectRnaSeqMetrics"

  @Input(doc = "The input SAM or BAM files to analyze", required = true)
  var input: File = _

  @Input(doc = "Gene annotations in refFlat form", required = true)
  var refFlat: File = _

  @Output(doc = "Output metrics file", required = true)
  var output: File = _

  @Argument(doc = "Location of rRNA sequences in interval list format", required = false)
  var ribosomalIntervals: File = config("ribosomal_intervals")

  @Argument(doc = "Strand specificity", required = false)
  var strandSpecificity: String = config("strand_specificity")

  @Argument(doc = "Minimum length of transcripts to use for coverage-based values calculation", required = false)
  var minimumLength: Option[Int] = config("minimum_length")

  @Argument(doc = "PDF output of the coverage chart", required = false)
  var chartOutput: File = _

  @Argument(doc = "Sequences to ignore for mapped reads", required = false)
  var ignoreSequence: String = config("ignore_sequence")

  @Argument(doc = "Minimum overlap percentage a fragment must have to be considered rRNA", required = false)
  var rRNAFragmentPercentage: Double = config("rrna_fragment_percentage")

  @Argument(doc = "Metric accumulation level", required = false)
  var metricAccumulationLevel: String = config("metric_accumulation_level")

  @Argument(doc = "Reference FASTA sequence", required = false)
  var referenceSequence: File = config("reference_sequence")

  @Argument(doc = "Assume alignment file is sorted by position", required = false)
  var assumeSorted: Boolean = config("assume_sorted")

  @Argument(doc = "Stop after processing N reads", required = false)
  var stopAfter: Long = config("stop_after")

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
