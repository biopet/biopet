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

import nl.lumc.sasc.biopet.extensions.gatk.gather.GatherVcfs
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Gather, Input, Output, _}

class VariantRecalibrator(val parent: Configurable) extends CommandLineGATK {
  def analysis_type = "VariantRecalibrator"

  /** Recalibration mode to employ */
  @Argument(fullName = "mode", shortName = "mode", doc = "Recalibration mode to employ", required = true, exclusiveOf = "", validation = "")
  var mode: String = _

  /** Max number of Gaussians for the positive model */
  @Argument(fullName = "maxGaussians", shortName = "mG", doc = "Max number of Gaussians for the positive model", required = false, exclusiveOf = "", validation = "")
  var maxGaussians: Option[Int] = config("maxGaussians")

  /** Max number of Gaussians for the negative model */
  @Argument(fullName = "maxNegativeGaussians", shortName = "mNG", doc = "Max number of Gaussians for the negative model", required = false, exclusiveOf = "", validation = "")
  var maxNegativeGaussians: Option[Int] = config("maxNegativeGaussians")

  /** Maximum number of VBEM iterations */
  @Argument(fullName = "maxIterations", shortName = "mI", doc = "Maximum number of VBEM iterations", required = false, exclusiveOf = "", validation = "")
  var maxIterations: Option[Int] = config("maxIterations")

  /** Number of k-means iterations */
  @Argument(fullName = "numKMeans", shortName = "nKM", doc = "Number of k-means iterations", required = false, exclusiveOf = "", validation = "")
  var numKMeans: Option[Int] = config("numKMeans")

  /** Annotation value divergence threshold (number of standard deviations from the means)  */
  @Argument(fullName = "stdThreshold", shortName = "std", doc = "Annotation value divergence threshold (number of standard deviations from the means) ", required = false, exclusiveOf = "", validation = "")
  var stdThreshold: Option[Double] = config("stdThreshold")

  /** Format string for stdThreshold */
  @Argument(fullName = "stdThresholdFormat", shortName = "", doc = "Format string for stdThreshold", required = false, exclusiveOf = "", validation = "")
  var stdThresholdFormat: String = "%s"

  /** The shrinkage parameter in the variational Bayes algorithm. */
  @Argument(fullName = "shrinkage", shortName = "shrinkage", doc = "The shrinkage parameter in the variational Bayes algorithm.", required = false, exclusiveOf = "", validation = "")
  var shrinkage: Option[Double] = config("shrinkage")

  /** Format string for shrinkage */
  @Argument(fullName = "shrinkageFormat", shortName = "", doc = "Format string for shrinkage", required = false, exclusiveOf = "", validation = "")
  var shrinkageFormat: String = "%s"

  /** The dirichlet parameter in the variational Bayes algorithm. */
  @Argument(fullName = "dirichlet", shortName = "dirichlet", doc = "The dirichlet parameter in the variational Bayes algorithm.", required = false, exclusiveOf = "", validation = "")
  var dirichlet: Option[Double] = config("dirichlet")

  /** Format string for dirichlet */
  @Argument(fullName = "dirichletFormat", shortName = "", doc = "Format string for dirichlet", required = false, exclusiveOf = "", validation = "")
  var dirichletFormat: String = "%s"

  /** The number of prior counts to use in the variational Bayes algorithm. */
  @Argument(fullName = "priorCounts", shortName = "priorCounts", doc = "The number of prior counts to use in the variational Bayes algorithm.", required = false, exclusiveOf = "", validation = "")
  var priorCounts: Option[Double] = config("priorCounts")

  /** Format string for priorCounts */
  @Argument(fullName = "priorCountsFormat", shortName = "", doc = "Format string for priorCounts", required = false, exclusiveOf = "", validation = "")
  var priorCountsFormat: String = "%s"

  /** Maximum number of training data */
  @Argument(fullName = "maxNumTrainingData", shortName = "maxNumTrainingData", doc = "Maximum number of training data", required = false, exclusiveOf = "", validation = "")
  var maxNumTrainingData: Option[Int] = config("maxNumTrainingData")

  /** Minimum number of bad variants */
  @Argument(fullName = "minNumBadVariants", shortName = "minNumBad", doc = "Minimum number of bad variants", required = false, exclusiveOf = "", validation = "")
  var minNumBadVariants: Option[Int] = config("minNumBadVariants")

