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

  /**
   * Short name of beforeReportFile
   * @return Short name of beforeReportFile
   */
  def before = this.beforeReportFile

  /**
   * Short name of beforeReportFile
   * @param value Short name of beforeReportFile
   */
  def before_=(value: File) { this.beforeReportFile = value }

  /** file containing the BQSR second-pass report file */
  @Input(fullName = "afterReportFile", shortName = "after", doc = "file containing the BQSR second-pass report file", required = false, exclusiveOf = "", validation = "")
  var afterReportFile: File = _

  /**
   * Short name of afterReportFile
   * @return Short name of afterReportFile
   */
  def after = this.afterReportFile

  /**
   * Short name of afterReportFile
   * @param value Short name of afterReportFile
   */
  def after_=(value: File) { this.afterReportFile = value }

  /** do not emit warning messages related to suspicious last modification time order of inputs */
  @Argument(fullName = "ignoreLastModificationTimes", shortName = "ignoreLMT", doc = "do not emit warning messages related to suspicious last modification time order of inputs", required = false, exclusiveOf = "", validation = "")
  var ignoreLastModificationTimes: Boolean = _

  /**
   * Short name of ignoreLastModificationTimes
   * @return Short name of ignoreLastModificationTimes
   */
  def ignoreLMT = this.ignoreLastModificationTimes

  /**
   * Short name of ignoreLastModificationTimes
   * @param value Short name of ignoreLastModificationTimes
   */
  def ignoreLMT_=(value: Boolean) { this.ignoreLastModificationTimes = value }

  /** location of the output report */
  @Output(fullName = "plotsReportFile", shortName = "plots", doc = "location of the output report", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var plotsReportFile: File = _

  /**
   * Short name of plotsReportFile
   * @return Short name of plotsReportFile
   */
  def plots = this.plotsReportFile

  /**
   * Short name of plotsReportFile
   * @param value Short name of plotsReportFile
   */
  def plots_=(value: File) { this.plotsReportFile = value }

  /** location of the csv intermediate file */
  @Output(fullName = "intermediateCsvFile", shortName = "csv", doc = "location of the csv intermediate file", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var intermediateCsvFile: File = _

  /**
   * Short name of intermediateCsvFile
   * @return Short name of intermediateCsvFile
   */
  def csv = this.intermediateCsvFile

  /**
   * Short name of intermediateCsvFile
   * @param value Short name of intermediateCsvFile
   */
  def csv_=(value: File) { this.intermediateCsvFile = value }

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = _

  /**
   * Short name of filter_reads_with_N_cigar
   * @return Short name of filter_reads_with_N_cigar
   */
  def filterRNC = this.filter_reads_with_N_cigar

  /**
   * Short name of filter_reads_with_N_cigar
   * @param value Short name of filter_reads_with_N_cigar
   */
  def filterRNC_=(value: Boolean) { this.filter_reads_with_N_cigar = value }

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = _

  /**
   * Short name of filter_mismatching_base_and_quals
   * @return Short name of filter_mismatching_base_and_quals
   */
  def filterMBQ = this.filter_mismatching_base_and_quals

  /**
   * Short name of filter_mismatching_base_and_quals
   * @param value Short name of filter_mismatching_base_and_quals
   */
  def filterMBQ_=(value: Boolean) { this.filter_mismatching_base_and_quals = value }

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = _

  /**
   * Short name of filter_bases_not_stored
   * @return Short name of filter_bases_not_stored
   */
  def filterNoBases = this.filter_bases_not_stored

  /**
   * Short name of filter_bases_not_stored
   * @param value Short name of filter_bases_not_stored
   */
  def filterNoBases_=(value: Boolean) { this.filter_bases_not_stored = value }

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
    ac.before = before
    ac.after = after
    ac.plots = plots
    ac
  }
}
