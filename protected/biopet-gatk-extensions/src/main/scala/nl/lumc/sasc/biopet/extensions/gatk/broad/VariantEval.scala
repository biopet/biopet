/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Input, Output}

//class VariantEval(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantEval with GatkGeneral {
//}
//
//object VariantEval {
//  def apply(root: Configurable, sample: File, compareWith: File,
//            output: File): VariantEval = {
//    val vareval = new VariantEval(root)
//    vareval.eval = Seq(sample)
//    vareval.comp = Seq(compareWith)
//    vareval.out = output
//    vareval
//  }
//
//  def apply(root: Configurable, sample: File, compareWith: File,
//            output: File, ST: Seq[String], EV: Seq[String]): VariantEval = {
//    val vareval = new VariantEval(root)
//    vareval.eval = Seq(sample)
//    vareval.comp = Seq(compareWith)
//    vareval.out = output
//    vareval.noST = true
//    vareval.ST = ST
//    vareval.noEV = true
//    vareval.EV = EV
//    vareval
//  }
//
//}

import java.io.File
import org.broadinstitute.gatk.utils.commandline.Argument
import org.broadinstitute.gatk.utils.commandline.Gather
import org.broadinstitute.gatk.utils.commandline.Input
import org.broadinstitute.gatk.utils.commandline.Output

class VariantEval(val root: Configurable) extends CommandLineGATK {
  def analysis_type = "VariantEval"