  /** LOD score cutoff for selecting bad variants */
  @Argument(fullName = "badLodCutoff", shortName = "badLodCutoff", doc = "LOD score cutoff for selecting bad variants", required = false, exclusiveOf = "", validation = "")
  var badLodCutoff: Option[Double] = config("badLodCutoff")

  /** Format string for badLodCutoff */
  @Argument(fullName = "badLodCutoffFormat", shortName = "", doc = "Format string for badLodCutoff", required = false, exclusiveOf = "", validation = "")
  var badLodCutoffFormat: String = "%s"

  /** Apply logit transform and jitter to MQ values */
  @Argument(fullName = "MQCapForLogitJitterTransform", shortName = "MQCap", doc = "Apply logit transform and jitter to MQ values", required = false, exclusiveOf = "", validation = "")
  var MQCapForLogitJitterTransform: Option[Int] = config("MQCapForLogitJitterTransform")

  /** MQ is by default transformed to log[(MQ_cap + epsilon - MQ)/(MQ + epsilon)] to make it more Gaussian-like.  Use this flag to not do that. */
  @Argument(fullName = "no_MQ_logit", shortName = "NoMQLogit", doc = "MQ is by default transformed to log[(MQ_cap + epsilon - MQ)/(MQ + epsilon)] to make it more Gaussian-like.  Use this flag to not do that.", required = false, exclusiveOf = "", validation = "")
  var no_MQ_logit: Boolean = config("no_MQ_logit", default = false)

  /** Amount of jitter (as a factor to a Normal(0,1) noise) to add to the MQ capped values */
  @Argument(fullName = "MQ_jitter", shortName = "MQJitt", doc = "Amount of jitter (as a factor to a Normal(0,1) noise) to add to the MQ capped values", required = false, exclusiveOf = "", validation = "")
  var MQ_jitter: Option[Double] = config("MQ_jitter", default = false)

  /** Format string for MQ_jitter */
  @Argument(fullName = "MQ_jitterFormat", shortName = "", doc = "Format string for MQ_jitter", required = false, exclusiveOf = "", validation = "")
  var MQ_jitterFormat: String = "%s"

  /** The raw input variants to be recalibrated */
  @Input(fullName = "input", shortName = "input", doc = "The raw input variants to be recalibrated", required = true, exclusiveOf = "", validation = "")
  var input: Seq[File] = Nil

  /** Additional raw input variants to be used in building the model */
  @Input(fullName = "aggregate", shortName = "aggregate", doc = "Additional raw input variants to be used in building the model", required = false, exclusiveOf = "", validation = "")
  var aggregate: List[File] = config("aggregate", default = Nil)

  /** A list of sites for which to apply a prior probability of being correct but which aren't used by the algorithm (training and truth sets are required to run) */
  @Input(fullName = "resource", shortName = "resource", doc = "A list of sites for which to apply a prior probability of being correct but which aren't used by the algorithm (training and truth sets are required to run)", required = true, exclusiveOf = "", validation = "")
  var resource: List[File] = config("resource", default = Nil)

  /** The output recal file used by ApplyRecalibration */
  @Output(fullName = "recal_file", shortName = "recalFile", doc = "The output recal file used by ApplyRecalibration", required = true, exclusiveOf = "", validation = "")
  @Gather(classOf[GatherVcfs])
  var recal_file: File = _

