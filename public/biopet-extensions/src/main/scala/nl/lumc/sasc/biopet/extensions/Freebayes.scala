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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction, Reference }
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
 * Extension for freebayes
 *
 * Created by pjvan_thof on 3/3/15.
 */
class Freebayes(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {

  @Input(required = true)
  var bamfiles: List[File] = Nil

  @Input(required = true)
  var reference: File = _

  @Output(required = true)
  var outputVcf: File = null

  @Input(required = false)
  var bam_list: Option[File] = config("bam_list")

  @Input(required = false)
  var targets: Option[File] = config("targets")

  @Input(required = false)
  var samples: Option[File] = config("samples")

  @Input(required = false)
  var populations: Option[File] = config("populations")

  @Input(required = false)
  var cnv_map: Option[File] = config("cnv_map")

  @Input(required = false)
  var trace: Option[File] = config("trace")

  @Input(required = false)
  var failed_alleles: Option[File] = config("failed_alleles")

  @Input(required = false)
  var observation_bias: Option[File] = config("observation_bias")

  @Input(required = false)
  var contamination_estimates: Option[File] = config("contamination_estimates")

  @Input(required = false)
  var variant_input: Option[File] = config("variant_input")

  @Input(required = false)
  var haplotype_basis_alleles: Option[File] = config("haplotype_basis_alleles")

  var pvar: Option[Int] = config("pvar")
  var theta: Option[Int] = config("theta")
  var ploidy: Option[Int] = config("ploidy")
  var use_best_n_alleles: Option[Int] = config("use_best_n_alleles")
  var max_complex_gap: Option[Int] = config("max_complex_gap")
  var min_repeat_size: Option[Int] = config("min_repeat_size")
  var min_repeat_entropy: Option[Int] = config("min_repeat_entropy")
  var read_mismatch_limit: Option[Int] = config("read_mismatch_limit")
  var read_max_mismatch_fraction: Option[Int] = config("read_max_mismatch_fraction")
  var read_snp_limit: Option[Int] = config("read_snp_limit")
  var read_indel_limit: Option[Int] = config("read_indel_limit")
  var min_alternate_fraction: Option[Int] = config("min_alternate_fraction")
  var min_alternate_count: Option[Int] = config("min_alternate_count")
  var min_alternate_qsum: Option[Int] = config("min_alternate_qsum")
  var min_alternate_total: Option[Int] = config("min_alternate_total")
  var min_coverage: Option[Int] = config("min_coverage")
  var genotyping_max_iterations: Option[Int] = config("genotyping_max_iterations")
  var genotyping_max_banddepth: Option[Int] = config("genotyping_max_banddepth")
  var genotype_variant_threshold: Option[Int] = config("genotype_variant_threshold")
  var read_dependence_factor: Option[Int] = config("read_dependence_factor")
  var min_mapping_quality: Option[Double] = config("min_mapping_quality")
  var min_base_quality: Option[Double] = config("min_base_quality")
  var min_supporting_allele_qsum: Option[Double] = config("min_supporting_allele_qsum")
  var min_supporting_mapping_qsum: Option[Double] = config("min_supporting_mapping_qsum")
  var mismatch_base_quality_threshold: Option[Double] = config("mismatch_base_quality_threshold")
  var base_quality_cap: Option[Double] = config("base_quality_cap")
  var prob_contamination: Option[Double] = config("prob_contamination")
  var stdin: Boolean = config("stdin", default = false)
  var only_use_input_alleles: Boolean = config("only_use_input_alleles", default = false)
  var report_all_haplotype_alleles: Boolean = config("report_all_haplotype_alleles", default = false)
  var report_monomorphic: Boolean = config("report_monomorphic", default = false)
  var pooled_discrete: Boolean = config("pooled_discrete", default = false)
  var pooled_continuous: Boolean = config("pooled_continuous", default = false)
  var use_reference_allele: Boolean = config("use_reference_allele", default = false)
  var no_snps: Boolean = config("no_snps", default = false)
  var no_indels: Boolean = config("no_indels", default = false)
  var no_mnps: Boolean = config("no_mnps", default = false)
  var no_complex: Boolean = config("no_complex", default = false)
  var no_partial_observations: Boolean = config("no_partial_observations", default = false)
  var dont_left_align_indels: Boolean = config("dont_left_align_indels", default = false)
  var use_duplicate_reads: Boolean = config("use_duplicate_reads", default = false)
  var standard_filters: Boolean = config("standard_filters", default = false)
  var no_population_priors: Boolean = config("no_population_priors", default = false)
  var hwe_priors_off: Boolean = config("hwe_priors_off", default = false)
  var binomial_obs_priors_off: Boolean = config("binomial_obs_priors_off", default = false)
  var allele_balance_priors_off: Boolean = config("allele_balance_priors_off", default = false)
  var legacy_gls: Boolean = config("legacy_gls", default = false)
  var report_genotype_likelihood_max: Boolean = config("report_genotype_likelihood_max", default = false)
  var exclude_unobserved_genotypes: Boolean = config("exclude_unobserved_genotypes", default = false)
  var use_mapping_quality: Boolean = config("use_mapping_quality", default = false)
  var harmonic_indel_quality: Boolean = config("harmonic_indel_quality", default = false)
  var genotype_qualities: Boolean = config("genotype_qualities", default = false)
  var debug: Boolean = config("debug", default = logger.isDebugEnabled)

  var haplotypeLength: Option[Int] = config("haplotype_length")

  executable = config("exe", default = "freebayes")
  def versionRegex = """version:  (.*)""".r
  def versionCommand = executable + " --version"

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    reference = referenceFasta()
  }

  def cmdLine = executable +
    required("--fasta-reference", reference) +
    repeat("--bam", bamfiles) +
    optional("--bam-list", bam_list) +
    optional("--targets", targets) +
    optional("--samples", samples) +
    optional("--populations", populations) +
    optional("--cnv-map", cnv_map) +
    optional("--trace", trace) +
    optional("--failed-alleles", failed_alleles) +
    optional("--observation-bias", observation_bias) +
    optional("--contamination-estimates", contamination_estimates) +
    optional("--variant-input", variant_input) +
    optional("--haplotype-basis-alleles", haplotype_basis_alleles) +
    optional("--vcf", outputVcf) +
    optional("--pvar", pvar) +
    optional("--theta", theta) +
    optional("--ploidy", ploidy) +
    optional("--use-best-n-alleles", use_best_n_alleles) +
    optional("--max-complex-gap", max_complex_gap) +
    optional("--min-repeat-size", min_repeat_size) +
    optional("--min-repeat-entropy", min_repeat_entropy) +
    optional("--read-mismatch-limit", read_mismatch_limit) +
    optional("--read-max-mismatch-fraction", read_max_mismatch_fraction) +
    optional("--read-snp-limit", read_snp_limit) +
    optional("--read-indel-limit", read_indel_limit) +
    optional("--min-alternate-fraction", min_alternate_fraction) +
    optional("--min-alternate-count", min_alternate_count) +
    optional("--min-alternate-qsum", min_alternate_qsum) +
    optional("--min-alternate-total", min_alternate_total) +
    optional("--min-coverage", min_coverage) +
    optional("--genotyping-max-iterations", genotyping_max_iterations) +
    optional("--genotyping-max-banddepth", genotyping_max_banddepth) +
    optional("--genotype-variant-threshold", genotype_variant_threshold) +
    optional("--read-dependence-factor", read_dependence_factor) +
    optional("--min-mapping-quality", min_mapping_quality) +
    optional("--min-base-quality", min_base_quality) +
    optional("--min-supporting-allele-qsum", min_supporting_allele_qsum) +
    optional("--min-supporting-mapping-qsum", min_supporting_mapping_qsum) +
    optional("--mismatch-base-quality-threshold", mismatch_base_quality_threshold) +
    optional("--base-quality-cap", base_quality_cap) +
    optional("--prob-contamination", prob_contamination) +
    conditional(only_use_input_alleles, "--only-use-input-alleles") +
    conditional(report_all_haplotype_alleles, "--report-all-haplotype-alleles") +
    conditional(report_monomorphic, "--report-monomorphic") +
    conditional(pooled_discrete, "--pooled-discrete") +
    conditional(pooled_continuous, "--pooled-continuous") +
    conditional(use_reference_allele, "--use-reference-allele") +
    conditional(no_snps, "--no-snps") +
    conditional(no_indels, "--no-indels") +
    conditional(no_mnps, "--no-mnps") +
    conditional(no_complex, "--no-complex") +
    conditional(no_partial_observations, "--no-partial-observations") +
    conditional(dont_left_align_indels, "--dont-left-align-indels") +
    conditional(use_duplicate_reads, "--use-duplicate-reads") +
    conditional(standard_filters, "--standard-filters") +
    conditional(no_population_priors, "--no-population-priors") +
    conditional(hwe_priors_off, "--hwe-priors-off") +
    conditional(binomial_obs_priors_off, "--binomial-obs-priors-off") +
    conditional(allele_balance_priors_off, "--allele-balance-priors-off") +
    conditional(legacy_gls, "--legacy-gls") +
    conditional(report_genotype_likelihood_max, "--report-genotype-likelihood-max") +
    conditional(exclude_unobserved_genotypes, "--exclude-unobserved-genotypes") +
    conditional(use_mapping_quality, "--use-mapping-quality") +
    conditional(harmonic_indel_quality, "--harmonic-indel-quality") +
    conditional(genotype_qualities, "--genotype-qualities") +
    conditional(debug, "--debug") +
    optional("--haplotype-length", haplotypeLength)
}