  /** An output file created by the walker.  Will overwrite contents if file exists */
  @Output(fullName = "out", shortName = "o", doc = "An output file created by the walker.  Will overwrite contents if file exists", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var out: File = _

  /** Input evaluation file(s) */
  @Input(fullName = "eval", shortName = "eval", doc = "Input evaluation file(s)", required = true, exclusiveOf = "", validation = "")
  var eval: Seq[File] = Nil

  /** Input comparison file(s) */
  @Input(fullName = "comp", shortName = "comp", doc = "Input comparison file(s)", required = false, exclusiveOf = "", validation = "")
  var comp: Seq[File] = Nil

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: Option[File] = None

  /** Evaluations that count calls at sites of true variation (e.g., indel calls) will use this argument as their gold standard for comparison */
  @Input(fullName = "goldStandard", shortName = "gold", doc = "Evaluations that count calls at sites of true variation (e.g., indel calls) will use this argument as their gold standard for comparison", required = false, exclusiveOf = "", validation = "")
  var goldStandard: Option[File] = None

  /** List the available eval modules and exit */
  @Argument(fullName = "list", shortName = "ls", doc = "List the available eval modules and exit", required = false, exclusiveOf = "", validation = "")
  var list: Boolean = _

  /** One or more stratifications to use when evaluating the data */
  @Argument(fullName = "select_exps", shortName = "select", doc = "One or more stratifications to use when evaluating the data", required = false, exclusiveOf = "", validation = "")
  var select_exps: Seq[String] = Nil

  /** Names to use for the list of stratifications (must be a 1-to-1 mapping) */
  @Argument(fullName = "select_names", shortName = "selectName", doc = "Names to use for the list of stratifications (must be a 1-to-1 mapping)", required = false, exclusiveOf = "", validation = "")
  var select_names: Seq[String] = Nil

  /** Derive eval and comp contexts using only these sample genotypes, when genotypes are available in the original context */
  @Argument(fullName = "sample", shortName = "sn", doc = "Derive eval and comp contexts using only these sample genotypes, when genotypes are available in the original context", required = false, exclusiveOf = "", validation = "")
  var sample: Seq[String] = Nil

  /** Name of ROD bindings containing variant sites that should be treated as known when splitting eval rods into known and novel subsets */
  @Argument(fullName = "known_names", shortName = "knownName", doc = "Name of ROD bindings containing variant sites that should be treated as known when splitting eval rods into known and novel subsets", required = false, exclusiveOf = "", validation = "")
  var known_names: Seq[String] = Nil

  /** One or more specific stratification modules to apply to the eval track(s) (in addition to the standard stratifications, unless -noS is specified) */
  @Argument(fullName = "stratificationModule", shortName = "ST", doc = "One or more specific stratification modules to apply to the eval track(s) (in addition to the standard stratifications, unless -noS is specified)", required = false, exclusiveOf = "", validation = "")
  var stratificationModule: Seq[String] = Nil

  /** Do not use the standard stratification modules by default (instead, only those that are specified with the -S option) */
  @Argument(fullName = "doNotUseAllStandardStratifications", shortName = "noST", doc = "Do not use the standard stratification modules by default (instead, only those that are specified with the -S option)", required = false, exclusiveOf = "", validation = "")
  var doNotUseAllStandardStratifications: Boolean = _

  /** One or more specific eval modules to apply to the eval track(s) (in addition to the standard modules, unless -noEV is specified) */
  @Argument(fullName = "evalModule", shortName = "EV", doc = "One or more specific eval modules to apply to the eval track(s) (in addition to the standard modules, unless -noEV is specified)", required = false, exclusiveOf = "", validation = "")
  var evalModule: Seq[String] = Nil

  /** Do not use the standard modules by default (instead, only those that are specified with the -EV option) */
  @Argument(fullName = "doNotUseAllStandardModules", shortName = "noEV", doc = "Do not use the standard modules by default (instead, only those that are specified with the -EV option)", required = false, exclusiveOf = "", validation = "")
  var doNotUseAllStandardModules: Boolean = _

  /** Minimum phasing quality */
  @Argument(fullName = "minPhaseQuality", shortName = "mpq", doc = "Minimum phasing quality", required = false, exclusiveOf = "", validation = "")
  var minPhaseQuality: Option[Double] = None

  /** Format string for minPhaseQuality */
  @Argument(fullName = "minPhaseQualityFormat", shortName = "", doc = "Format string for minPhaseQuality", required = false, exclusiveOf = "", validation = "")
  var minPhaseQualityFormat: String = "%s"

  /** Minimum genotype QUAL score for each trio member required to accept a site as a violation. Default is 50. */
  @Argument(fullName = "mendelianViolationQualThreshold", shortName = "mvq", doc = "Minimum genotype QUAL score for each trio member required to accept a site as a violation. Default is 50.", required = false, exclusiveOf = "", validation = "")
  var mendelianViolationQualThreshold: Option[Double] = None

  /** Format string for mendelianViolationQualThreshold */
  @Argument(fullName = "mendelianViolationQualThresholdFormat", shortName = "", doc = "Format string for mendelianViolationQualThreshold", required = false, exclusiveOf = "", validation = "")
  var mendelianViolationQualThresholdFormat: String = "%s"

  /** Per-sample ploidy (number of chromosomes per sample) */
  @Argument(fullName = "samplePloidy", shortName = "ploidy", doc = "Per-sample ploidy (number of chromosomes per sample)", required = false, exclusiveOf = "", validation = "")
  var samplePloidy: Option[Int] = None

  /** Fasta file with ancestral alleles */
  @Argument(fullName = "ancestralAlignments", shortName = "aa", doc = "Fasta file with ancestral alleles", required = false, exclusiveOf = "", validation = "")
  var ancestralAlignments: File = _

  /** If provided only comp and eval tracks with exactly matching reference and alternate alleles will be counted as overlapping */
  @Argument(fullName = "requireStrictAlleleMatch", shortName = "strict", doc = "If provided only comp and eval tracks with exactly matching reference and alternate alleles will be counted as overlapping", required = false, exclusiveOf = "", validation = "")
  var requireStrictAlleleMatch: Boolean = _

  /** If provided, modules that track polymorphic sites will not require that a site have AC > 0 when the input eval has genotypes */
  @Argument(fullName = "keepAC0", shortName = "keepAC0", doc = "If provided, modules that track polymorphic sites will not require that a site have AC > 0 when the input eval has genotypes", required = false, exclusiveOf = "", validation = "")
  var keepAC0: Boolean = _

  /** If provided, modules that track polymorphic sites will not require that a site have AC > 0 when the input eval has genotypes */
  @Argument(fullName = "numSamples", shortName = "numSamples", doc = "If provided, modules that track polymorphic sites will not require that a site have AC > 0 when the input eval has genotypes", required = false, exclusiveOf = "", validation = "")
  var numSamples: Option[Int] = None

  /** If provided, all -eval tracks will be merged into a single eval track */
  @Argument(fullName = "mergeEvals", shortName = "mergeEvals", doc = "If provided, all -eval tracks will be merged into a single eval track", required = false, exclusiveOf = "", validation = "")
  var mergeEvals: Boolean = _

  /** File containing tribble-readable features for the IntervalStratificiation */
  @Input(fullName = "stratIntervals", shortName = "stratIntervals", doc = "File containing tribble-readable features for the IntervalStratificiation", required = false, exclusiveOf = "", validation = "")
  var stratIntervals: File = _

  /** File containing tribble-readable features describing a known list of copy number variants */
  @Input(fullName = "knownCNVs", shortName = "knownCNVs", doc = "File containing tribble-readable features describing a known list of copy number variants", required = false, exclusiveOf = "", validation = "")
  var knownCNVs: File = _

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = _

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = _

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = _

  override def freezeFieldValues() {
    super.freezeFieldValues()
    deps ++= eval.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    deps ++= comp.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    dbsnp.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    goldStandard.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
  }

  override def cmdLine = super.cmdLine +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-eval", eval, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-comp", comp, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-gold", goldStandard), goldStandard, spaceSeparated = true, escape = true, format = "%s") +
    conditional(list, "-ls", escape = true, format = "%s") +
    repeat("-select", select_exps, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-selectName", select_names, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-sn", sample, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-knownName", known_names, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-ST", stratificationModule, spaceSeparated = true, escape = true, format = "%s") +
    conditional(doNotUseAllStandardStratifications, "-noST", escape = true, format = "%s") +
    repeat("-EV", evalModule, spaceSeparated = true, escape = true, format = "%s") +
    conditional(doNotUseAllStandardModules, "-noEV", escape = true, format = "%s") +
    optional("-mpq", minPhaseQuality, spaceSeparated = true, escape = true, format = minPhaseQualityFormat) +
    optional("-mvq", mendelianViolationQualThreshold, spaceSeparated = true, escape = true, format = mendelianViolationQualThresholdFormat) +
    optional("-ploidy", samplePloidy, spaceSeparated = true, escape = true, format = "%s") +
    optional("-aa", ancestralAlignments, spaceSeparated = true, escape = true, format = "%s") +
    conditional(requireStrictAlleleMatch, "-strict", escape = true, format = "%s") +
    conditional(keepAC0, "-keepAC0", escape = true, format = "%s") +
    optional("-numSamples", numSamples, spaceSeparated = true, escape = true, format = "%s") +
    conditional(mergeEvals, "-mergeEvals", escape = true, format = "%s") +
    optional("-stratIntervals", stratIntervals, spaceSeparated = true, escape = true, format = "%s") +
    optional("-knownCNVs", knownCNVs, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
