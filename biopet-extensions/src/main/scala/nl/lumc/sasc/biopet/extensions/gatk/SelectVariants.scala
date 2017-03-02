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
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class SelectVariants(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "SelectVariants"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** Input VCF file */
  @Input(fullName = "variant", shortName = "V", doc = "Input VCF file", required = true, exclusiveOf = "", validation = "")
  var variant: File = _

  /** Output variants not called in this comparison track */
  @Input(fullName = "discordance", shortName = "disc", doc = "Output variants not called in this comparison track", required = false, exclusiveOf = "", validation = "")
  var discordance: Option[File] = None

  /** Output variants also called in this comparison track */
  @Input(fullName = "concordance", shortName = "conc", doc = "Output variants also called in this comparison track", required = false, exclusiveOf = "", validation = "")
  var concordance: Option[File] = None

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /** Include genotypes from this sample */
  @Argument(fullName = "sample_name", shortName = "sn", doc = "Include genotypes from this sample", required = false, exclusiveOf = "", validation = "")
  var sample_name: List[String] = config("sample_name", default = Nil)

  /** Regular expression to select multiple samples */
  @Argument(fullName = "sample_expressions", shortName = "se", doc = "Regular expression to select multiple samples", required = false, exclusiveOf = "", validation = "")
  var sample_expressions: List[String] = config("sample_expressions", default = Nil)

  /** File containing a list of samples to include */
  @Input(fullName = "sample_file", shortName = "sf", doc = "File containing a list of samples to include", required = false, exclusiveOf = "", validation = "")
  var sample_file: List[File] = config("sample_file", default = Nil)

  /** Exclude genotypes from this sample */
  @Argument(fullName = "exclude_sample_name", shortName = "xl_sn", doc = "Exclude genotypes from this sample", required = false, exclusiveOf = "", validation = "")
  var exclude_sample_name: List[String] = config("exclude_sample_name", default = Nil)

  /** List of samples to exclude */
  @Input(fullName = "exclude_sample_file", shortName = "xl_sf", doc = "List of samples to exclude", required = false, exclusiveOf = "", validation = "")
  var exclude_sample_file: List[File] = config("exclude_sample_file", default = Nil)

  /** List of sample expressions to exclude */
  @Input(fullName = "exclude_sample_expressions", shortName = "xl_se", doc = "List of sample expressions to exclude", required = false, exclusiveOf = "", validation = "")
  var exclude_sample_expressions: List[File] = config("exclude_sample_expressions", default = Nil)

  /** One or more criteria to use when selecting the data */
  @Argument(fullName = "selectexpressions", shortName = "select", doc = "One or more criteria to use when selecting the data", required = false, exclusiveOf = "", validation = "")
  var selectexpressions: List[String] = config("selectexpressions", default = Nil)

  /** Invert the selection criteria for -select */
  @Argument(fullName = "invertselect", shortName = "invertSelect", doc = "Invert the selection criteria for -select", required = false, exclusiveOf = "", validation = "")
  var invertselect: Boolean = config("invertselect", default = false)

  /** Don't include non-variant sites */
  @Argument(fullName = "excludeNonVariants", shortName = "env", doc = "Don't include non-variant sites", required = false, exclusiveOf = "", validation = "")
  var excludeNonVariants: Boolean = config("excludeNonVariants", default = false)

  /** Don't include filtered sites */
  @Argument(fullName = "excludeFiltered", shortName = "ef", doc = "Don't include filtered sites", required = false, exclusiveOf = "", validation = "")
  var excludeFiltered: Boolean = config("excludeFiltered", default = false)

  /** Preserve original alleles, do not trim */
  @Argument(fullName = "preserveAlleles", shortName = "noTrim", doc = "Preserve original alleles, do not trim", required = false, exclusiveOf = "", validation = "")
  var preserveAlleles: Boolean = config("preserveAlleles", default = false)

  /** Remove alternate alleles not present in any genotypes */
  @Argument(fullName = "removeUnusedAlternates", shortName = "trimAlternates", doc = "Remove alternate alleles not present in any genotypes", required = false, exclusiveOf = "", validation = "")
  var removeUnusedAlternates: Boolean = config("removeUnusedAlternates", default = false)

  /** Select only variants of a particular allelicity */
  @Argument(fullName = "restrictAllelesTo", shortName = "restrictAllelesTo", doc = "Select only variants of a particular allelicity", required = false, exclusiveOf = "", validation = "")
  var restrictAllelesTo: Option[String] = config("restrictAllelesTo")

  /** Store the original AC, AF, and AN values after subsetting */
  @Argument(fullName = "keepOriginalAC", shortName = "keepOriginalAC", doc = "Store the original AC, AF, and AN values after subsetting", required = false, exclusiveOf = "", validation = "")
  var keepOriginalAC: Boolean = config("keepOriginalAC", default = false)

  /** Store the original DP value after subsetting */
  @Argument(fullName = "keepOriginalDP", shortName = "keepOriginalDP", doc = "Store the original DP value after subsetting", required = false, exclusiveOf = "", validation = "")
  var keepOriginalDP: Boolean = config("keepOriginalDP", default = false)

  /** Output mendelian violation sites only */
  @Argument(fullName = "mendelianViolation", shortName = "mv", doc = "Output mendelian violation sites only", required = false, exclusiveOf = "", validation = "")
  var mendelianViolation: Boolean = config("mendelianViolation", default = false)

  /** Output non-mendelian violation sites only */
  @Argument(fullName = "invertMendelianViolation", shortName = "invMv", doc = "Output non-mendelian violation sites only", required = false, exclusiveOf = "", validation = "")
  var invertMendelianViolation: Boolean = config("invertMendelianViolation", default = false)

  /** Minimum GQ score for each trio member to accept a site as a violation */
  @Argument(fullName = "mendelianViolationQualThreshold", shortName = "mvq", doc = "Minimum GQ score for each trio member to accept a site as a violation", required = false, exclusiveOf = "", validation = "")
  var mendelianViolationQualThreshold: Option[Double] = config("mendelianViolationQualThreshold")

  /** Format string for mendelianViolationQualThreshold */
  @Argument(fullName = "mendelianViolationQualThresholdFormat", shortName = "", doc = "Format string for mendelianViolationQualThreshold", required = false, exclusiveOf = "", validation = "")
  var mendelianViolationQualThresholdFormat: String = "%s"

  /** Select a fraction of variants at random from the input */
  @Argument(fullName = "select_random_fraction", shortName = "fraction", doc = "Select a fraction of variants at random from the input", required = false, exclusiveOf = "", validation = "")
  var select_random_fraction: Option[Double] = config("select_random_fraction")

  /** Format string for select_random_fraction */
  @Argument(fullName = "select_random_fractionFormat", shortName = "", doc = "Format string for select_random_fraction", required = false, exclusiveOf = "", validation = "")
  var select_random_fractionFormat: String = "%s"

  /** Select a fraction of genotypes at random from the input and sets them to no-call */
  @Argument(fullName = "remove_fraction_genotypes", shortName = "fractionGenotypes", doc = "Select a fraction of genotypes at random from the input and sets them to no-call", required = false, exclusiveOf = "", validation = "")
  var remove_fraction_genotypes: Option[Double] = config("remove_fraction_genotypes")

  /** Format string for remove_fraction_genotypes */
  @Argument(fullName = "remove_fraction_genotypesFormat", shortName = "", doc = "Format string for remove_fraction_genotypes", required = false, exclusiveOf = "", validation = "")
  var remove_fraction_genotypesFormat: String = "%s"

  /** Select only a certain type of variants from the input file */
  @Argument(fullName = "selectTypeToInclude", shortName = "selectType", doc = "Select only a certain type of variants from the input file", required = false, exclusiveOf = "", validation = "")
  var selectTypeToInclude: List[String] = config("selectTypeToInclude", default = Nil)

  /** Do not select certain type of variants from the input file */
  @Argument(fullName = "selectTypeToExclude", shortName = "xlSelectType", doc = "Do not select certain type of variants from the input file", required = false, exclusiveOf = "", validation = "")
  var selectTypeToExclude: Seq[String] = config("selectTypeToExclude", default = Nil)

  /** List of variant IDs to select */
  @Input(fullName = "keepIDs", shortName = "IDs", doc = "List of variant IDs to select", required = false, exclusiveOf = "", validation = "")
  var keepIDs: Option[File] = config("keepIDs")

  /** List of variant IDs to select */
  @Argument(fullName = "excludeIDs", shortName = "xlIDs", doc = "List of variant IDs to select", required = false, exclusiveOf = "", validation = "")
  var excludeIDs: Option[File] = config("excludeIDs")

  /** If true, the incoming VariantContext will be fully decoded */
  @Argument(fullName = "fullyDecode", shortName = "", doc = "If true, the incoming VariantContext will be fully decoded", required = false, exclusiveOf = "", validation = "")
  var fullyDecode: Boolean = config("fullyDecode", default = false)

  /** If true, we won't actually write the output file.  For efficiency testing only */
  @Argument(fullName = "justRead", shortName = "", doc = "If true, we won't actually write the output file.  For efficiency testing only", required = false, exclusiveOf = "", validation = "")
  var justRead: Boolean = config("justRead", default = false)

  /** Maximum size of indels to include */
  @Argument(fullName = "maxIndelSize", shortName = "", doc = "Maximum size of indels to include", required = false, exclusiveOf = "", validation = "")
  var maxIndelSize: Option[Int] = config("maxIndelSize")

  /** Minimum size of indels to include */
  @Argument(fullName = "minIndelSize", shortName = "", doc = "Minimum size of indels to include", required = false, exclusiveOf = "", validation = "")
  var minIndelSize: Option[Int] = config("minIndelSize")

  /** Maximum number of samples filtered at the genotype level */
  @Argument(fullName = "maxFilteredGenotypes", shortName = "", doc = "Maximum number of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var maxFilteredGenotypes: Option[Int] = config("maxFilteredGenotypes")

  /** Minimum number of samples filtered at the genotype level */
  @Argument(fullName = "minFilteredGenotypes", shortName = "", doc = "Minimum number of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var minFilteredGenotypes: Option[Int] = config("minFilteredGenotypes")

  /** Maximum fraction of samples filtered at the genotype level */
  @Argument(fullName = "maxFractionFilteredGenotypes", shortName = "", doc = "Maximum fraction of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var maxFractionFilteredGenotypes: Option[Double] = config("maxFractionFilteredGenotypes")

  /** Format string for maxFractionFilteredGenotypes */
  @Argument(fullName = "maxFractionFilteredGenotypesFormat", shortName = "", doc = "Format string for maxFractionFilteredGenotypes", required = false, exclusiveOf = "", validation = "")
  var maxFractionFilteredGenotypesFormat: String = "%s"

  /** Maximum fraction of samples filtered at the genotype level */
  @Argument(fullName = "minFractionFilteredGenotypes", shortName = "", doc = "Maximum fraction of samples filtered at the genotype level", required = false, exclusiveOf = "", validation = "")
  var minFractionFilteredGenotypes: Option[Double] = config("minFractionFilteredGenotypes")

  /** Format string for minFractionFilteredGenotypes */
  @Argument(fullName = "minFractionFilteredGenotypesFormat", shortName = "", doc = "Format string for minFractionFilteredGenotypes", required = false, exclusiveOf = "", validation = "")
  var minFractionFilteredGenotypesFormat: String = "%s"

  /** Set filtered genotypes to no-call */
  @Argument(fullName = "setFilteredGtToNocall", shortName = "", doc = "Set filtered genotypes to no-call", required = false, exclusiveOf = "", validation = "")
  var setFilteredGtToNocall: Boolean = config("setFilteredGtToNocall", default = false)

  /** Allow samples other than those in the VCF to be specified on the command line. These samples will be ignored. */
  @Argument(fullName = "ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES", shortName = "", doc = "Allow samples other than those in the VCF to be specified on the command line. These samples will be ignored.", required = false, exclusiveOf = "", validation = "")
  var ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES: Boolean = config("ALLOW_NONOVERLAPPING_COMMAND_LINE_SAMPLES", default = false)

  /** Forces output VCF to be compliant to up-to-date version */
  @Argument(fullName = "forceValidOutput", shortName = "", doc = "Forces output VCF to be compliant to up-to-date version", required = false, exclusiveOf = "", validation = "")
  var forceValidOutput: Boolean = config("forceValidOutput", default = false)

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
    if (variant != null)
      deps :+= VcfUtils.getVcfIndexFile(variant)
    discordance.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    concordance.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
  }

  override def cmdLine = super.cmdLine +
    required(TaggedFile.formatCommandLineParameter("-V", variant), variant, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-disc", discordance.getOrElse(null)), discordance, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-conc", concordance.getOrElse(null)), concordance, spaceSeparated = true, escape = true, format = "%s") +
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