  /** The output tranches file used by ApplyRecalibration */
  @Output(fullName = "tranches_file", shortName = "tranchesFile", doc = "The output tranches file used by ApplyRecalibration", required = true, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var tranches_file: File = _

  /** The expected novel Ti/Tv ratio to use when calculating FDR tranches and for display on the optimization curve output figures. (approx 2.15 for whole genome experiments). ONLY USED FOR PLOTTING PURPOSES! */
  @Argument(fullName = "target_titv", shortName = "titv", doc = "The expected novel Ti/Tv ratio to use when calculating FDR tranches and for display on the optimization curve output figures. (approx 2.15 for whole genome experiments). ONLY USED FOR PLOTTING PURPOSES!", required = false, exclusiveOf = "", validation = "")
  var target_titv: Option[Double] = config("target_titv")

  /** Format string for target_titv */
  @Argument(fullName = "target_titvFormat", shortName = "", doc = "Format string for target_titv", required = false, exclusiveOf = "", validation = "")
  var target_titvFormat: String = "%s"

  /** The names of the annotations which should used for calculations */
  @Argument(fullName = "use_annotation", shortName = "an", doc = "The names of the annotations which should used for calculations", required = true, exclusiveOf = "", validation = "")
  var use_annotation: List[String] = config("use_annotation", default = Nil)

  /** The levels of truth sensitivity at which to slice the data. (in percent, that is 1.0 for 1 percent) */
  @Argument(fullName = "TStranche", shortName = "tranche", doc = "The levels of truth sensitivity at which to slice the data. (in percent, that is 1.0 for 1 percent)", required = false, exclusiveOf = "", validation = "")
  var TStranche: List[Double] = config("TStranche", default = Nil)

  /** If specified, the variant recalibrator will also use variants marked as filtered by the specified filter name in the input VCF file */
  @Argument(fullName = "ignore_filter", shortName = "ignoreFilter", doc = "If specified, the variant recalibrator will also use variants marked as filtered by the specified filter name in the input VCF file", required = false, exclusiveOf = "", validation = "")
  var ignore_filter: List[String] = config("ignore_filter", default = Nil)

  /** If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file. */
  @Argument(fullName = "ignore_all_filters", shortName = "ignoreAllFilters", doc = "If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file.", required = false, exclusiveOf = "", validation = "")
  var ignore_all_filters: Boolean = _

  /** The output rscript file generated by the VQSR to aid in visualization of the input data and learned model */
  @Output(fullName = "rscript_file", shortName = "rscriptFile", doc = "The output rscript file generated by the VQSR to aid in visualization of the input data and learned model", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var rscript_file: File = _

  /** Used to debug the random number generation inside the VQSR. Do not use. */
  @Argument(fullName = "replicate", shortName = "replicate", doc = "Used to debug the random number generation inside the VQSR. Do not use.", required = false, exclusiveOf = "", validation = "")
  var replicate: Option[Int] = config("replicate")

  /** Trust that all the input training sets' unfiltered records contain only polymorphic sites to drastically speed up the computation. */
  @Argument(fullName = "trustAllPolymorphic", shortName = "allPoly", doc = "Trust that all the input training sets' unfiltered records contain only polymorphic sites to drastically speed up the computation.", required = false, exclusiveOf = "", validation = "")
  var trustAllPolymorphic: Boolean = config("trustAllPolymorphic", default = false)

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
    deps ++= aggregate.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    deps ++= resource.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (recal_file != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(recal_file))
      outputIndex = VcfUtils.getVcfIndexFile(recal_file)
  }

  override def cmdLine = super.cmdLine +
    required("-mode", mode, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mG", maxGaussians, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mNG", maxNegativeGaussians, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mI", maxIterations, spaceSeparated = true, escape = true, format = "%s") +
    optional("-nKM", numKMeans, spaceSeparated = true, escape = true, format = "%s") +
    optional("-std", stdThreshold, spaceSeparated = true, escape = true, format = stdThresholdFormat) +
    optional("-shrinkage", shrinkage, spaceSeparated = true, escape = true, format = shrinkageFormat) +
    optional("-dirichlet", dirichlet, spaceSeparated = true, escape = true, format = dirichletFormat) +
    optional("-priorCounts", priorCounts, spaceSeparated = true, escape = true, format = priorCountsFormat) +
    optional("-maxNumTrainingData", maxNumTrainingData, spaceSeparated = true, escape = true, format = "%s") +
    optional("-minNumBad", minNumBadVariants, spaceSeparated = true, escape = true, format = "%s") +
    optional("-badLodCutoff", badLodCutoff, spaceSeparated = true, escape = true, format = badLodCutoffFormat) +
    optional("-MQCap", MQCapForLogitJitterTransform, spaceSeparated = true, escape = true, format = "%s") +
    conditional(no_MQ_logit, "-NoMQLogit", escape = true, format = "%s") +
    optional("-MQJitt", MQ_jitter, spaceSeparated = true, escape = true, format = MQ_jitterFormat) +
    repeat("-input", input, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-aggregate", aggregate, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-resource", resource, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    required("-recalFile", recal_file, spaceSeparated = true, escape = true, format = "%s") +
    required("-tranchesFile", tranches_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-titv", target_titv, spaceSeparated = true, escape = true, format = target_titvFormat) +
    repeat("-an", use_annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-tranche", TStranche, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-ignoreFilter", ignore_filter, spaceSeparated = true, escape = true, format = "%s") +
    conditional(ignore_all_filters, "-ignoreAllFilters", escape = true, format = "%s") +
    optional("-rscriptFile", rscript_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-replicate", replicate, spaceSeparated = true, escape = true, format = "%s") +
    conditional(trustAllPolymorphic, "-allPoly", escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
