/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

//import java.io.File
//
//import nl.lumc.sasc.biopet.utils.config.Configurable
//import org.broadinstitute.gatk.utils.commandline.{ Gather, Output }
//
//class GenotypeGVCFs(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.GenotypeGVCFs with GatkGeneral {
//
//  @Gather(enabled = false)
//  @Output(required = false)
//  protected var vcfIndex: File = _
//
//  annotation ++= config("annotation", default = Seq(), freeVar = false).asStringList
//
//  if (config.contains("dbsnp")) dbsnp = config("dbsnp")
//  if (config.contains("scattercount", "genotypegvcfs")) scatterCount = config("scattercount")
//
//  if (config("inputtype", default = "dna").asString == "rna") {
//    stand_call_conf = config("stand_call_conf", default = 20)
//    stand_emit_conf = config("stand_emit_conf", default = 0)
//  } else {
//    stand_call_conf = config("stand_call_conf", default = 30)
//    stand_emit_conf = config("stand_emit_conf", default = 0)
//  }
//
//  override def freezeFieldValues(): Unit = {
//    super.freezeFieldValues()
//    if (out.getName.endsWith(".vcf.gz")) vcfIndex = new File(out.getAbsolutePath + ".tbi")
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class GenotypeGVCFs(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "GenotypeGVCFs"
  analysis_type = "GenotypeGVCFs"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** One or more input gVCF files */
  @Input(fullName = "variant", shortName = "V", doc = "One or more input gVCF files", required = true, exclusiveOf = "", validation = "")
  var variant: Seq[File] = Nil

  /**
   * Short name of variant
   * @return Short name of variant
   */
  def V = this.variant

  /**
   * Short name of variant
   * @param value Short name of variant
   */
  def V_=(value: Seq[File]) { this.variant = value }

  /** Dependencies on any indexes of variant */
  @Input(fullName = "variantIndexes", shortName = "", doc = "Dependencies on any indexes of variant", required = false, exclusiveOf = "", validation = "")
  private var variantIndexes: Seq[File] = Nil

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /**
   * Short name of out
   * @return Short name of out
   */
  def o = this.out

  /**
   * Short name of out
   * @param value Short name of out
   */
  def o_=(value: File) { this.out = value }

  /** Automatically generated index for out */
  @Output(fullName = "outIndex", shortName = "", doc = "Automatically generated index for out", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var outIndex: File = _

  /** Include loci found to be non-variant after genotyping */
  @Argument(fullName = "includeNonVariantSites", shortName = "allSites", doc = "Include loci found to be non-variant after genotyping", required = false, exclusiveOf = "", validation = "")
  var includeNonVariantSites: Boolean = _

  /**
   * Short name of includeNonVariantSites
   * @return Short name of includeNonVariantSites
   */
  def allSites = this.includeNonVariantSites

  /**
   * Short name of includeNonVariantSites
   * @param value Short name of includeNonVariantSites
   */
  def allSites_=(value: Boolean) { this.includeNonVariantSites = value }

  /** Assume duplicate samples are present and uniquify all names with '.variant' and file number index */
  @Argument(fullName = "uniquifySamples", shortName = "uniquifySamples", doc = "Assume duplicate samples are present and uniquify all names with '.variant' and file number index", required = false, exclusiveOf = "", validation = "")
  var uniquifySamples: Boolean = _

  /** If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site */
  @Argument(fullName = "annotateNDA", shortName = "nda", doc = "If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site", required = false, exclusiveOf = "", validation = "")
  var annotateNDA: Boolean = _

  /**
   * Short name of annotateNDA
   * @return Short name of annotateNDA
   */
  def nda = this.annotateNDA

  /**
   * Short name of annotateNDA
   * @param value Short name of annotateNDA
   */
  def nda_=(value: Boolean) { this.annotateNDA = value }

  /** Heterozygosity value used to compute prior likelihoods for any locus */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false, exclusiveOf = "", validation = "")
  var heterozygosity: Option[Double] = None

  /**
   * Short name of heterozygosity
   * @return Short name of heterozygosity
   */
  def hets = this.heterozygosity

  /**
   * Short name of heterozygosity
   * @param value Short name of heterozygosity
   */
  def hets_=(value: Option[Double]) { this.heterozygosity = value }

  /** Format string for heterozygosity */
  @Argument(fullName = "heterozygosityFormat", shortName = "", doc = "Format string for heterozygosity", required = false, exclusiveOf = "", validation = "")
  var heterozygosityFormat: String = "%s"

  /** Heterozygosity for indel calling */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosity: Option[Double] = None

  /**
   * Short name of indel_heterozygosity
   * @return Short name of indel_heterozygosity
   */
  def indelHeterozygosity = this.indel_heterozygosity

  /**
   * Short name of indel_heterozygosity
   * @param value Short name of indel_heterozygosity
   */
  def indelHeterozygosity_=(value: Option[Double]) { this.indel_heterozygosity = value }

  /** Format string for indel_heterozygosity */
  @Argument(fullName = "indel_heterozygosityFormat", shortName = "", doc = "Format string for indel_heterozygosity", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosityFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be called */
  @Argument(fullName = "standard_min_confidence_threshold_for_calling", shortName = "stand_call_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be called", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_calling: Option[Double] = None

  /**
   * Short name of standard_min_confidence_threshold_for_calling
   * @return Short name of standard_min_confidence_threshold_for_calling
   */
  def stand_call_conf = this.standard_min_confidence_threshold_for_calling

  /**
   * Short name of standard_min_confidence_threshold_for_calling
   * @param value Short name of standard_min_confidence_threshold_for_calling
   */
  def stand_call_conf_=(value: Option[Double]) { this.standard_min_confidence_threshold_for_calling = value }

  /** Format string for standard_min_confidence_threshold_for_calling */
  @Argument(fullName = "standard_min_confidence_threshold_for_callingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_callingFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold) */
  @Argument(fullName = "standard_min_confidence_threshold_for_emitting", shortName = "stand_emit_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold)", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emitting: Option[Double] = None

  /**
   * Short name of standard_min_confidence_threshold_for_emitting
   * @return Short name of standard_min_confidence_threshold_for_emitting
   */
  def stand_emit_conf = this.standard_min_confidence_threshold_for_emitting

  /**
   * Short name of standard_min_confidence_threshold_for_emitting
   * @param value Short name of standard_min_confidence_threshold_for_emitting
   */
  def stand_emit_conf_=(value: Option[Double]) { this.standard_min_confidence_threshold_for_emitting = value }

  /** Format string for standard_min_confidence_threshold_for_emitting */
  @Argument(fullName = "standard_min_confidence_threshold_for_emittingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_emitting", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emittingFormat: String = "%s"

  /** Maximum number of alternate alleles to genotype */
  @Argument(fullName = "max_alternate_alleles", shortName = "maxAltAlleles", doc = "Maximum number of alternate alleles to genotype", required = false, exclusiveOf = "", validation = "")
  var max_alternate_alleles: Option[Int] = None

  /**
   * Short name of max_alternate_alleles
   * @return Short name of max_alternate_alleles
   */
  def maxAltAlleles = this.max_alternate_alleles

  /**
   * Short name of max_alternate_alleles
   * @param value Short name of max_alternate_alleles
   */
  def maxAltAlleles_=(value: Option[Int]) { this.max_alternate_alleles = value }

  /** Input prior for calls */
  @Argument(fullName = "input_prior", shortName = "inputPrior", doc = "Input prior for calls", required = false, exclusiveOf = "", validation = "")
  var input_prior: Seq[Double] = Nil

  /**
   * Short name of input_prior
   * @return Short name of input_prior
   */
  def inputPrior = this.input_prior

  /**
   * Short name of input_prior
   * @param value Short name of input_prior
   */
  def inputPrior_=(value: Seq[Double]) { this.input_prior = value }

  /** Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy). */
  @Argument(fullName = "sample_ploidy", shortName = "ploidy", doc = "Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy).", required = false, exclusiveOf = "", validation = "")
  var sample_ploidy: Option[Int] = None

  /**
   * Short name of sample_ploidy
   * @return Short name of sample_ploidy
   */
  def ploidy = this.sample_ploidy

  /**
   * Short name of sample_ploidy
   * @param value Short name of sample_ploidy
   */
  def ploidy_=(value: Option[Int]) { this.sample_ploidy = value }

  /** One or more specific annotations to recompute.  The single value 'none' removes the default annotations */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to recompute.  The single value 'none' removes the default annotations", required = false, exclusiveOf = "", validation = "")
  var annotation: Seq[String] = Nil

  /**
   * Short name of annotation
   * @return Short name of annotation
   */
  def A = this.annotation

  /**
   * Short name of annotation
   * @param value Short name of annotation
   */
  def A_=(value: Seq[String]) { this.annotation = value }

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var group: Seq[String] = Nil

  /**
   * Short name of group
   * @return Short name of group
   */
  def G = this.group

  /**
   * Short name of group
   * @param value Short name of group
   */
  def G_=(value: Seq[String]) { this.group = value }

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: File = _

  /**
   * Short name of dbsnp
   * @return Short name of dbsnp
   */
  def D = this.dbsnp

  /**
   * Short name of dbsnp
   * @param value Short name of dbsnp
   */
  def D_=(value: File) { this.dbsnp = value }

  /** Dependencies on the index of dbsnp */
  @Input(fullName = "dbsnpIndex", shortName = "", doc = "Dependencies on the index of dbsnp", required = false, exclusiveOf = "", validation = "")
  private var dbsnpIndex: Seq[File] = Nil

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
    variantIndexes ++= variant.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(out.getPath))
        outIndex = new File(out.getPath + ".idx")
    if (dbsnp != null)
      dbsnpIndex :+= new File(dbsnp.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine +
    repeat("-V", variant, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    conditional(includeNonVariantSites, "-allSites", escape = true, format = "%s") +
    conditional(uniquifySamples, "-uniquifySamples", escape = true, format = "%s") +
    conditional(annotateNDA, "-nda", escape = true, format = "%s") +
    optional("-hets", heterozygosity, spaceSeparated = true, escape = true, format = heterozygosityFormat) +
    optional("-indelHeterozygosity", indel_heterozygosity, spaceSeparated = true, escape = true, format = indel_heterozygosityFormat) +
    optional("-stand_call_conf", standard_min_confidence_threshold_for_calling, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_callingFormat) +
    optional("-stand_emit_conf", standard_min_confidence_threshold_for_emitting, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_emittingFormat) +
    optional("-maxAltAlleles", max_alternate_alleles, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-inputPrior", input_prior, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ploidy", sample_ploidy, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object GenotypeGVCFs {
  def apply(root: Configurable, gvcfFiles: List[File], output: File): GenotypeGVCFs = {
    val gg = new GenotypeGVCFs(root)
    gg.variant = gvcfFiles
    gg.out = output
    //if (gg.out.getName.endsWith(".vcf.gz")) gg.vcfIndex = new File(gg.out.getAbsolutePath + ".tbi")
    gg
  }
}
