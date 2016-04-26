/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

//import java.io.File
//
//import nl.lumc.sasc.biopet.utils.config.Configurable
//import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
//
//class VariantRecalibrator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantRecalibrator with GatkGeneral {
//  override val defaultThreads = 4
//
//  nt = Option(getThreads)
//  memoryLimit = Option(nt.getOrElse(1) * 2)
//
//  if (config.contains("dbsnp")) resource :+= new TaggedFile(config("dbsnp").asString, "known=true,training=false,truth=false,prior=2.0")
//
//  an = config("annotation", default = List("QD", "DP", "FS", "ReadPosRankSum", "MQRankSum")).asStringList
//  minNumBadVariants = config("minnumbadvariants")
//  maxGaussians = config("maxgaussians")
//}
//
//object VariantRecalibrator {
//  def apply(root: Configurable, input: File, recal_file: File, tranches_file: File, indel: Boolean = false): VariantRecalibrator = {
//    val vr = new VariantRecalibrator(root) {
//      override lazy val configNamespace = "variantrecalibrator"
//      override def configPath: List[String] = (if (indel) "indel" else "snp") :: super.configPath
//      if (indel) {
//        mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
//        if (config.contains("mills")) resource :+= new TaggedFile(config("mills").asString, "known=false,training=true,truth=true,prior=12.0")
//      } else {
//        mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
//        if (config.contains("hapmap")) resource +:= new TaggedFile(config("hapmap").asString, "known=false,training=true,truth=true,prior=15.0")
//        if (config.contains("omni")) resource +:= new TaggedFile(config("omni").asString, "known=false,training=true,truth=true,prior=12.0")
//        if (config.contains("1000G")) resource +:= new TaggedFile(config("1000G").asString, "known=false,training=true,truth=false,prior=10.0")
//      }
//    }
//    vr.input :+= input
//    vr.recal_file = recal_file
//    vr.tranches_file = tranches_file
//    vr
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, TaggedFile }
import org.broadinstitute.gatk.utils.commandline.{ Gather, Input, Output, _ }

class VariantRecalibrator(val root: Configurable) extends CommandLineGATK {
  analysisName = "VariantRecalibrator"
  analysis_type = "VariantRecalibrator"

  /** Recalibration mode to employ */
  @Argument(fullName = "mode", shortName = "mode", doc = "Recalibration mode to employ", required = true, exclusiveOf = "", validation = "")
  var mode: String = _

  /** Max number of Gaussians for the positive model */
  @Argument(fullName = "maxGaussians", shortName = "mG", doc = "Max number of Gaussians for the positive model", required = false, exclusiveOf = "", validation = "")
  var maxGaussians: Option[Int] = None

  /**
   * Short name of maxGaussians
   * @return Short name of maxGaussians
   */
  def mG = this.maxGaussians

  /**
   * Short name of maxGaussians
   * @param value Short name of maxGaussians
   */
  def mG_=(value: Option[Int]) { this.maxGaussians = value }

  /** Max number of Gaussians for the negative model */
  @Argument(fullName = "maxNegativeGaussians", shortName = "mNG", doc = "Max number of Gaussians for the negative model", required = false, exclusiveOf = "", validation = "")
  var maxNegativeGaussians: Option[Int] = None

  /**
   * Short name of maxNegativeGaussians
   * @return Short name of maxNegativeGaussians
   */
  def mNG = this.maxNegativeGaussians

  /**
   * Short name of maxNegativeGaussians
   * @param value Short name of maxNegativeGaussians
   */
  def mNG_=(value: Option[Int]) { this.maxNegativeGaussians = value }

  /** Maximum number of VBEM iterations */
  @Argument(fullName = "maxIterations", shortName = "mI", doc = "Maximum number of VBEM iterations", required = false, exclusiveOf = "", validation = "")
  var maxIterations: Option[Int] = None

  /**
   * Short name of maxIterations
   * @return Short name of maxIterations
   */
  def mI = this.maxIterations

  /**
   * Short name of maxIterations
   * @param value Short name of maxIterations
   */
  def mI_=(value: Option[Int]) { this.maxIterations = value }

  /** Number of k-means iterations */
  @Argument(fullName = "numKMeans", shortName = "nKM", doc = "Number of k-means iterations", required = false, exclusiveOf = "", validation = "")
  var numKMeans: Option[Int] = None

