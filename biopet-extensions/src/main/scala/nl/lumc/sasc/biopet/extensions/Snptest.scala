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

import nl.lumc.sasc.biopet.core.{Version, Reference, BiopetCommandLineFunction}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 3/25/16.
  */
class Snptest(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {
  @Input(required = true)
  var inputGenotypes: List[File] = Nil

  @Input(required = true)
  var inputSampleFiles: List[File] = Nil

  @Output(required = false)
  var logFile: Option[File] = None

  @Output(required = false)
  var outputFile: Option[File] = None

  @Output(required = false)
  var outputDatabaseFile: Option[File] = None

  var assumeChromosome: Option[String] = config("assume_chromosome")
  var genotypeField: Option[String] = config("genotype_field")
  var genotypeProbabilityScale: Option[String] = config("genotype_probability_scale")
  var haploidGenotypeCoding: Option[String] = config("haploid_genotype_coding")
  var missingCode: Option[String] = config("missing_code")
  var totalProbLimit: Option[Double] = config("total_prob_limit")
  var tableName: Option[String] = config("table_name")
  var useLongColumnNamingScheme: Boolean = config("use_long_column_naming_scheme", default = false)

  var excludeSamples: List[String] = config("exclude_samples", default = Nil)

  @Input(required = false)
  val excludeSnp: Option[File] = config("exclude_snp")

  var missThresh: Option[Double] = config("miss_thresh")
  var baselinePhenotype: Option[String] = config("baseline_phenotype")
  var bayesian: List[String] = config("bayesian", default = Nil)
  var callThresh: Option[Double] = config("call_thresh")
  var frequentist: List[String] = config("frequentist", default = Nil)
  var fullParameterEstimates: Boolean = config("full_parameter_estimates", default = false)
  var method: Option[String] = config("method")
  var pheno: Option[String] = config("pheno")
  var summaryStatsOnly: Boolean = config("summary_stats_only", default = false)

  var covAll: Boolean = config("cov_all", default = false)
  var covAllContinuous: Boolean = config("cov_all_continuous", default = false)
  var covAllDiscrete: Boolean = config("cov_all_discrete", default = false)
  var covNames: List[String] = config("cov_names", default = Nil)
  var sexColumn: Option[String] = config("sex_column")
  var stratifyOn: Option[String] = config("stratify_on")

  var conditionOn: List[String] = config("condition_on", default = Nil)

  var priorAdd: List[String] = config("prior_add", default = Nil)
  var priorCov: List[String] = config("prior_cov", default = Nil)
  var priorDom: List[String] = config("prior_dom", default = Nil)
  var priorGen: List[String] = config("prior_gen", default = Nil)
  var priorHet: List[String] = config("prior_het", default = Nil)
  var priorRec: List[String] = config("prior_rec", default = Nil)
  var tDf: Option[String] = config("t_df")
  var tPrior: Boolean = config("t_prior", default = false)

  var priorMqtQ: Option[String] = config("prior_mqt_Q")
  var priorQtVb: Option[String] = config("prior_qt_V_b")
  var priorQtVq: Option[String] = config("prior_qt_V_q")
  var priorQtA: Option[String] = config("prior_qt_a")
  var priorQtB: Option[String] = config("prior_qt_b")
  var priorQtMeanB: Option[String] = config("prior_qt_mean_b")
  var priorQtMeanQ: Option[String] = config("prior_qt_mean_q")

  var meanBf: List[String] = config("mean_bf", default = Nil)

  var analysisDescription: Option[String] = config("analysis_description")
  var chunk: Option[Int] = config("chunk")
  var debug: Boolean = config("debug", default = false)
  var hwe: Boolean = config("hwe", default = false)
  var lowerSampleLimit: Option[Int] = config("lower_sample_limit")
  var overlap: Boolean = config("overlap", default = false)
  var printids: Boolean = config("printids", default = false)
  var quantileNormalisePhenotypes: Boolean =
    config("quantile_normalise_phenotypes", default = false)
  var range: List[String] = config("range", default = Nil)
  var renorm: Boolean = config("renorm", default = false)
  var snpid: List[String] = config("snpid", default = Nil)
  var useRawCovariates: Boolean = config("use_raw_covariates", default = false)
  var useRawPhenotypes: Boolean = config("use_raw_phenotypes", default = false)
  var noClobber: Boolean = config("no_clobber", default = false)

  executable = config("exe", default = "snptest")

  def versionCommand: String = executable + " -help"
  def versionRegex: List[Regex] = "Welcome to SNPTEST (.*)".r :: Nil

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(inputGenotypes.length == inputSampleFiles.length)
  }

  def cmdLine: String = {
    val data = inputGenotypes.zip(inputSampleFiles).flatMap(x => List(x._1, x._2))
    required(executable) +
      optional("-assume_chromosome", assumeChromosome) +
      multiArg("-data", data, groupSize = 2) +
      optional("-genotype_field", genotypeField) +
      optional("-genotype_probability_scale", genotypeProbabilityScale) +
      optional("-haploid_genotype_coding", haploidGenotypeCoding) +
      optional("-missing_code", missingCode) +
      optional("-total_prob_limit", totalProbLimit) +
      optional("-log", logFile) +
      optional("-o", outputFile) +
      optional("-odb", outputDatabaseFile) +
      optional("-table_name", tableName) +
      conditional(useLongColumnNamingScheme, "-use_long_column_naming_scheme") +
      multiArg("-exclude_samples", excludeSamples) +
      optional("-exclude_snp", excludeSnp) +
      optional("-miss_thresh", missThresh) +
      optional("-baseline_phenotype", baselinePhenotype) +
      multiArg("-bayesian", bayesian) +
      optional("-call_thresh", callThresh) +
      multiArg("-frequentist", frequentist) +
      conditional(fullParameterEstimates, "-full_parameter_estimates") +
      optional("-method", method) +
      optional("-pheno", pheno) +
      conditional(summaryStatsOnly, "-summary_stats_only") +
      conditional(covAll, "-cov_all") +
      conditional(covAllContinuous, "-cov_all_continuous") +
      conditional(covAllDiscrete, "-cov_all_discrete") +
      multiArg("-cov_names", covNames) +
      optional("-sex_column", sexColumn) +
      optional("-stratify_on", stratifyOn) +
      multiArg("-condition_on", conditionOn) +
      multiArg("-prior_add", priorAdd, groupSize = 4, maxGroups = 1) +
      multiArg("-prior_cov", priorCov, groupSize = 2, maxGroups = 1) +
      multiArg("-prior_dom", priorDom, groupSize = 4, maxGroups = 1) +
      multiArg("-prior_gen", priorGen, groupSize = 6, maxGroups = 1) +
      multiArg("-prior_het", priorHet, groupSize = 4, maxGroups = 1) +
      multiArg("-prior_rec", priorRec, groupSize = 4, maxGroups = 1) +
      optional("-t_df", tDf) +
      conditional(tPrior, "-t_prior") +
      optional("-prior_mqt_Q", priorMqtQ) +
      optional("-prior_qt_V_b", priorQtVb) +
      optional("-prior_qt_V_q", priorQtVq) +
      optional("-prior_qt_a", priorQtA) +
      optional("-prior_qt_b", priorQtB) +
      optional("-prior_qt_mean_b", priorQtMeanB) +
      optional("-prior_qt_mean_q", priorQtMeanQ) +
      multiArg("-mean_bf", meanBf, groupSize = 2, maxGroups = 1) +
      optional("-analysis_description", analysisDescription) +
      optional("-analysis_name", analysisName) +
      optional("-chunk", chunk) +
      conditional(debug, "-debug") +
      conditional(hwe, "-hwe") +
      optional("-lower_sample_limit", lowerSampleLimit) +
      conditional(overlap, "-overlap") +
      conditional(printids, "-printids") +
      conditional(quantileNormalisePhenotypes, "quantile_normalise_phenotypes") +
      multiArg("-range", range, groupSize = 2) +
      conditional(renorm, "-renorm") +
      multiArg("-snpid", snpid) +
      conditional(useRawCovariates, "-use_raw_covariates") +
      conditional(useRawPhenotypes, "-use_raw_phenotypes") +
      conditional(noClobber, "-no_clobber")
  }
}
