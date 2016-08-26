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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction, Reference }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

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
  var bamList: Option[File] = config("bam_list")

  @Input(required = false)
  var targets: Option[File] = config("targets", freeVar = false)

  @Input(required = false)
  var samples: Option[File] = config("samples", freeVar = false)

  @Input(required = false)
  var populations: Option[File] = config("populations", freeVar = false)

  @Input(required = false)
  var cnvMap: Option[File] = config("cnv_map", freeVar = false)

  @Input(required = false)
  var trace: Option[File] = config("trace", freeVar = false)

  @Input(required = false)
  var failedAlleles: Option[File] = config("failed_alleles", freeVar = false)

  @Input(required = false)
  var observationBias: Option[File] = config("observation_bias")

  @Input(required = false)
  var contaminationEstimates: Option[File] = config("contamination_estimates")

  @Input(required = false)
  var variantInput: Option[File] = config("variant_input", freeVar = false)

  @Input(required = false)
  var haplotypeBasisAlleles: Option[File] = config("haplotype_basis_alleles", freeVar = false)

  var pvar: Option[Int] = config("pvar", freeVar = false)
  var theta: Option[Int] = config("theta", freeVar = false)
  var ploidy: Option[Int] = config("ploidy", freeVar = false)
  var useBestNAlleles: Option[Int] = config("use_best_n_alleles")
  var maxComplexGap: Option[Int] = config("max_complex_gap")
  var minRepeatSize: Option[Int] = config("min_repeat_size")
  var minRepeatEntropy: Option[Int] = config("min_repeat_entropy")
  var readMismatchLimit: Option[Int] = config("read_mismatch_limit")
  var readMaxMismatchFraction: Option[Int] = config("read_max_mismatch_fraction")
  var readSnpLimit: Option[Int] = config("read_snp_limit")
  var readIndelLimit: Option[Int] = config("read_indel_limit")
  var minAlternateFraction: Option[Double] = config("min_alternate_fraction")
  var minAlternateCount: Option[Int] = config("min_alternate_count")
  var minAlternateQsum: Option[Int] = config("min_alternate_qsum")
  var minAlternateTotal: Option[Int] = config("min_alternate_total")
  var minCoverage: Option[Int] = config("min_coverage")
  var genotypingMaxIterations: Option[Int] = config("genotyping_max_iterations")
  var genotypingMaxBanddepth: Option[Int] = config("genotyping_max_banddepth")
  var genotypeVariantThreshold: Option[Int] = config("genotype_variant_threshold")
  var readDependenceFactor: Option[Int] = config("read_dependence_factor")
  var minMappingQuality: Option[Double] = config("min_mapping_quality")
  var minBaseQuality: Option[Double] = config("min_base_quality")
  var minSupportingAlleleQsum: Option[Double] = config("min_supporting_allele_qsum")
  var minSupportingMappingQsum: Option[Double] = config("min_supporting_mapping_qsum")
  var mismatchBaseQualityThreshold: Option[Double] = config("mismatch_base_quality_threshold")
  var baseQualityCap: Option[Double] = config("base_quality_cap")
  var probContamination: Option[Double] = config("prob_contamination")
  var onlyUseInputAlleles: Boolean = config("only_use_input_alleles", default = false)
  var reportAllHaplotypeAlleles: Boolean = config("report_all_haplotype_alleles", default = false)
  var reportMonomorphic: Boolean = config("report_monomorphic", default = false)
  var pooledDiscrete: Boolean = config("pooled_discrete", default = false)
  var pooledContinuous: Boolean = config("pooled_continuous", default = false)
  var useReferenceAllele: Boolean = config("use_reference_allele", default = false)
  var noSnps: Boolean = config("no_snps", default = false)
  var noIndels: Boolean = config("no_indels", default = false)
  var noMnps: Boolean = config("no_mnps", default = false)
  var noComplex: Boolean = config("no_complex", default = false)
  var noPartialObservations: Boolean = config("no_partial_observations", default = false)
  var dontLeftAlignIndels: Boolean = config("dont_left_align_indels", default = false)
  var useDuplicateReads: Boolean = config("use_duplicate_reads", default = false)
  var standardFilters: Boolean = config("standard_filters", default = false)
  var noPopulationPriors: Boolean = config("no_population_priors", default = false)
  var hwePriorsOff: Boolean = config("hwe_priors_off", default = false)
  var binomialObsPriorsOff: Boolean = config("binomial_obs_priors_off", default = false)
  var alleleBalancePriorsOff: Boolean = config("allele_balance_priors_off", default = false)
  var legacyGls: Boolean = config("legacy_gls", default = false)
  var reportGenotypeLikelihoodMax: Boolean = config("report_genotype_likelihood_max", default = false)
  var excludeUnobservedGenotypes: Boolean = config("exclude_unobserved_genotypes", default = false)
  var useMappingQuality: Boolean = config("use_mapping_quality", default = false)
  var harmonicIndelQuality: Boolean = config("harmonic_indel_quality", default = false)
  var genotypeQualities: Boolean = config("genotype_qualities", default = false)
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
    optional("--bam-list", bamList) +
    optional("--targets", targets) +
    optional("--samples", samples) +
    optional("--populations", populations) +
    optional("--cnv-map", cnvMap) +
    optional("--trace", trace) +
    optional("--failed-alleles", failedAlleles) +
    optional("--observation-bias", observationBias) +
    optional("--contamination-estimates", contaminationEstimates) +
    optional("--variant-input", variantInput) +
    optional("--haplotype-basis-alleles", haplotypeBasisAlleles) +
    optional("--pvar", pvar) +
    optional("--theta", theta) +
    optional("--ploidy", ploidy) +
    optional("--use-best-n-alleles", useBestNAlleles) +
    optional("--max-complex-gap", maxComplexGap) +
    optional("--min-repeat-size", minRepeatSize) +
    optional("--min-repeat-entropy", minRepeatEntropy) +
    optional("--read-mismatch-limit", readMismatchLimit) +
    optional("--read-max-mismatch-fraction", readMaxMismatchFraction) +
    optional("--read-snp-limit", readSnpLimit) +
    optional("--read-indel-limit", readIndelLimit) +
    optional("--min-alternate-fraction", minAlternateFraction) +
    optional("--min-alternate-count", minAlternateCount) +
    optional("--min-alternate-qsum", minAlternateQsum) +
    optional("--min-alternate-total", minAlternateTotal) +
    optional("--min-coverage", minCoverage) +
    optional("--genotyping-max-iterations", genotypingMaxIterations) +
    optional("--genotyping-max-banddepth", genotypingMaxBanddepth) +
    optional("--genotype-variant-threshold", genotypeVariantThreshold) +
    optional("--read-dependence-factor", readDependenceFactor) +
    optional("--min-mapping-quality", minMappingQuality) +
    optional("--min-base-quality", minBaseQuality) +
    optional("--min-supporting-allele-qsum", minSupportingAlleleQsum) +
    optional("--min-supporting-mapping-qsum", minSupportingMappingQsum) +
    optional("--mismatch-base-quality-threshold", mismatchBaseQualityThreshold) +
    optional("--base-quality-cap", baseQualityCap) +
    optional("--prob-contamination", probContamination) +
    conditional(onlyUseInputAlleles, "--only-use-input-alleles") +
    conditional(reportAllHaplotypeAlleles, "--report-all-haplotype-alleles") +
    conditional(reportMonomorphic, "--report-monomorphic") +
    conditional(pooledDiscrete, "--pooled-discrete") +
    conditional(pooledContinuous, "--pooled-continuous") +
    conditional(useReferenceAllele, "--use-reference-allele") +
    conditional(noSnps, "--no-snps") +
    conditional(noIndels, "--no-indels") +
    conditional(noMnps, "--no-mnps") +
    conditional(noComplex, "--no-complex") +
    conditional(noPartialObservations, "--no-partial-observations") +
    conditional(dontLeftAlignIndels, "--dont-left-align-indels") +
    conditional(useDuplicateReads, "--use-duplicate-reads") +
    conditional(standardFilters, "--standard-filters") +
    conditional(noPopulationPriors, "--no-population-priors") +
    conditional(hwePriorsOff, "--hwe-priors-off") +
    conditional(binomialObsPriorsOff, "--binomial-obs-priors-off") +
    conditional(alleleBalancePriorsOff, "--allele-balance-priors-off") +
    conditional(legacyGls, "--legacy-gls") +
    conditional(reportGenotypeLikelihoodMax, "--report-genotype-likelihood-max") +
    conditional(excludeUnobservedGenotypes, "--exclude-unobserved-genotypes") +
    conditional(useMappingQuality, "--use-mapping-quality") +
    conditional(harmonicIndelQuality, "--harmonic-indel-quality") +
    conditional(genotypeQualities, "--genotype-qualities") +
    conditional(debug, "--debug") +
    optional("--haplotype-length", haplotypeLength) +
    (if (inputAsStdin) required("--stdin") else "") +
    (if (outputAsStsout) "" else optional("--vcf", outputVcf))
}
