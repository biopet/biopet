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
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{ Gather, Input, Output, _ }

class UnifiedGenotyper(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "UnifiedGenotyper"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** Genotype likelihoods calculation model to employ -- SNP is the default option, while INDEL is also available for calling indels and BOTH is available for calling both together */
  @Argument(fullName = "genotype_likelihoods_model", shortName = "glm", doc = "Genotype likelihoods calculation model to employ -- SNP is the default option, while INDEL is also available for calling indels and BOTH is available for calling both together", required = false, exclusiveOf = "", validation = "")
  var genotype_likelihoods_model: Option[String] = config("genotype_likelihoods_model")

  /** The PCR error rate to be used for computing fragment-based likelihoods */
  @Argument(fullName = "pcr_error_rate", shortName = "pcr_error", doc = "The PCR error rate to be used for computing fragment-based likelihoods", required = false, exclusiveOf = "", validation = "")
  var pcr_error_rate: Option[Double] = config("pcr_error_rate")

  /** Format string for pcr_error_rate */
  @Argument(fullName = "pcr_error_rateFormat", shortName = "", doc = "Format string for pcr_error_rate", required = false, exclusiveOf = "", validation = "")
  var pcr_error_rateFormat: String = "%s"

  /** If provided, we will calculate the SLOD (SB annotation) */
  @Argument(fullName = "computeSLOD", shortName = "slod", doc = "If provided, we will calculate the SLOD (SB annotation)", required = false, exclusiveOf = "", validation = "")
  var computeSLOD: Boolean = config("computeSLOD", default = false)

  /** The PairHMM implementation to use for -glm INDEL genotype likelihood calculations */
  @Argument(fullName = "pair_hmm_implementation", shortName = "pairHMM", doc = "The PairHMM implementation to use for -glm INDEL genotype likelihood calculations", required = false, exclusiveOf = "", validation = "")
  var pair_hmm_implementation: Option[String] = config("pair_hmm_implementation")

  /** Minimum base quality required to consider a base for calling */
  @Argument(fullName = "min_base_quality_score", shortName = "mbq", doc = "Minimum base quality required to consider a base for calling", required = false, exclusiveOf = "", validation = "")
  var min_base_quality_score: Option[Int] = config("min_base_quality_score")

  /** Maximum fraction of reads with deletions spanning this locus for it to be callable */
  @Argument(fullName = "max_deletion_fraction", shortName = "deletions", doc = "Maximum fraction of reads with deletions spanning this locus for it to be callable", required = false, exclusiveOf = "", validation = "")
  var max_deletion_fraction: Option[Double] = config("max_deletion_fraction")

  /** Format string for max_deletion_fraction */
  @Argument(fullName = "max_deletion_fractionFormat", shortName = "", doc = "Format string for max_deletion_fraction", required = false, exclusiveOf = "", validation = "")
  var max_deletion_fractionFormat: String = "%s"

  /** Minimum number of consensus indels required to trigger genotyping run */
  @Argument(fullName = "min_indel_count_for_genotyping", shortName = "minIndelCnt", doc = "Minimum number of consensus indels required to trigger genotyping run", required = false, exclusiveOf = "", validation = "")
  var min_indel_count_for_genotyping: Option[Int] = config("min_indel_count_for_genotyping")

  /** Minimum fraction of all reads at a locus that must contain an indel (of any allele) for that sample to contribute to the indel count for alleles */
  @Argument(fullName = "min_indel_fraction_per_sample", shortName = "minIndelFrac", doc = "Minimum fraction of all reads at a locus that must contain an indel (of any allele) for that sample to contribute to the indel count for alleles", required = false, exclusiveOf = "", validation = "")
  var min_indel_fraction_per_sample: Option[Double] = config("min_indel_fraction_per_sample")

  /** Format string for min_indel_fraction_per_sample */
  @Argument(fullName = "min_indel_fraction_per_sampleFormat", shortName = "", doc = "Format string for min_indel_fraction_per_sample", required = false, exclusiveOf = "", validation = "")
  var min_indel_fraction_per_sampleFormat: String = "%s"

  /** Indel gap continuation penalty, as Phred-scaled probability.  I.e., 30 => 10 -30/10 */
  @Argument(fullName = "indelGapContinuationPenalty", shortName = "indelGCP", doc = "Indel gap continuation penalty, as Phred-scaled probability.  I.e., 30 => 10^-30/10", required = false, exclusiveOf = "", validation = "")
  var indelGapContinuationPenalty: Option[String] = config("indelGapContinuationPenalty")

  /** Indel gap open penalty, as Phred-scaled probability.  I.e., 30 => 10 -30/10 */
  @Argument(fullName = "indelGapOpenPenalty", shortName = "indelGOP", doc = "Indel gap open penalty, as Phred-scaled probability.  I.e., 30 => 10^-30/10", required = false, exclusiveOf = "", validation = "")
  var indelGapOpenPenalty: Option[String] = config("indelGapOpenPenalty")

  /** Indel haplotype size */
  @Argument(fullName = "indelHaplotypeSize", shortName = "indelHSize", doc = "Indel haplotype size", required = false, exclusiveOf = "", validation = "")
  var indelHaplotypeSize: Option[Int] = config("indelHaplotypeSize")

  /** Output indel debug info */
  @Argument(fullName = "indelDebug", shortName = "indelDebug", doc = "Output indel debug info", required = false, exclusiveOf = "", validation = "")
  var indelDebug: Boolean = config("indelDebug", default = false)

  /** expt */
  @Argument(fullName = "ignoreSNPAlleles", shortName = "ignoreSNPAlleles", doc = "expt", required = false, exclusiveOf = "", validation = "")
  var ignoreSNPAlleles: Boolean = config("ignoreSNPAlleles", default = false)

  /** expt */
  @Argument(fullName = "allReadsSP", shortName = "dl", doc = "expt", required = false, exclusiveOf = "", validation = "")
  var allReadsSP: Boolean = config("allReadsSP", default = false)

  /** Ignore lane when building error model, error model is then per-site */
  @Argument(fullName = "ignoreLaneInfo", shortName = "ignoreLane", doc = "Ignore lane when building error model, error model is then per-site", required = false, exclusiveOf = "", validation = "")
  var ignoreLaneInfo: Boolean = config("ignoreLaneInfo", default = false)

  /** VCF file with the truth callset for the reference sample */
  @Input(fullName = "reference_sample_calls", shortName = "referenceCalls", doc = "VCF file with the truth callset for the reference sample", required = false, exclusiveOf = "", validation = "")
  var reference_sample_calls: Option[File] = config("reference_sample_calls")

  /** Reference sample name. */
  @Argument(fullName = "reference_sample_name", shortName = "refsample", doc = "Reference sample name.", required = false, exclusiveOf = "", validation = "")
  var reference_sample_name: Option[String] = config("reference_sample_name")

  /** Min quality score to consider. Smaller numbers process faster. Default: Q1. */
  @Argument(fullName = "min_quality_score", shortName = "minqs", doc = "Min quality score to consider. Smaller numbers process faster. Default: Q1.", required = false, exclusiveOf = "", validation = "")
  var min_quality_score: Option[String] = config("min_quality_score")

  /** Max quality score to consider. Smaller numbers process faster. Default: Q40. */
  @Argument(fullName = "max_quality_score", shortName = "maxqs", doc = "Max quality score to consider. Smaller numbers process faster. Default: Q40.", required = false, exclusiveOf = "", validation = "")
  var max_quality_score: Option[String] = config("max_quality_score")

  /** Phred-Scaled prior quality of the site. Default: Q20. */
  @Argument(fullName = "site_quality_prior", shortName = "site_prior", doc = "Phred-Scaled prior quality of the site. Default: Q20.", required = false, exclusiveOf = "", validation = "")
  var site_quality_prior: Option[String] = config("site_quality_prior")

  /** The minimum confidence in the error model to make a call. Number should be between 0 (no power requirement) and 1 (maximum power required). */
  @Argument(fullName = "min_power_threshold_for_calling", shortName = "min_call_power", doc = "The minimum confidence in the error model to make a call. Number should be between 0 (no power requirement) and 1 (maximum power required).", required = false, exclusiveOf = "", validation = "")
  var min_power_threshold_for_calling: Option[Double] = config("min_power_threshold_for_calling")

  /** Format string for min_power_threshold_for_calling */
  @Argument(fullName = "min_power_threshold_for_callingFormat", shortName = "", doc = "Format string for min_power_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var min_power_threshold_for_callingFormat: String = "%s"

  /** If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site */
  @Argument(fullName = "annotateNDA", shortName = "nda", doc = "If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site", required = false, exclusiveOf = "", validation = "")
  var annotateNDA: Boolean = config("annotateNDA", default = false)

  /** Heterozygosity value used to compute prior likelihoods for any locus */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false, exclusiveOf = "", validation = "")
  var heterozygosity: Option[Double] = config("heterozygosity")

  /** Format string for heterozygosity */
  @Argument(fullName = "heterozygosityFormat", shortName = "", doc = "Format string for heterozygosity", required = false, exclusiveOf = "", validation = "")
  var heterozygosityFormat: String = "%s"

  /** Heterozygosity for indel calling */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosity: Option[Double] = config("indel_heterozygosity")

  /** Format string for indel_heterozygosity */
  @Argument(fullName = "indel_heterozygosityFormat", shortName = "", doc = "Format string for indel_heterozygosity", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosityFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be called */
  @Argument(fullName = "standard_min_confidence_threshold_for_calling", shortName = "stand_call_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be called", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_calling: Option[Double] = config("stand_call_conf")

  /** Format string for standard_min_confidence_threshold_for_calling */
  @Argument(fullName = "standard_min_confidence_threshold_for_callingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_callingFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold) */
  @Argument(fullName = "standard_min_confidence_threshold_for_emitting", shortName = "stand_emit_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold)", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emitting: Option[Double] = config("stand_emit_conf")

  /** Format string for standard_min_confidence_threshold_for_emitting */
  @Argument(fullName = "standard_min_confidence_threshold_for_emittingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_emitting", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emittingFormat: String = "%s"

  /** Maximum number of alternate alleles to genotype */
  @Argument(fullName = "max_alternate_alleles", shortName = "maxAltAlleles", doc = "Maximum number of alternate alleles to genotype", required = false, exclusiveOf = "", validation = "")
  var max_alternate_alleles: Option[Int] = config("max_alternate_alleles")

  /** Input prior for calls */
  @Argument(fullName = "input_prior", shortName = "inputPrior", doc = "Input prior for calls", required = false, exclusiveOf = "", validation = "")
  var input_prior: List[Double] = config("input_prior", default = Nil)

  /** Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy). */
  @Argument(fullName = "sample_ploidy", shortName = "ploidy", doc = "Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy).", required = false, exclusiveOf = "", validation = "")
  var sample_ploidy: Option[Int] = config("sample_ploidy")

  /** Specifies how to determine the alternate alleles to use for genotyping */
  @Argument(fullName = "genotyping_mode", shortName = "gt_mode", doc = "Specifies how to determine the alternate alleles to use for genotyping", required = false, exclusiveOf = "", validation = "")
  var genotyping_mode: Option[String] = config("genotyping_mode")

  /** The set of alleles at which to genotype when --genotyping_mode is GENOTYPE_GIVEN_ALLELES */
  @Input(fullName = "alleles", shortName = "alleles", doc = "The set of alleles at which to genotype when --genotyping_mode is GENOTYPE_GIVEN_ALLELES", required = false, exclusiveOf = "", validation = "")
  var alleles: Option[File] = config("alleles")

  /** Fraction of contamination in sequencing data (for all samples) to aggressively remove */
  @Argument(fullName = "contamination_fraction_to_filter", shortName = "contamination", doc = "Fraction of contamination in sequencing data (for all samples) to aggressively remove", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_to_filter: Option[Double] = config("contamination_fraction_to_filter")

  /** Format string for contamination_fraction_to_filter */
  @Argument(fullName = "contamination_fraction_to_filterFormat", shortName = "", doc = "Format string for contamination_fraction_to_filter", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_to_filterFormat: String = "%s"

  /** Tab-separated File containing fraction of contamination in sequencing data (per sample) to aggressively remove. Format should be \"<SampleID><TAB><Contamination>\" (Contamination is double) per line; No header. */
  @Argument(fullName = "contamination_fraction_per_sample_file", shortName = "contaminationFile", doc = "Tab-separated File containing fraction of contamination in sequencing data (per sample) to aggressively remove. Format should be \"<SampleID><TAB><Contamination>\" (Contamination is double) per line; No header.", required = false, exclusiveOf = "", validation = "")
  var contamination_fraction_per_sample_file: Option[File] = config("contamination_fraction_per_sample_file")

  /** Non-reference probability calculation model to employ */
  @Argument(fullName = "p_nonref_model", shortName = "pnrm", doc = "Non-reference probability calculation model to employ", required = false, exclusiveOf = "", validation = "")
  var p_nonref_model: Option[String] = config("p_nonref_model")

  /** x */
  @Argument(fullName = "exactcallslog", shortName = "logExactCalls", doc = "x", required = false, exclusiveOf = "", validation = "")
  var exactcallslog: Option[File] = config("exactcallslog")

  /** Specifies which type of calls we should output */
  @Argument(fullName = "output_mode", shortName = "out_mode", doc = "Specifies which type of calls we should output", required = false, exclusiveOf = "", validation = "")
  var output_mode: Option[String] = config("output_mode")

  /** Annotate all sites with PLs */
  @Argument(fullName = "allSitePLs", shortName = "allSitePLs", doc = "Annotate all sites with PLs", required = false, exclusiveOf = "", validation = "")
  var allSitePLs: Boolean = config("allSitePLs", default = false)

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: Option[File] = dbsnpVcfFile

  /** Comparison VCF file */
  @Input(fullName = "comp", shortName = "comp", doc = "Comparison VCF file", required = false, exclusiveOf = "", validation = "")
  var comp: List[File] = config("comp", default = Nil)

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /** If provided, only these samples will be emitted into the VCF, regardless of which samples are present in the BAM file */
  @Argument(fullName = "onlyEmitSamples", shortName = "onlyEmitSamples", doc = "If provided, only these samples will be emitted into the VCF, regardless of which samples are present in the BAM file", required = false, exclusiveOf = "", validation = "")
  var onlyEmitSamples: List[String] = config("onlyEmitSamples", default = Nil)

  /** File to print all of the annotated and detailed debugging output */
  @Argument(fullName = "debug_file", shortName = "debug_file", doc = "File to print all of the annotated and detailed debugging output", required = false, exclusiveOf = "", validation = "")
  var debug_file: File = _

  /** File to print any relevant callability metrics output */
  @Argument(fullName = "metrics_file", shortName = "metrics", doc = "File to print any relevant callability metrics output", required = false, exclusiveOf = "", validation = "")
  var metrics_file: File = _

  /** One or more specific annotations to apply to variant calls */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var annotation: List[String] = config("annotation", default = Nil, freeVar = false)

  /** One or more specific annotations to exclude */
  @Argument(fullName = "excludeAnnotation", shortName = "XA", doc = "One or more specific annotations to exclude", required = false, exclusiveOf = "", validation = "")
  var excludeAnnotation: List[String] = config("excludeAnnotation", default = Nil)

  /** One or more classes/groups of annotations to apply to variant calls.  The single value 'none' removes the default group */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls.  The single value 'none' removes the default group", required = false, exclusiveOf = "", validation = "")
  var group: List[String] = config("group", default = Nil)

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
    reference_sample_calls.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    alleles.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    dbsnp.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    deps ++= comp.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
  }

  override def cmdLine: String = super.cmdLine +
    optional("-glm", genotype_likelihoods_model) +
    optional("-pcr_error", pcr_error_rate, format = pcr_error_rateFormat) +
    conditional(computeSLOD, "-slod") +
    optional("-pairHMM", pair_hmm_implementation) +
    optional("-mbq", min_base_quality_score) +
    optional("-deletions", max_deletion_fraction, format = max_deletion_fractionFormat) +
    optional("-minIndelCnt", min_indel_count_for_genotyping) +
    optional("-minIndelFrac", min_indel_fraction_per_sample, format = min_indel_fraction_per_sampleFormat) +
    optional("-indelGCP", indelGapContinuationPenalty) +
    optional("-indelGOP", indelGapOpenPenalty) +
    optional("-indelHSize", indelHaplotypeSize) +
    conditional(indelDebug, "-indelDebug") +
    conditional(ignoreSNPAlleles, "-ignoreSNPAlleles") +
    conditional(allReadsSP, "-dl") +
    conditional(ignoreLaneInfo, "-ignoreLane") +
    optional(TaggedFile.formatCommandLineParameter("-referenceCalls", reference_sample_calls.orNull), reference_sample_calls) +
    optional("-refsample", reference_sample_name) +
    optional("-minqs", min_quality_score) +
    optional("-maxqs", max_quality_score) +
    optional("-site_prior", site_quality_prior) +
    optional("-min_call_power", min_power_threshold_for_calling, format = min_power_threshold_for_callingFormat) +
    conditional(annotateNDA, "-nda") +
    optional("-hets", heterozygosity, format = heterozygosityFormat) +
    optional("-indelHeterozygosity", indel_heterozygosity, format = indel_heterozygosityFormat) +
    optional("-stand_call_conf", standard_min_confidence_threshold_for_calling, format = standard_min_confidence_threshold_for_callingFormat) +
    optional("-maxAltAlleles", max_alternate_alleles) +
    repeat("-inputPrior", input_prior) +
    optional("-ploidy", sample_ploidy) +
    optional("-gt_mode", genotyping_mode) +
    optional(TaggedFile.formatCommandLineParameter("-alleles", alleles.orNull), alleles) +
    optional("-contamination", contamination_fraction_to_filter, format = contamination_fraction_to_filterFormat) +
    optional("-contaminationFile", contamination_fraction_per_sample_file) +
    optional("-pnrm", p_nonref_model) +
    optional("-logExactCalls", exactcallslog) +
    optional("-out_mode", output_mode) +
    conditional(allSitePLs, "-allSitePLs") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp.orNull), dbsnp) +
    repeat("-comp", comp, formatPrefix = TaggedFile.formatCommandLineParameter) +
    optional("-o", out) +
    repeat("-onlyEmitSamples", onlyEmitSamples) +
    optional("-debug_file", debug_file) +
    optional("-metrics", metrics_file) +
    repeat("-A", annotation) +
    repeat("-XA", excludeAnnotation) +
    repeat("-G", group) +
    conditional(filter_reads_with_N_cigar, "-filterRNC") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ") +
    conditional(filter_bases_not_stored, "-filterNoBases") +
    (this.getVersion match {
      case Some(s) if s.contains("3.0") | s.contains("3.1") | s.contains("3.2") | s.contains("3.3") | s.contains("3.4") | s.contains("3.5") | s.contains("3.6") =>
        optional("-stand_emit_conf", standard_min_confidence_threshold_for_emitting, format = standard_min_confidence_threshold_for_emittingFormat)
      case _ => ""
    })
}

object UnifiedGenotyper {
  def apply(root: Configurable, inputFiles: List[File], outputFile: File): UnifiedGenotyper = {
    val ug = new UnifiedGenotyper(root)
    ug.input_file = inputFiles
    ug.out = outputFile
    ug
  }
}