  /**
   * Short name of numKMeans
   * @return Short name of numKMeans
   */
  def nKM = this.numKMeans

  /**
   * Short name of numKMeans
   * @param value Short name of numKMeans
   */
  def nKM_=(value: Option[Int]) { this.numKMeans = value }

  /** Annotation value divergence threshold (number of standard deviations from the means)  */
  @Argument(fullName = "stdThreshold", shortName = "std", doc = "Annotation value divergence threshold (number of standard deviations from the means) ", required = false, exclusiveOf = "", validation = "")
  var stdThreshold: Option[Double] = None

  /**
   * Short name of stdThreshold
   * @return Short name of stdThreshold
   */
  def std = this.stdThreshold

  /**
   * Short name of stdThreshold
   * @param value Short name of stdThreshold
   */
  def std_=(value: Option[Double]) { this.stdThreshold = value }

  /** Format string for stdThreshold */
  @Argument(fullName = "stdThresholdFormat", shortName = "", doc = "Format string for stdThreshold", required = false, exclusiveOf = "", validation = "")
  var stdThresholdFormat: String = "%s"

  /** The shrinkage parameter in the variational Bayes algorithm. */
  @Argument(fullName = "shrinkage", shortName = "shrinkage", doc = "The shrinkage parameter in the variational Bayes algorithm.", required = false, exclusiveOf = "", validation = "")
  var shrinkage: Option[Double] = None

  /** Format string for shrinkage */
  @Argument(fullName = "shrinkageFormat", shortName = "", doc = "Format string for shrinkage", required = false, exclusiveOf = "", validation = "")
  var shrinkageFormat: String = "%s"

  /** The dirichlet parameter in the variational Bayes algorithm. */
  @Argument(fullName = "dirichlet", shortName = "dirichlet", doc = "The dirichlet parameter in the variational Bayes algorithm.", required = false, exclusiveOf = "", validation = "")
  var dirichlet: Option[Double] = None

  /** Format string for dirichlet */
  @Argument(fullName = "dirichletFormat", shortName = "", doc = "Format string for dirichlet", required = false, exclusiveOf = "", validation = "")
  var dirichletFormat: String = "%s"

  /** The number of prior counts to use in the variational Bayes algorithm. */
  @Argument(fullName = "priorCounts", shortName = "priorCounts", doc = "The number of prior counts to use in the variational Bayes algorithm.", required = false, exclusiveOf = "", validation = "")
  var priorCounts: Option[Double] = None

  /** Format string for priorCounts */
  @Argument(fullName = "priorCountsFormat", shortName = "", doc = "Format string for priorCounts", required = false, exclusiveOf = "", validation = "")
  var priorCountsFormat: String = "%s"

  /** Maximum number of training data */
  @Argument(fullName = "maxNumTrainingData", shortName = "maxNumTrainingData", doc = "Maximum number of training data", required = false, exclusiveOf = "", validation = "")
  var maxNumTrainingData: Option[Int] = None

  /** Minimum number of bad variants */
  @Argument(fullName = "minNumBadVariants", shortName = "minNumBad", doc = "Minimum number of bad variants", required = false, exclusiveOf = "", validation = "")
  var minNumBadVariants: Option[Int] = None

  /**
   * Short name of minNumBadVariants
   * @return Short name of minNumBadVariants
   */
  def minNumBad = this.minNumBadVariants

  /**
   * Short name of minNumBadVariants
   * @param value Short name of minNumBadVariants
   */
  def minNumBad_=(value: Option[Int]) { this.minNumBadVariants = value }

  /** LOD score cutoff for selecting bad variants */
  @Argument(fullName = "badLodCutoff", shortName = "badLodCutoff", doc = "LOD score cutoff for selecting bad variants", required = false, exclusiveOf = "", validation = "")
  var badLodCutoff: Option[Double] = None

  /** Format string for badLodCutoff */
  @Argument(fullName = "badLodCutoffFormat", shortName = "", doc = "Format string for badLodCutoff", required = false, exclusiveOf = "", validation = "")
  var badLodCutoffFormat: String = "%s"

  /** Apply logit transform and jitter to MQ values */
  @Argument(fullName = "MQCapForLogitJitterTransform", shortName = "MQCap", doc = "Apply logit transform and jitter to MQ values", required = false, exclusiveOf = "", validation = "")
  var MQCapForLogitJitterTransform: Option[Int] = None

