package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Input, Output}

class MuTect2(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  def analysis_type: String = "MuTect2"

  /** Getter and setter for tumor sample bam file. */
  def tumorSampleBam_= (value:File):Unit = input_file :+= TaggedFile(value, "tumor")
  def tumorSampleBam = input_file.find(
    file => file.isInstanceOf[TaggedFile] && file.asInstanceOf[TaggedFile].tag == "tumor").getOrElse(null)

  /** Getter and setter for normal sample bam file. */
  def normalSampleBam_= (value:File):Unit = input_file :+= TaggedFile(value, "normal")
  def normalSampleBam = input_file.find(
    file => file.isInstanceOf[TaggedFile] && file.asInstanceOf[TaggedFile].tag == "normal").getOrElse(null)

  /** vcf file with info from cosmic db TODO desc  */
  @Input(fullName = "cosmic", shortName = "cosmic", required = false)
  var cosmic: Option[File] = config("cosmic")

  /** Vcf file of the dbSNP database. When it's provided, then it's possible to use the param 'dbsnpNormalLod', see the
    * description of that parameter for explanation. In addition, sIDs from this file are used to populate the ID column
    * of the output.
    * */
  @Input(fullName = "dbsnp", shortName = "D", required = false)
  var dbsnp: Option[File] = dbsnpVcfFile

  @Input(fullName = "normal_panel", shortName = "PON", required = false)
  var ponFile: Option[File] = config("normal_panel")

  @Input(fullName = "contamination_fraction_per_sample_file", shortName = "contaminationFile", required = false)
  var contaminationFile: Option[File] = config("contamination_file")

  /** Output file of the program. */
  @Output(fullName = "out", shortName = "o", required = true)
  @Gather(classOf[CatVariantsGatherer])
  var outputVcf: File = _

  /**
    * The very first threshold value that is used when assessing if a particular site is a variant in the tumor sample.
    * Two probabilities are calculated: probability of the model that the site is a variant and the probability of the
    * model that it's not. A site is classified as a candidate variant if the logarithm of the ratio between these 2
    * probabilities is higher than this threshold. Candidate variants are subject to further filtering but this value
    * decides if the site enters this processing, it is used for the initial selection. Raising the value increases
    * specificity and lowers sensitivity.
    *
    * Default value: 6.3
    * */
  @Argument(fullName = "tumor_lod", required = false)
  var tumorLOD: Option[Double] = None

  /** TODO: unclear how it differs from the param above 'tumorLod'.
    * Default value: 4.0
    * */
  @Argument(fullName = "initial_tumor_lod", required = false)
  var initialTumorLOD: Option[Double] = None

  /**
    * A threshold used when deciding if a variant found in tumor is reference in the normal sample (the parameter used when
    * finding variants in tumor is described above - 'tumorLod'). If it is reference in the normal, then the variant is
    * classified as a somatic variant existing only in tumor. The threshold is applied on the logarithm of the ratio
    * between the probabilities of the model that normal doesn't have a variant at the site and the probability
    * of the model that it has a variant.
    *
    * Default value: 2.2
    * */
  @Argument(fullName = "normal_lod", required = false)
  var normalLOD: Option[Double] = None

  /** TODO: unclear how it differs from the param above 'normalLod'.
    * Default value: 0.5
    * */
  @Argument(fullName = "initial_normal_lod", required = false)
  var initialNormalLOD: Option[Double] = None

  /**
    * Modelling takes into account also the probability for a site to have a variant in the general population. For sites
    * in the dbSNP database the probability is higher, so the threshold applied when deciding that a variant found in tumor
    * is missing in the normal should also be higher. If a site is in the dbSNP database then this threshold is used, if it's
    * not then the parameter 'normalLod' is used.
    *
    * Default value: 5.5
    * */
  @Argument(fullName = "dbsnp_normal_lod", required = false, otherArgumentRequired = "dbsnp")
  var dbsnpNormalLod: Option[Double] = None

  /** If this fraction is greater than zero, the caller will aggressively attempt to remove contamination through
    * biased down-sampling of reads (for all samples). It will ignore the contamination fraction of reads for each
    * alternate allele. So if the pileup contains N total bases, then we will try to remove (N * contamination fraction)
    * bases for each alternate allele.
    * Default value: 0
    * */
  @Argument(fullName = "contamination_fraction_to_filter", shortName="contamination", required = false)
  var contaminationFractionToFilter: Option[Double] = config("contamination_fraction_to_filter")

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false)
  var group: List[String] = config("group", default = Nil)

  /** Heterozygosity value used to compute prior likelihoods for any locus. */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false)
  var heterozygosity: Option[Double] = config("heterozygosity")

  /** Heterozygosity for indel calling. */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false)
  var heterozygosityForIndels: Option[Double] = config("indel_heterozygosity")

  /** Standard deviation of heterozygosity. */
  @Argument(fullName = "heterozygosity_stdev", shortName = "heterozygosityStandardDeviation", doc = "Standard deviation of heterozygosity", required = false)
  var heterozygositySD: Option[Double] = config("heterozygosity_stdev")

  /** Value used for filtering tumor variants. If a position that has been found to have a variant in tumor, has reads with the variant alternate allele also in the normal sample and if the number of such reads is higher than the value given with this parameter and the sum of the quality scores in these reads in this position is higher than given with the parameter 'maxAltAllelesInNormalQScoreSum', then the variant is filtered out from the final result.
    * Default value:  1 */
  @Argument(fullName = "max_alt_alleles_in_normal_count", required = false)
  var maxAltAllelesInNormalCount: Option[Int] = config("max_alt_alleles_in_normal_count")

  /** Value used for filtering tumor variants. If a position that has been found to have a variant in tumor, has reads with the variant alternate allele also in the normal sample and if the fraction of such reads from all reads is higher than the value given with this parameter and the sum of the quality scores in these reads in this position is higher than given with the parameter 'maxAltAllelesInNormalQScoreSum', then the variant is filtered out from the final result.
    * Default value:  0.03 */
  @Argument(fullName = "max_alt_alleles_in_normal_fraction", required = false)
  var maxAltAllelesInNormalFraction: Option[Double] = config("max_alt_alleles_in_normal_fraction")

  /** For explanation see the description of the parameters above.
    * Default value:  20 */
  @Argument(fullName = "max_alt_alleles_in_qscore_sum", required = false)
  var maxAltAllelesInNormalQScoreSum: Option[Int] = config("max_alt_alleles_in_qscore_sum")

  /** Maximum reads in an active region. When downsampling, level the coverage of the reads in each sample to no more
    * than maxReadsInRegionPerSample reads, not reducing coverage at any read start to less than minReadsPerAlignmentStart */
  @Argument(fullName = "maxReadsInRegionPerSample", shortName = "maxReadsInRegionPerSample", required = false)
  var maxReadsInRegionPerSample: Option[Int] = config("maxReadsInRegionPerSample")

  /** Minimum base quality required to consider a base for calling.
    * Default value:  10 */
  @Argument(fullName = "min_base_quality_score", shortName = "mbq", required = false)
  var minBaseQScore: Option[Int] = config("min_base_quality_score")

  /** Minimum number of reads sharing the same alignment start for each genomic location in an active region */
  @Argument(fullName = "minReadsPerAlignmentStart", shortName = "minReadsPerAlignStart", required = false)
  var minReadsPerAlignmentStart: Option[Int] = config("minReadsPerAlignmentStart")

  /** Value used for filtering tumor variants to exclude false positives caused by misalignments evidenced by alternate alleles occurring near the start and end of reads. Variants are rejected if their median distance from the start/end of of the reads is lower than this parameter and if the median absolute deviation is lower than the value given with the parameter 'pir_mad_threshold'. Filtering is done only if the parameter 'enableClusteredReadPositionFilter' is set to true.
    * Default value:  10 */
  @Argument(fullName = "pir_median_threshold", required = false)
  var pirMedianThreshold: Option[Double] = config("pir_median_threshold")

  /** Value used for filtering tumor variants to exclude false positives caused by misalignments evidenced by alternate alleles occurring near the start and end of reads. Variants are rejected if their median distance from the start/end of the reads is lower than given with the parameter 'pirMedianThreshold' and if the median absolute deviation is lower than given with this parameter. Filtering is done only if the parameter 'enableClusteredReadPositionFilter' is set to true.
    * Default value:  3 */
  @Argument(fullName = "pir_mad_threshold", required = false)
  var pirMadThreshold: Option[Double] = config("pir_mad_threshold")

  /** TODO */
  @Argument(fullName = "power_constant_qscore", required = false)
  var powerConstantQScore: Option[Int] = config("power_constant_qscore")

  /** Ploidy per sample. For pooled data, this should be set to (Number of samples in each pool x Sample Ploidy).
    * Default value: 2
    * */
  @Argument(fullName = "sample_ploidy", shortName="ploidy", required = false)
  var ploidy: Option[Int] = config("sample_ploidy")

  /** The minimum phred-scaled confidence threshold at which variants should be called. */
  @Argument(fullName = "standard_min_confidence_threshold_for_calling", shortName = "stand_call_conf", required = false)
  var standardCallConf: Option[Double] = config("stand_call_conf")

  // flags:

  /** TODO */
  @Argument(fullName = "annotateNDA", shortName = "nda", required = false)
  var annotateNDA: Boolean = config("annotate_nda", default = false)

  /** If this parameter is set to true, then tumor variants are filtered to exclude false positives that are caused by misalignments evidenced by alternate alleles occurring near the start and end of reads. For filtering values of the parameters 'pirMedianThreshold' and 'pirMadThreshold' are used.
    * Default value:  false */
  @Argument(fullName = "enable_clustered_read_position_filter", required = false)
  var enableClusteredReadPositionFilter: Boolean = config("enable_clustered_read_position_filter", default = false)

  /** TODO */
  @Argument(fullName = "enable_strand_artifact_filter", required = false)
  var enableStrandArtifactFilter: Boolean = config("enable_strand_artifact_filter", default = false)

  /** TODO */
  @Argument(fullName = "useNewAFCalculator", shortName = "newQual", required = false)
  var useNewAFCalculator: Boolean = config("use_new_af_calculator", default = false)


  override def cmdLine = super.cmdLine +
    required("--out", outputVcf) +
    optional("--cosmic", cosmic) +
    optional("--dbsnp", dbsnp) +
    optional("--normal_panel", ponFile) +
    optional("--contamination_fraction_per_sample_file", contaminationFile) +
    optional("--contamination_fraction_to_filter", contaminationFractionToFilter) +
    optional("--dbsnp_normal_lod", dbsnpNormalLod) +
    repeat("--group", group) +
    optional("--heterozygosity", heterozygosity) +
    optional("--heterozygosity_stdev", heterozygositySD) +
    optional("--indel_heterozygosity", heterozygosityForIndels) +
    optional("--initial_normal_lod", initialNormalLOD) +
    optional("--initial_tumor_lod", initialTumorLOD) +
    optional("--max_alt_alleles_in_normal_count", maxAltAllelesInNormalCount) +
    optional("--max_alt_alleles_in_normal_fraction", maxAltAllelesInNormalFraction) +
    optional("--max_alt_alleles_in_qscore_sum", maxAltAllelesInNormalQScoreSum) +
    optional("--maxReadsInRegionPerSample", maxReadsInRegionPerSample) +
    optional("--min_base_quality_score", minBaseQScore) +
    optional("--normal_lod", normalLOD) +
    optional("--pir_mad_threshold", pirMadThreshold) +
    optional("--pir_median_threshold", pirMedianThreshold) +
    optional("--power_constant_qscore", powerConstantQScore) +
    optional("--sample_ploidy", ploidy) +
    optional("-stand_call_conf", standardCallConf) +
    optional("--tumor_lod", tumorLOD) +
    conditional(annotateNDA, "--annotateNDA") +
    conditional(enableClusteredReadPositionFilter, "--enable_clustered_read_position_filter") +
    conditional(enableStrandArtifactFilter, "--enable_strand_artifact_filter") +
    conditional(useNewAFCalculator, "--useNewAFCalculator")
}

object MuTect2 {
  def apply(parent: Configurable, tumorSampleBam: File, normalSampleBam: File, output: File): MuTect2 = {
    val mutect2 = new MuTect2(parent)
    mutect2.tumorSampleBam = tumorSampleBam
    mutect2.normalSampleBam = normalSampleBam
    mutect2.outputVcf = output
    mutect2
  }
}
