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

  @Argument(required = false)
  var calculateCoveraOverGenes: Option[File] = None

  @Argument(required = false)
  var countType: Option[String] = None

  @Argument(required = false)
  var maxBaseQuality: Option[Int] = None

  @Argument(required = false)
  var maxMappingQuality: Option[Int] = None

  @Argument(required = false)
  var minBaseQuality: Option[Int] = None

  @Argument(required = false)
  var minMappingQuality: Option[Int] = None

  @Argument(required = false)
  var outputFormat: Option[String] = None

  @Argument(required = false)
  var partitionType: Option[String] = None

  @Argument(required = false)
  var omitDepthOutputAtEachBase: Boolean = false

  @Argument(required = false)
  var omitIntervalStatistics: Boolean = false

  @Argument(required = false)
  var omitLocusTable: Boolean = false

  @Argument(required = false)
  var omitPerSampleStats: Boolean = false

  @Argument(required = false)
  var printBaseCounts: Boolean = false

  @Argument(required = false)
  var nBins: Option[Int] = None

  @Argument(required = false)
  var start: Option[Int] = None

  @Argument(required = false)
  var stop: Option[Int] = None

  @Argument(required = false)
  var summaryCoverageThreshold: Option[Int] = None

  @Argument(required = false)
  var ignoreDeletionSites: Boolean = false

  @Argument(required = false)
  var includeDeletions: Boolean = false

  @Argument(required = false)
  var includeRefNSites: Boolean = false

  @Argument(required = false)
  var printBinEndpointsAndExit: Boolean = false

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
      optional("--calculateCoverageOverGenes", calculateCoveraOverGenes) +
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
