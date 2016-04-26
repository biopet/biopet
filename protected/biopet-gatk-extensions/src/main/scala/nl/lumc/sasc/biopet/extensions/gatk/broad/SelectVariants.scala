/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

//import java.io.File
//
//import nl.lumc.sasc.biopet.utils.config.Configurable
//
//class SelectVariants(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.SelectVariants with GatkGeneral {
//  if (config.contains("scattercount")) scatterCount = config("scattercount")
//}
//
//object SelectVariants {
//  def apply(root: Configurable, input: File, output: File): SelectVariants = {
//    val sv = new SelectVariants(root)
//    sv.variant = input
//    sv.out = output
//    sv
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class SelectVariants(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "SelectVariants"
  analysis_type = "SelectVariants"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** Input VCF file */
  @Input(fullName = "variant", shortName = "V", doc = "Input VCF file", required = true, exclusiveOf = "", validation = "")
  var variant: File = _

  /**
   * Short name of variant
   * @return Short name of variant
   */
  def V = this.variant

  /**
   * Short name of variant
   * @param value Short name of variant
   */
  def V_=(value: File) { this.variant = value }

  /** Dependencies on the index of variant */
  @Input(fullName = "variantIndex", shortName = "", doc = "Dependencies on the index of variant", required = false, exclusiveOf = "", validation = "")
  private var variantIndex: Seq[File] = Nil

  /** Output variants not called in this comparison track */
  @Input(fullName = "discordance", shortName = "disc", doc = "Output variants not called in this comparison track", required = false, exclusiveOf = "", validation = "")
  var discordance: File = _

  /**
   * Short name of discordance
   * @return Short name of discordance
   */
  def disc = this.discordance

  /**
   * Short name of discordance
   * @param value Short name of discordance
   */
  def disc_=(value: File) { this.discordance = value }

  /** Dependencies on the index of discordance */
  @Input(fullName = "discordanceIndex", shortName = "", doc = "Dependencies on the index of discordance", required = false, exclusiveOf = "", validation = "")
  private var discordanceIndex: Seq[File] = Nil

  /** Output variants also called in this comparison track */
  @Input(fullName = "concordance", shortName = "conc", doc = "Output variants also called in this comparison track", required = false, exclusiveOf = "", validation = "")
  var concordance: File = _

  /**
   * Short name of concordance
   * @return Short name of concordance
   */
  def conc = this.concordance

  /**
   * Short name of concordance
   * @param value Short name of concordance
   */
  def conc_=(value: File) { this.concordance = value }

  /** Dependencies on the index of concordance */
  @Input(fullName = "concordanceIndex", shortName = "", doc = "Dependencies on the index of concordance", required = false, exclusiveOf = "", validation = "")
  private var concordanceIndex: Seq[File] = Nil

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

  /** Include genotypes from this sample */
  @Argument(fullName = "sample_name", shortName = "sn", doc = "Include genotypes from this sample", required = false, exclusiveOf = "", validation = "")
  var sample_name: Seq[String] = Nil

  /**
   * Short name of sample_name
   * @return Short name of sample_name
   */
  def sn = this.sample_name

  /**
   * Short name of sample_name
   * @param value Short name of sample_name
   */
  def sn_=(value: Seq[String]) { this.sample_name = value }

  /** Regular expression to select multiple samples */
  @Argument(fullName = "sample_expressions", shortName = "se", doc = "Regular expression to select multiple samples", required = false, exclusiveOf = "", validation = "")
  var sample_expressions: Seq[String] = Nil

  /**
   * Short name of sample_expressions
   * @return Short name of sample_expressions
   */
  def se = this.sample_expressions

  /**
   * Short name of sample_expressions
   * @param value Short name of sample_expressions
   */
  def se_=(value: Seq[String]) { this.sample_expressions = value }

  /** File containing a list of samples to include */
  @Input(fullName = "sample_file", shortName = "sf", doc = "File containing a list of samples to include", required = false, exclusiveOf = "", validation = "")
  var sample_file: Seq[File] = Nil

  /**
   * Short name of sample_file
   * @return Short name of sample_file
   */
  def sf = this.sample_file

  /**
   * Short name of sample_file
   * @param value Short name of sample_file
   */
  def sf_=(value: Seq[File]) { this.sample_file = value }

  /** Exclude genotypes from this sample */
  @Argument(fullName = "exclude_sample_name", shortName = "xl_sn", doc = "Exclude genotypes from this sample", required = false, exclusiveOf = "", validation = "")
  var exclude_sample_name: Seq[String] = Nil

  /**
   * Short name of exclude_sample_name
   * @return Short name of exclude_sample_name
   */
  def xl_sn = this.exclude_sample_name

  /**
   * Short name of exclude_sample_name
   * @param value Short name of exclude_sample_name
   */
  def xl_sn_=(value: Seq[String]) { this.exclude_sample_name = value }

  /** List of samples to exclude */
  @Input(fullName = "exclude_sample_file", shortName = "xl_sf", doc = "List of samples to exclude", required = false, exclusiveOf = "", validation = "")
  var exclude_sample_file: Seq[File] = Nil

  /**
   * Short name of exclude_sample_file
   * @return Short name of exclude_sample_file
   */
  def xl_sf = this.exclude_sample_file

  /**
   * Short name of exclude_sample_file
   * @param value Short name of exclude_sample_file
   */
  def xl_sf_=(value: Seq[File]) { this.exclude_sample_file = value }

  /** List of sample expressions to exclude */
  @Input(fullName = "exclude_sample_expressions", shortName = "xl_se", doc = "List of sample expressions to exclude", required = false, exclusiveOf = "", validation = "")
  var exclude_sample_expressions: Seq[File] = Nil

  /**
   * Short name of exclude_sample_expressions
   * @return Short name of exclude_sample_expressions
   */
  def xl_se = this.exclude_sample_expressions

  /**
   * Short name of exclude_sample_expressions
   * @param value Short name of exclude_sample_expressions
   */
  def xl_se_=(value: Seq[File]) { this.exclude_sample_expressions = value }

  /** One or more criteria to use when selecting the data */
  @Argument(fullName = "selectexpressions", shortName = "select", doc = "One or more criteria to use when selecting the data", required = false, exclusiveOf = "", validation = "")
  var selectexpressions: Seq[String] = Nil

  /**
   * Short name of selectexpressions
   * @return Short name of selectexpressions
   */
  def select = this.selectexpressions

  /**
   * Short name of selectexpressions
   * @param value Short name of selectexpressions
   */
  def select_=(value: Seq[String]) { this.selectexpressions = value }

  /** Invert the selection criteria for -select */
  @Argument(fullName = "invertselect", shortName = "invertSelect", doc = "Invert the selection criteria for -select", required = false, exclusiveOf = "", validation = "")
  var invertselect: Boolean = _

  /**
   * Short name of invertselect
   * @return Short name of invertselect
   */
  def invertSelect = this.invertselect

  /**
   * Short name of invertselect
   * @param value Short name of invertselect
   */
  def invertSelect_=(value: Boolean) { this.invertselect = value }

  /** Don't include non-variant sites */
  @Argument(fullName = "excludeNonVariants", shortName = "env", doc = "Don't include non-variant sites", required = false, exclusiveOf = "", validation = "")
  var excludeNonVariants: Boolean = _

  /**
   * Short name of excludeNonVariants
   * @return Short name of excludeNonVariants
   */
  def env = this.excludeNonVariants

  /**
   * Short name of excludeNonVariants
   * @param value Short name of excludeNonVariants
   */
  def env_=(value: Boolean) { this.excludeNonVariants = value }

  /** Don't include filtered sites */
  @Argument(fullName = "excludeFiltered", shortName = "ef", doc = "Don't include filtered sites", required = false, exclusiveOf = "", validation = "")
  var excludeFiltered: Boolean = _

  /**
   * Short name of excludeFiltered
   * @return Short name of excludeFiltered
   */
  def ef = this.excludeFiltered

  /**
   * Short name of excludeFiltered
   * @param value Short name of excludeFiltered
   */
  def ef_=(value: Boolean) { this.excludeFiltered = value }

  /** Preserve original alleles, do not trim */
  @Argument(fullName = "preserveAlleles", shortName = "noTrim", doc = "Preserve original alleles, do not trim", required = false, exclusiveOf = "", validation = "")
  var preserveAlleles: Boolean = _

  /**
   * Short name of preserveAlleles
   * @return Short name of preserveAlleles
   */
  def noTrim = this.preserveAlleles

  /**
   * Short name of preserveAlleles
   * @param value Short name of preserveAlleles
   */
  def noTrim_=(value: Boolean) { this.preserveAlleles = value }

  /** Remove alternate alleles not present in any genotypes */
  @Argument(fullName = "removeUnusedAlternates", shortName = "trimAlternates", doc = "Remove alternate alleles not present in any genotypes", required = false, exclusiveOf = "", validation = "")
  var removeUnusedAlternates: Boolean = _

  /**
   * Short name of removeUnusedAlternates
   * @return Short name of removeUnusedAlternates
   */
  def trimAlternates = this.removeUnusedAlternates

  /**
   * Short name of removeUnusedAlternates
   * @param value Short name of removeUnusedAlternates
   */
  def trimAlternates_=(value: Boolean) { this.removeUnusedAlternates = value }

  /** Select only variants of a particular allelicity */
  @Argument(fullName = "restrictAllelesTo", shortName = "restrictAllelesTo", doc = "Select only variants of a particular allelicity", required = false, exclusiveOf = "", validation = "")
  var restrictAllelesTo: org.broadinstitute.gatk.tools.walkers.variantutils.SelectVariants.NumberAlleleRestriction = _

  /** Store the original AC, AF, and AN values after subsetting */
  @Argument(fullName = "keepOriginalAC", shortName = "keepOriginalAC", doc = "Store the original AC, AF, and AN values after subsetting", required = false, exclusiveOf = "", validation = "")
  var keepOriginalAC: Boolean = _

  /** Store the original DP value after subsetting */
  @Argument(fullName = "keepOriginalDP", shortName = "keepOriginalDP", doc = "Store the original DP value after subsetting", required = false, exclusiveOf = "", validation = "")
  var keepOriginalDP: Boolean = _

  /** Output mendelian violation sites only */
  @Argument(fullName = "mendelianViolation", shortName = "mv", doc = "Output mendelian violation sites only", required = false, exclusiveOf = "", validation = "")
  var mendelianViolation: Boolean = _

  /**
   * Short name of mendelianViolation
   * @return Short name of mendelianViolation
   */
  def mv = this.mendelianViolation

  /**
   * Short name of mendelianViolation
   * @param value Short name of mendelianViolation
   */
  def mv_=(value: Boolean) { this.mendelianViolation = value }

  /** Output non-mendelian violation sites only */
  @Argument(fullName = "invertMendelianViolation", shortName = "invMv", doc = "Output non-mendelian violation sites only", required = false, exclusiveOf = "", validation = "")
  var invertMendelianViolation: Boolean = _

  /**
   * Short name of invertMendelianViolation
   * @return Short name of invertMendelianViolation
   */
  def invMv = this.invertMendelianViolation

  /**
   * Short name of invertMendelianViolation
   * @param value Short name of invertMendelianViolation
   */
  def invMv_=(value: Boolean) { this.invertMendelianViolation = value }

  /** Minimum GQ score for each trio member to accept a site as a violation */
  @Argument(fullName = "mendelianViolationQualThreshold", shortName = "mvq", doc = "Minimum GQ score for each trio member to accept a site as a violation", required = false, exclusiveOf = "", validation = "")
  var mendelianViolationQualThreshold: Option[Double] = None

  /**
   * Short name of mendelianViolationQualThreshold
   * @return Short name of mendelianViolationQualThreshold
   */
  def mvq = this.mendelianViolationQualThreshold

  /**
   * Short name of mendelianViolationQualThreshold
   * @param value Short name of mendelianViolationQualThreshold
   */
  def mvq_=(value: Option[Double]) { this.mendelianViolationQualThreshold = value }

  /** Format string for mendelianViolationQualThreshold */
  @Argument(fullName = "mendelianViolationQualThresholdFormat", shortName = "", doc = "Format string for mendelianViolationQualThreshold", required = false, exclusiveOf = "", validation = "")
  var mendelianViolationQualThresholdFormat: String = "%s"

  /** Select a fraction of variants at random from the input */
  @Argument(fullName = "select_random_fraction", shortName = "fraction", doc = "Select a fraction of variants at random from the input", required = false, exclusiveOf = "", validation = "")
  var select_random_fraction: Option[Double] = None

  /**
   * Short name of select_random_fraction
   * @return Short name of select_random_fraction
   */
  def fraction = this.select_random_fraction

  /**
   * Short name of select_random_fraction
   * @param value Short name of select_random_fraction
   */
  def fraction_=(value: Option[Double]) { this.select_random_fraction = value }

  /** Format string for select_random_fraction */
  @Argument(fullName = "select_random_fractionFormat", shortName = "", doc = "Format string for select_random_fraction", required = false, exclusiveOf = "", validation = "")
  var select_random_fractionFormat: String = "%s"

  /** Select a fraction of genotypes at random from the input and sets them to no-call */
  @Argument(fullName = "remove_fraction_genotypes", shortName = "fractionGenotypes", doc = "Select a fraction of genotypes at random from the input and sets them to no-call", required = false, exclusiveOf = "", validation = "")
  var remove_fraction_genotypes: Option[Double] = None

  /**
   * Short name of remove_fraction_genotypes
   * @return Short name of remove_fraction_genotypes
   */
  def fractionGenotypes = this.remove_fraction_genotypes

  /**
   * Short name of remove_fraction_genotypes
   * @param value Short name of remove_fraction_genotypes
   */
  def fractionGenotypes_=(value: Option[Double]) { this.remove_fraction_genotypes = value }

  /** Format string for remove_fraction_genotypes */
  @Argument(fullName = "remove_fraction_genotypesFormat", shortName = "", doc = "Format string for remove_fraction_genotypes", required = false, exclusiveOf = "", validation = "")
  var remove_fraction_genotypesFormat: String = "%s"

  /** Select only a certain type of variants from the input file */
  @Argument(fullName = "selectTypeToInclude", shortName = "selectType", doc = "Select only a certain type of variants from the input file", required = false, exclusiveOf = "", validation = "")
  var selectTypeToInclude: Seq[htsjdk.variant.variantcontext.VariantContext.Type] = Nil

  /**
   * Short name of selectTypeToInclude
   * @return Short name of selectTypeToInclude
   */
  def selectType = this.selectTypeToInclude

  /**
   * Short name of selectTypeToInclude
   * @param value Short name of selectTypeToInclude
   */
  def selectType_=(value: Seq[htsjdk.variant.variantcontext.VariantContext.Type]) { this.selectTypeToInclude = value }

  /** Do not select certain type of variants from the input file */
  @Argument(fullName = "selectTypeToExclude", shortName = "xlSelectType", doc = "Do not select certain type of variants from the input file", required = false, exclusiveOf = "", validation = "")
  var selectTypeToExclude: Seq[htsjdk.variant.variantcontext.VariantContext.Type] = Nil

  /**
   * Short name of selectTypeToExclude
   * @return Short name of selectTypeToExclude
   */
  def xlSelectType = this.selectTypeToExclude

  /**
   * Short name of selectTypeToExclude
   * @param value Short name of selectTypeToExclude
   */
  def xlSelectType_=(value: Seq[htsjdk.variant.variantcontext.VariantContext.Type]) { this.selectTypeToExclude = value }

  /** List of variant IDs to select */
  @Argument(fullName = "keepIDs", shortName = "IDs", doc = "List of variant IDs to select", required = false, exclusiveOf = "", validation = "")
  var keepIDs: File = _

  /**
   * Short name of keepIDs
   * @return Short name of keepIDs
   */
  def IDs = this.keepIDs

  /**
   * Short name of keepIDs
   * @param value Short name of keepIDs
   */
  def IDs_=(value: File) { this.keepIDs = value }

  /** List of variant IDs to select */
  @Argument(fullName = "excludeIDs", shortName = "xlIDs", doc = "List of variant IDs to select", required = false, exclusiveOf = "", validation = "")
  var excludeIDs: File = _

  /**
   * Short name of excludeIDs
   * @return Short name of excludeIDs
   */
  def xlIDs = this.excludeIDs

  /**
   * Short name of excludeIDs
   * @param value Short name of excludeIDs
   */
  def xlIDs_=(value: File) { this.excludeIDs = value }

  /** If true, the incoming VariantContext will be fully decoded */
  @Argument(fullName = "fullyDecode", shortName = "", doc = "If true, the incoming VariantContext will be fully decoded", required = false, exclusiveOf = "", validation = "")
  var fullyDecode: Boolean = _

  /** If true, we won't actually write the output file.  For efficiency testing only */
  @Argument(fullName = "justRead", shortName = "", doc = "If true, we won't actually write the output file.  For efficiency testing only", required = false, exclusiveOf = "", validation = "")
  var justRead: Boolean = _

  /** Maximum size of indels to include */
  @Argument(fullName = "maxIndelSize", shortName = "", doc = "Maximum size of indels to include", required = false, exclusiveOf = "", validation = "")
  var maxIndelSize: Option[Int] = None

  /** Minimum size of indels to include */
  @Argument(fullName = "minIndelSize", shortName = "", doc = "Minimum size of indels to include", required = false, exclusiveOf = "", validation = "")
  var minIndelSize: Option[Int] = None

  /** Maximum number of samples filtered at the genotype level */
  @Argument(fullName = "maxFilteredGenotypes", shortName = "", doc = "Maximum number of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var maxFilteredGenotypes: Option[Int] = None

  /** Minimum number of samples filtered at the genotype level */
  @Argument(fullName = "minFilteredGenotypes", shortName = "", doc = "Minimum number of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var minFilteredGenotypes: Option[Int] = None

  /** Maximum fraction of samples filtered at the genotype level */
  @Argument(fullName = "maxFractionFilteredGenotypes", shortName = "", doc = "Maximum fraction of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var maxFractionFilteredGenotypes: Option[Double] = None

  /** Format string for maxFractionFilteredGenotypes */
  @Argument(fullName = "maxFractionFilteredGenotypesFormat", shortName = "", doc = "Format string for maxFractionFilteredGenotypes", required = false, exclusiveOf = "", validation = "")
  var maxFractionFilteredGenotypesFormat: String = "%s"

  /** Maximum fraction of samples filtered at the genotype level */
  @Argument(fullName = "minFractionFilteredGenotypes", shortName = "", doc = "Maximum fraction of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var minFractionFilteredGenotypes: Option[Double] = None

  /** Format string for minFractionFilteredGenotypes */
  @Argument(fullName = "minFractionFilteredGenotypesFormat", shortName = "", doc = "Format string for minFractionFilteredGenotypes", required = false, exclusiveOf = "", validation = "")
  var minFractionFilteredGenotypesFormat: String = "%s"

  /** Set filtered genotypes to no-call */
  @Argument(fullName = "setFilteredGtToNocall", shortName = "", doc = "Set filtered genotypes to no-call", required = false, exclusiveOf = "", validation = "")
  var setFilteredGtToNocall: Boolean = _

  /** Allow samples other than those in the VCF to be specified on the command line. These samples will be ignored. */
  @Argument(fullName = "ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES", shortName = "", doc = "Allow samples other than those in the VCF to be specified on the command line. These samples will be ignored.", required = false, exclusiveOf = "", validation = "")
  var ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES: Boolean = _

  /** Forces output VCF to be compliant to up-to-date version */
  @Argument(fullName = "forceValidOutput", shortName = "", doc = "Forces output VCF to be compliant to up-to-date version", required = false, exclusiveOf = "", validation = "")
  var forceValidOutput: Boolean = _

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
    if (variant != null)
      variantIndex :+= new File(variant.getPath + ".idx")
    if (discordance != null)
      discordanceIndex :+= new File(discordance.getPath + ".idx")
    if (concordance != null)
      concordanceIndex :+= new File(concordance.getPath + ".idx")
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(out.getPath))
        outIndex = new File(out.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine +
    required(TaggedFile.formatCommandLineParameter("-V", variant), variant, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-disc", discordance), discordance, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-conc", concordance), concordance, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-sn", sample_name, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-se", sample_expressions, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-sf", sample_file, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-xl_sn", exclude_sample_name, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-xl_sf", exclude_sample_file, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-xl_se", exclude_sample_expressions, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-select", selectexpressions, spaceSeparated = true, escape = true, format = "%s") +
    conditional(invertselect, "-invertSelect", escape = true, format = "%s") +
    conditional(excludeNonVariants, "-env", escape = true, format = "%s") +
    conditional(excludeFiltered, "-ef", escape = true, format = "%s") +
    conditional(preserveAlleles, "-noTrim", escape = true, format = "%s") +
    conditional(removeUnusedAlternates, "-trimAlternates", escape = true, format = "%s") +
    optional("-restrictAllelesTo", restrictAllelesTo, spaceSeparated = true, escape = true, format = "%s") +
    conditional(keepOriginalAC, "-keepOriginalAC", escape = true, format = "%s") +
    conditional(keepOriginalDP, "-keepOriginalDP", escape = true, format = "%s") +
    conditional(mendelianViolation, "-mv", escape = true, format = "%s") +
    conditional(invertMendelianViolation, "-invMv", escape = true, format = "%s") +
    optional("-mvq", mendelianViolationQualThreshold, spaceSeparated = true, escape = true, format = mendelianViolationQualThresholdFormat) +
    optional("-fraction", select_random_fraction, spaceSeparated = true, escape = true, format = select_random_fractionFormat) +
    optional("-fractionGenotypes", remove_fraction_genotypes, spaceSeparated = true, escape = true, format = remove_fraction_genotypesFormat) +
    repeat("-selectType", selectTypeToInclude, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-xlSelectType", selectTypeToExclude, spaceSeparated = true, escape = true, format = "%s") +
    optional("-IDs", keepIDs, spaceSeparated = true, escape = true, format = "%s") +
    optional("-xlIDs", excludeIDs, spaceSeparated = true, escape = true, format = "%s") +
    conditional(fullyDecode, "--fullyDecode", escape = true, format = "%s") +
    conditional(justRead, "--justRead", escape = true, format = "%s") +
    optional("--maxIndelSize", maxIndelSize, spaceSeparated = true, escape = true, format = "%s") +
    optional("--minIndelSize", minIndelSize, spaceSeparated = true, escape = true, format = "%s") +
    optional("--maxFilteredGenotypes", maxFilteredGenotypes, spaceSeparated = true, escape = true, format = "%s") +
    optional("--minFilteredGenotypes", minFilteredGenotypes, spaceSeparated = true, escape = true, format = "%s") +
    optional("--maxFractionFilteredGenotypes", maxFractionFilteredGenotypes, spaceSeparated = true, escape = true, format = maxFractionFilteredGenotypesFormat) +
    optional("--minFractionFilteredGenotypes", minFractionFilteredGenotypes, spaceSeparated = true, escape = true, format = minFractionFilteredGenotypesFormat) +
    conditional(setFilteredGtToNocall, "--setFilteredGtToNocall", escape = true, format = "%s") +
    conditional(ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES, "--ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES", escape = true, format = "%s") +
    conditional(forceValidOutput, "--forceValidOutput", escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
