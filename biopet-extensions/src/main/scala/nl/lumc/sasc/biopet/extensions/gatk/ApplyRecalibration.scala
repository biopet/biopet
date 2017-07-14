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
import nl.lumc.sasc.biopet.extensions.gatk.gather.GatherVcfs
import nl.lumc.sasc.biopet.extensions.gatk.scatter.{GATKScatterFunction, LocusScatterFunction}
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Input, Output}

class ApplyRecalibration(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "ApplyRecalibration"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** The raw input variants to be recalibrated */
  @Input(fullName = "input", shortName = "input", doc = "The raw input variants to be recalibrated", required = true, exclusiveOf = "", validation = "")
  var input: Seq[File] = Nil

  /** The input recal file used by ApplyRecalibration */
  @Input(fullName = "recal_file", shortName = "recalFile", doc = "The input recal file used by ApplyRecalibration", required = true, exclusiveOf = "", validation = "")
  var recal_file: File = _

  /** The input tranches file describing where to cut the data */
  @Input(fullName = "tranches_file", shortName = "tranchesFile", doc = "The input tranches file describing where to cut the data", required = false, exclusiveOf = "", validation = "")
  var tranches_file: File = _

  /** The output filtered and recalibrated VCF file in which each variant is annotated with its VQSLOD value */
  @Output(fullName = "out", shortName = "o", doc = "The output filtered and recalibrated VCF file in which each variant is annotated with its VQSLOD value", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[GatherVcfs])
  var out: File = _

  /** The truth sensitivity level at which to start filtering */
  @Argument(fullName = "ts_filter_level", shortName = "ts_filter_level", doc = "The truth sensitivity level at which to start filtering", required = false, exclusiveOf = "", validation = "")
  var ts_filter_level: Option[Double] = config("ts_filter_level")

  /** Format string for ts_filter_level */
  @Argument(fullName = "ts_filter_levelFormat", shortName = "", doc = "Format string for ts_filter_level", required = false, exclusiveOf = "", validation = "")
  var ts_filter_levelFormat: String = "%s"

  /** The VQSLOD score below which to start filtering */
  @Argument(fullName = "lodCutoff", shortName = "lodCutoff", doc = "The VQSLOD score below which to start filtering", required = false, exclusiveOf = "", validation = "")
  var lodCutoff: Option[Double] = config("lodCutoff")

  /** Format string for lodCutoff */
  @Argument(fullName = "lodCutoffFormat", shortName = "", doc = "Format string for lodCutoff", required = false, exclusiveOf = "", validation = "")
  var lodCutoffFormat: String = "%s"

  /** If specified, the recalibration will be applied to variants marked as filtered by the specified filter name in the input VCF file */
  @Argument(fullName = "ignore_filter", shortName = "ignoreFilter", doc = "If specified, the recalibration will be applied to variants marked as filtered by the specified filter name in the input VCF file", required = false, exclusiveOf = "", validation = "")
  var ignore_filter: List[String] = config("ignore_filter", default = Nil)

  /** If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file. */
  @Argument(fullName = "ignore_all_filters", shortName = "ignoreAllFilters", doc = "If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file.", required = false, exclusiveOf = "", validation = "")
  var ignore_all_filters: Boolean = config("ignore_all_filters", default = false)

  /** Don't output filtered loci after applying the recalibration */
  @Argument(fullName = "excludeFiltered", shortName = "ef", doc = "Don't output filtered loci after applying the recalibration", required = false, exclusiveOf = "", validation = "")
  var excludeFiltered: Boolean = config("excludeFiltered", default = false)

  /** Recalibration mode to employ: 1.) SNP for recalibrating only SNPs (emitting indels untouched in the output VCF); 2.) INDEL for indels; and 3.) BOTH for recalibrating both SNPs and indels simultaneously. */
  @Argument(fullName = "mode", shortName = "mode", doc = "Recalibration mode to employ: 1.) SNP for recalibrating only SNPs (emitting indels untouched in the output VCF); 2.) INDEL for indels; and 3.) BOTH for recalibrating both SNPs and indels simultaneously.", required = false, exclusiveOf = "", validation = "")
  var mode: String = _

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  @Output
  @Gather(enabled = false)
  private var outputIndex: File = _

  override def beforeGraph() {
    super.beforeGraph()
    deps ++= input.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (recal_file != null)
      deps :+= VcfUtils.getVcfIndexFile(recal_file)
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
    num_threads = Option(getThreads)
  }

  override def cmdLine: String = super.cmdLine +
    repeat("-input", input, formatPrefix = TaggedFile.formatCommandLineParameter) +
    required(TaggedFile.formatCommandLineParameter("-recalFile", recal_file), recal_file) +
    optional("-tranchesFile", tranches_file) +
    optional("-o", out) +
    optional("-ts_filter_level", ts_filter_level, format = ts_filter_levelFormat) +
    optional("-lodCutoff", lodCutoff, format = lodCutoffFormat) +
    repeat("-ignoreFilter", ignore_filter) + conditional(ignore_all_filters, "-ignoreAllFilters") +
    conditional(excludeFiltered, "-ef") +
    optional("-mode", mode) +
    conditional(filter_reads_with_N_cigar, "-filterRNC") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ") +
    conditional(filter_bases_not_stored, "-filterNoBases")
}
