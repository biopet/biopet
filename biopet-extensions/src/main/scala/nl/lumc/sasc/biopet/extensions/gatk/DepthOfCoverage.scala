package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output }

/**
 * Created by Sander Bollen on 23-11-16.
 */
class DepthOfCoverage(val root: Configurable) extends CommandLineGATK {

  def analysis_type = "DepthOfCoverage"

  /*
  This tool creates several files with `out` as the root
   */
  @Output(fullName = "out", shortName = "o", doc = "File name base to which coverage metric should be written")
  var out: File = _

  private var _summaryFile: File = _
  private var _statisticsFile: File = _
  private var _intervalSummaryFile: File =  _
  private var _intervalStatisticsFile: File = _
  private var _geneSummaryFile: File = _
  private var _geneStatisticsFile: File = _
  private var _cumulativeCoverageCountsFile: File =  _
  private var _cumulativeCoverageProportionsFile: File = _

  @Output
  var summaryFile = _summaryFile
  @Output
  var statisticsFile = _statisticsFile
  @Output
  var intervalSummaryFile = _intervalSummaryFile
  @Output
  var intervalStatisticsFile = _intervalStatisticsFile
  @Output
  var geneSummaryFile = _geneSummaryFile
  @Output
  var geneStatisticsFile = _geneStatisticsFile
  @Output
  var culumativeCoverageCountsFile = _cumulativeCoverageCountsFile
  @Output
  var cumulativeCoverageProportionsFile = _cumulativeCoverageProportionsFile

  var calculateCoverageOverGenes: Option[File] = config("calculate_coverage_over_genes", namespace = "depth_of_coverage", default = None)

  var countType: Option[String] = config("count_type", namespace = "depth_of_coverage", default = None)

  var maxBaseQuality: Option[Int] = config("max_base_quality", namespace = "depth_of_coverage", default = None)

  var maxMappingQuality: Option[Int] = config("max_mapping_quality", namespace = "depth_of_coverage", default = None)

  var minBaseQuality: Option[Int] = config("min_base_quality", namespace = "depth_of_coverage", default = None)

  var minMappingQuality: Option[Int] = config("min_mapping_quality", namespace = "depth_of_coverage", default = None)

  var outputFormat: Option[String] = config("output_format", namespace = "depth_of_coverage", default = None)

  var partitionType: Option[String] = config("partition_type", namespace = "depth_of_coverage", default = None)

  var omitDepthOutputAtEachBase: Boolean = config("omit_depth_output_at_each_base", namespace = "depth_of_coverage", default = false)

  var omitIntervalStatistics: Boolean = config("omit_interval_statistics", namespace = "depth_of_coverage", default = false)

  var omitLocusTable: Boolean = config("omit_locus_table", namespace = "depth_of_coverage", default = false)

  var omitPerSampleStats: Boolean = config("omit_per_sample_stats", namespace = "depth_of_coverage", default = false)

  var printBaseCounts: Boolean = config("print_base_counts", namespace = "depth_of_coverage", default = false)

  var nBins: Option[Int] = config("n_bins", namespace = "depth_of_coverage", default = None)

  var start: Option[Int] = config("start", namespace = "depth_of_coverage", default = None)

  var stop: Option[Int] = config("stop", namespace = "depth_of_coverage", default = None)

  var summaryCoverageThreshold: Option[Int] = config("summary_coverage_threshold", namespace = "depth_of_coverage", default = None)

  var ignoreDeletionSites: Boolean = config("ignore_deletion_sites", namespace = "depth_of_coverage", default = false)

  var includeDeletions: Boolean = config("include_deletions", namespace = "depth_of_coverage", default = false)

  var includeRefNSites: Boolean = config("include_RefNSites", namespace = "depth_of_coverage", default = false)

  var printBinEndpointsAndExit: Boolean = config("print_bin_endpoint_and_exit", namespace = "depth_of_coverage" , default = false)

  override def beforeGraph() = {
    super.beforeGraph()
    if (out == null) {
      throw new IllegalStateException("You must set the <out> variable")
    }
    _summaryFile = new File(out + ".sample_summary")
    _statisticsFile = new File(out + ".sample_statistics")
    _intervalSummaryFile =  new File(out + ".sample_interval_summary")
    _intervalStatisticsFile = new File(out + ".sample_interval_statistics")
    _geneSummaryFile = new File(out + ".sample_gene_summary")
    _geneStatisticsFile = new File(out + ".sample_gene_statistics")
    _cumulativeCoverageCountsFile =  new File(out + ".sample_cumulative_coverage_counts")
    _cumulativeCoverageProportionsFile = new File(out + ".sample_cumulative_coverage_proportions")
  }

  override def cmdLine = {
    super.cmdLine + required("--out", out) +
      optional("--calculateCoverageOverGenes", calculateCoverageOverGenes) +
      optional("--countType", countType) +
      optional("--maxBaseQuality", maxBaseQuality) +
      optional("--maxMappingQuality", maxMappingQuality) +
      optional("--minBaseQuality", minBaseQuality) +
      optional("--minMappingQuality", minMappingQuality) +
      optional("--outputFormat", outputFormat) +
      optional("--partitionType", partitionType) +
      conditional(omitDepthOutputAtEachBase, "--omitDepthOutputAtEachBase") +
      conditional(omitIntervalStatistics, "--omitIntervalStatistics") +
      conditional(omitLocusTable, "--omitLocusTable") +
      conditional(omitPerSampleStats, "--omitPerSampleStats") +
      conditional(printBaseCounts, "--printBaseCounts") +
      optional("--nBins", nBins) +
      optional("--start", start) +
      optional("--stop", stop) +
      optional("--summaryCoverageThreshold", summaryCoverageThreshold) +
      conditional(ignoreDeletionSites, "--ignoreDeletionSites") +
      conditional(includeDeletions, "--includeDeletions") +
      conditional(includeRefNSites, "--includeRefNSites") +
      conditional(printBinEndpointsAndExit, "--printBinEndPointsAndExit")
  }
}
