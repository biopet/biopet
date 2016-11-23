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
  var out: String = _

  lazy val outFile: File = {
    new File(out)
  }

  lazy val summaryFile: File = {
    new File(out + "_summary")
  }

  lazy val statisticsFile: File = {
    new File(out + "_statistics")
  }

  lazy val intervalSummaryFile: File = {
    new File(out + "_interval_summary")
  }

  lazy val intervalStatisticsFile: File = {
    new File(out + "_interval_statistics")
  }

  lazy val geneSummaryFile: File = {
    new File(out + "_gene_summary")
  }

  lazy val geneStatisticsFile: File = {
    new File(out + "_gene_statistics")
  }

  lazy val cumulativeCoverageCountsFile: File = {
    new File(out + "_cumulative_coverage_counts")
  }

  lazy val cumulativeCoverageProportionsFile: File = {
    new File(out + "_cumulative_coverage_proportions")
  }

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