  /**
   * Short name of MQCapForLogitJitterTransform
   * @return Short name of MQCapForLogitJitterTransform
   */
  def MQCap = this.MQCapForLogitJitterTransform

  /**
   * Short name of MQCapForLogitJitterTransform
   * @param value Short name of MQCapForLogitJitterTransform
   */
  def MQCap_=(value: Option[Int]) { this.MQCapForLogitJitterTransform = value }

  /** MQ is by default transformed to log[(MQ_cap + epsilon - MQ)/(MQ + epsilon)] to make it more Gaussian-like.  Use this flag to not do that. */
  @Argument(fullName = "no_MQ_logit", shortName = "NoMQLogit", doc = "MQ is by default transformed to log[(MQ_cap + epsilon - MQ)/(MQ + epsilon)] to make it more Gaussian-like.  Use this flag to not do that.", required = false, exclusiveOf = "", validation = "")
  var no_MQ_logit: Boolean = _

  /**
   * Short name of no_MQ_logit
   * @return Short name of no_MQ_logit
   */
  def NoMQLogit = this.no_MQ_logit

  /**
   * Short name of no_MQ_logit
   * @param value Short name of no_MQ_logit
   */
  def NoMQLogit_=(value: Boolean) { this.no_MQ_logit = value }

  /** Amount of jitter (as a factor to a Normal(0,1) noise) to add to the MQ capped values */
  @Argument(fullName = "MQ_jitter", shortName = "MQJitt", doc = "Amount of jitter (as a factor to a Normal(0,1) noise) to add to the MQ capped values", required = false, exclusiveOf = "", validation = "")
  var MQ_jitter: Option[Double] = None

  /**
   * Short name of MQ_jitter
   * @return Short name of MQ_jitter
   */
  def MQJitt = this.MQ_jitter

  /**
   * Short name of MQ_jitter
   * @param value Short name of MQ_jitter
   */
  def MQJitt_=(value: Option[Double]) { this.MQ_jitter = value }

  /** Format string for MQ_jitter */
  @Argument(fullName = "MQ_jitterFormat", shortName = "", doc = "Format string for MQ_jitter", required = false, exclusiveOf = "", validation = "")
  var MQ_jitterFormat: String = "%s"

  /** The raw input variants to be recalibrated */
  @Input(fullName = "input", shortName = "input", doc = "The raw input variants to be recalibrated", required = true, exclusiveOf = "", validation = "")
  var input: Seq[File] = Nil

  /** Dependencies on any indexes of input */
  @Input(fullName = "inputIndexes", shortName = "", doc = "Dependencies on any indexes of input", required = false, exclusiveOf = "", validation = "")
  private var inputIndexes: Seq[File] = Nil

  /** Additional raw input variants to be used in building the model */
  @Input(fullName = "aggregate", shortName = "aggregate", doc = "Additional raw input variants to be used in building the model", required = false, exclusiveOf = "", validation = "")
  var aggregate: Seq[File] = Nil

  /** Dependencies on any indexes of aggregate */
  @Input(fullName = "aggregateIndexes", shortName = "", doc = "Dependencies on any indexes of aggregate", required = false, exclusiveOf = "", validation = "")
  private var aggregateIndexes: Seq[File] = Nil

  /** A list of sites for which to apply a prior probability of being correct but which aren't used by the algorithm (training and truth sets are required to run) */
  @Input(fullName = "resource", shortName = "resource", doc = "A list of sites for which to apply a prior probability of being correct but which aren't used by the algorithm (training and truth sets are required to run)", required = true, exclusiveOf = "", validation = "")
  var resource: Seq[File] = Nil

  /** Dependencies on any indexes of resource */
  @Input(fullName = "resourceIndexes", shortName = "", doc = "Dependencies on any indexes of resource", required = false, exclusiveOf = "", validation = "")
  private var resourceIndexes: Seq[File] = Nil

