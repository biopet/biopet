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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import nl.lumc.sasc.biopet.extensions.gatk.scatter.{GATKScatterFunction, LocusScatterFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Output, _}

class AnalyzeCovariates(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "AnalyzeCovariates"
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
  var ignoreLastModificationTimes: Boolean = config("ignoreLastModificationTimes", default = false)

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
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  override def cmdLine: String = super.cmdLine +
    optional("-before", beforeReportFile) +
    optional("-after", afterReportFile) +
    conditional(ignoreLastModificationTimes, "-ignoreLMT") +
    optional("-plots", plotsReportFile) +
    optional("-csv", intermediateCsvFile) +
    conditional(filter_reads_with_N_cigar, "-filterRNC") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ") +
    conditional(filter_bases_not_stored, "-filterNoBases")
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
