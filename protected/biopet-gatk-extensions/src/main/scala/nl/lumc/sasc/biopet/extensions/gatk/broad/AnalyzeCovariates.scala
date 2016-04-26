/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ GATKScatterFunction, LocusScatterFunction }
import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class AnalyzeCovariates(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "AnalyzeCovariates"
  analysis_type = "AnalyzeCovariates"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** file containing the BQSR first-pass report file */
  @Input(fullName = "beforeReportFile", shortName = "before", doc = "file containing the BQSR first-pass report file", required = false, exclusiveOf = "", validation = "")
  var beforeReportFile: File = _

  /** file containing the BQSR second-pass report file */
  @Input(fullName = "afterReportFile", shortName = "after", doc = "file containing the BQSR second-pass report file", required = false, exclusiveOf = "", validation = "")
  var afterReportFile: File = _

  /** do not emit warning messages related to suspicious last modification time order of inputs */
  @Argument(fullName = "ignoreLastModificationTimes", shortName = "ignoreLMT", doc = "do not emit warning messages related to suspicious last modification time order of inputs", required = false, exclusiveOf = "", validation = "")
  var ignoreLastModificationTimes: Boolean = _

  /** location of the output report */
  @Output(fullName = "plotsReportFile", shortName = "plots", doc = "location of the output report", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var plotsReportFile: File = _

  /** location of the csv intermediate file */
  @Output(fullName = "intermediateCsvFile", shortName = "csv", doc = "location of the csv intermediate file", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var intermediateCsvFile: File = _

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = _

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = _

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = _

  override def cmdLine = super.cmdLine +
    optional("-before", beforeReportFile, spaceSeparated = true, escape = true, format = "%s") +
    optional("-after", afterReportFile, spaceSeparated = true, escape = true, format = "%s") +
    conditional(ignoreLastModificationTimes, "-ignoreLMT", escape = true, format = "%s") +
    optional("-plots", plotsReportFile, spaceSeparated = true, escape = true, format = "%s") +
    optional("-csv", intermediateCsvFile, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object AnalyzeCovariates {
  def apply(root: Configurable, before: File, after: File, plots: File): AnalyzeCovariates = {
    val ac = new AnalyzeCovariates(root)
    ac.beforeReportFile = before
    ac.afterReportFile = after
    ac.plotsReportFile = plots
    ac
  }
}