  /** The output recal file used by ApplyRecalibration */
  @Output(fullName = "recal_file", shortName = "recalFile", doc = "The output recal file used by ApplyRecalibration", required = true, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var recal_file: File = _

  /**
   * Short name of recal_file
   * @return Short name of recal_file
   */
  def recalFile = this.recal_file

  /**
   * Short name of recal_file
   * @param value Short name of recal_file
   */
  def recalFile_=(value: File) { this.recal_file = value }

  /** Automatically generated index for recal_file */
  @Output(fullName = "recal_fileIndex", shortName = "", doc = "Automatically generated index for recal_file", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var recal_fileIndex: File = _

  /** The output tranches file used by ApplyRecalibration */
  @Output(fullName = "tranches_file", shortName = "tranchesFile", doc = "The output tranches file used by ApplyRecalibration", required = true, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var tranches_file: File = _

  /**
   * Short name of tranches_file
   * @return Short name of tranches_file
   */
  def tranchesFile = this.tranches_file

  /**
   * Short name of tranches_file
   * @param value Short name of tranches_file
   */
  def tranchesFile_=(value: File) { this.tranches_file = value }

  /** The expected novel Ti/Tv ratio to use when calculating FDR tranches and for display on the optimization curve output figures. (approx 2.15 for whole genome experiments). ONLY USED FOR PLOTTING PURPOSES! */
  @Argument(fullName = "target_titv", shortName = "titv", doc = "The expected novel Ti/Tv ratio to use when calculating FDR tranches and for display on the optimization curve output figures. (approx 2.15 for whole genome experiments). ONLY USED FOR PLOTTING PURPOSES!", required = false, exclusiveOf = "", validation = "")
  var target_titv: Option[Double] = None

  /**
   * Short name of target_titv
   * @return Short name of target_titv
   */
  def titv = this.target_titv

  /**
   * Short name of target_titv
   * @param value Short name of target_titv
   */
  def titv_=(value: Option[Double]) { this.target_titv = value }

  /** Format string for target_titv */
  @Argument(fullName = "target_titvFormat", shortName = "", doc = "Format string for target_titv", required = false, exclusiveOf = "", validation = "")
  var target_titvFormat: String = "%s"

  /** The names of the annotations which should used for calculations */
  @Argument(fullName = "use_annotation", shortName = "an", doc = "The names of the annotations which should used for calculations", required = true, exclusiveOf = "", validation = "")
  var use_annotation: Seq[String] = Nil

  /**
   * Short name of use_annotation
   * @return Short name of use_annotation
   */
  def an = this.use_annotation

  /**
   * Short name of use_annotation
   * @param value Short name of use_annotation
   */
  def an_=(value: Seq[String]) { this.use_annotation = value }

  /** The levels of truth sensitivity at which to slice the data. (in percent, that is 1.0 for 1 percent) */
  @Argument(fullName = "TStranche", shortName = "tranche", doc = "The levels of truth sensitivity at which to slice the data. (in percent, that is 1.0 for 1 percent)", required = false, exclusiveOf = "", validation = "")
  var TStranche: Seq[Double] = Nil

  /**
   * Short name of TStranche
   * @return Short name of TStranche
   */
  def tranche = this.TStranche

  /**
   * Short name of TStranche
   * @param value Short name of TStranche
   */
  def tranche_=(value: Seq[Double]) { this.TStranche = value }

  /** If specified, the variant recalibrator will also use variants marked as filtered by the specified filter name in the input VCF file */
  @Argument(fullName = "ignore_filter", shortName = "ignoreFilter", doc = "If specified, the variant recalibrator will also use variants marked as filtered by the specified filter name in the input VCF file", required = false, exclusiveOf = "", validation = "")
  var ignore_filter: Seq[String] = Nil

  /**
   * Short name of ignore_filter
   * @return Short name of ignore_filter
   */
  def ignoreFilter = this.ignore_filter

  /**
   * Short name of ignore_filter
   * @param value Short name of ignore_filter
   */
  def ignoreFilter_=(value: Seq[String]) { this.ignore_filter = value }

  /** If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file. */
  @Argument(fullName = "ignore_all_filters", shortName = "ignoreAllFilters", doc = "If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file.", required = false, exclusiveOf = "", validation = "")
  var ignore_all_filters: Boolean = _

  /**
   * Short name of ignore_all_filters
   * @return Short name of ignore_all_filters
   */
  def ignoreAllFilters = this.ignore_all_filters

  /**
   * Short name of ignore_all_filters
   * @param value Short name of ignore_all_filters
   */
  def ignoreAllFilters_=(value: Boolean) { this.ignore_all_filters = value }

  /** The output rscript file generated by the VQSR to aid in visualization of the input data and learned model */
  @Output(fullName = "rscript_file", shortName = "rscriptFile", doc = "The output rscript file generated by the VQSR to aid in visualization of the input data and learned model", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var rscript_file: File = _

  /**
   * Short name of rscript_file
   * @return Short name of rscript_file
   */
  def rscriptFile = this.rscript_file

  /**
   * Short name of rscript_file
   * @param value Short name of rscript_file
   */
  def rscriptFile_=(value: File) { this.rscript_file = value }

  /** Used to debug the random number generation inside the VQSR. Do not use. */
  @Argument(fullName = "replicate", shortName = "replicate", doc = "Used to debug the random number generation inside the VQSR. Do not use.", required = false, exclusiveOf = "", validation = "")
  var replicate: Option[Int] = None

  /** Trust that all the input training sets' unfiltered records contain only polymorphic sites to drastically speed up the computation. */
  @Argument(fullName = "trustAllPolymorphic", shortName = "allPoly", doc = "Trust that all the input training sets' unfiltered records contain only polymorphic sites to drastically speed up the computation.", required = false, exclusiveOf = "", validation = "")
  var trustAllPolymorphic: Boolean = _

  /**
   * Short name of trustAllPolymorphic
   * @return Short name of trustAllPolymorphic
   */
  def allPoly = this.trustAllPolymorphic

  /**
   * Short name of trustAllPolymorphic
   * @param value Short name of trustAllPolymorphic
   */
  def allPoly_=(value: Boolean) { this.trustAllPolymorphic = value }

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

  override def freezeFieldValues() {
    super.freezeFieldValues()
    inputIndexes ++= input.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    aggregateIndexes ++= aggregate.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    resourceIndexes ++= resource.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (recal_file != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(recal_file))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(recal_file.getPath))
        recal_fileIndex = new File(recal_file.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine + required("-mode", mode, spaceSeparated = true, escape = true, format = "%s") + optional("-mG", maxGaussians, spaceSeparated = true, escape = true, format = "%s") + optional("-mNG", maxNegativeGaussians, spaceSeparated = true, escape = true, format = "%s") + optional("-mI", maxIterations, spaceSeparated = true, escape = true, format = "%s") + optional("-nKM", numKMeans, spaceSeparated = true, escape = true, format = "%s") + optional("-std", stdThreshold, spaceSeparated = true, escape = true, format = stdThresholdFormat) + optional("-shrinkage", shrinkage, spaceSeparated = true, escape = true, format = shrinkageFormat) + optional("-dirichlet", dirichlet, spaceSeparated = true, escape = true, format = dirichletFormat) + optional("-priorCounts", priorCounts, spaceSeparated = true, escape = true, format = priorCountsFormat) + optional("-maxNumTrainingData", maxNumTrainingData, spaceSeparated = true, escape = true, format = "%s") + optional("-minNumBad", minNumBadVariants, spaceSeparated = true, escape = true, format = "%s") + optional("-badLodCutoff", badLodCutoff, spaceSeparated = true, escape = true, format = badLodCutoffFormat) + optional("-MQCap", MQCapForLogitJitterTransform, spaceSeparated = true, escape = true, format = "%s") + conditional(no_MQ_logit, "-NoMQLogit", escape = true, format = "%s") + optional("-MQJitt", MQ_jitter, spaceSeparated = true, escape = true, format = MQ_jitterFormat) + repeat("-input", input, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") + repeat("-aggregate", aggregate, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") + repeat("-resource", resource, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") + required("-recalFile", recal_file, spaceSeparated = true, escape = true, format = "%s") + required("-tranchesFile", tranches_file, spaceSeparated = true, escape = true, format = "%s") + optional("-titv", target_titv, spaceSeparated = true, escape = true, format = target_titvFormat) + repeat("-an", use_annotation, spaceSeparated = true, escape = true, format = "%s") + repeat("-tranche", TStranche, spaceSeparated = true, escape = true, format = "%s") + repeat("-ignoreFilter", ignore_filter, spaceSeparated = true, escape = true, format = "%s") + conditional(ignore_all_filters, "-ignoreAllFilters", escape = true, format = "%s") + optional("-rscriptFile", rscript_file, spaceSeparated = true, escape = true, format = "%s") + optional("-replicate", replicate, spaceSeparated = true, escape = true, format = "%s") + conditional(trustAllPolymorphic, "-allPoly", escape = true, format = "%s") + conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") + conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") + conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
