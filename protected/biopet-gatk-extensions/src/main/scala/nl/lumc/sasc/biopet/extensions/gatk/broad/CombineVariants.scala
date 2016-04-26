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
//class CombineVariants(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.CombineVariants with GatkGeneral {
//  if (config.contains("scattercount")) scatterCount = config("scattercount")
//}
//
//object CombineVariants {
//  def apply(root: Configurable, input: List[File], output: File): CombineVariants = {
//    val cv = new CombineVariants(root)
//    cv.variant = input
//    cv.out = output
//    cv
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class CombineVariants(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "CombineVariants"
  analysis_type = "CombineVariants"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** VCF files to merge together */
  @Input(fullName = "variant", shortName = "V", doc = "VCF files to merge together", required = true, exclusiveOf = "", validation = "")
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

  /** Determines how we should merge genotype records for samples shared across the ROD files */
  @Argument(fullName = "genotypemergeoption", shortName = "genotypeMergeOptions", doc = "Determines how we should merge genotype records for samples shared across the ROD files", required = false, exclusiveOf = "", validation = "")
  var genotypemergeoption: org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.GenotypeMergeType = _

  /**
   * Short name of genotypemergeoption
   * @return Short name of genotypemergeoption
   */
  def genotypeMergeOptions = this.genotypemergeoption

  /**
   * Short name of genotypemergeoption
   * @param value Short name of genotypemergeoption
   */
  def genotypeMergeOptions_=(value: org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.GenotypeMergeType) { this.genotypemergeoption = value }

  /** Determines how we should handle records seen at the same site in the VCF, but with different FILTER fields */
  @Argument(fullName = "filteredrecordsmergetype", shortName = "filteredRecordsMergeType", doc = "Determines how we should handle records seen at the same site in the VCF, but with different FILTER fields", required = false, exclusiveOf = "", validation = "")
  var filteredrecordsmergetype: org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.FilteredRecordMergeType = _

  /**
   * Short name of filteredrecordsmergetype
   * @return Short name of filteredrecordsmergetype
   */
  def filteredRecordsMergeType = this.filteredrecordsmergetype

  /**
   * Short name of filteredrecordsmergetype
   * @param value Short name of filteredrecordsmergetype
   */
  def filteredRecordsMergeType_=(value: org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.FilteredRecordMergeType) { this.filteredrecordsmergetype = value }

  /** Determines how we should handle records seen at the same site in the VCF, but with different allele types (for example, SNP vs. indel) */
  @Argument(fullName = "multipleallelesmergetype", shortName = "multipleAllelesMergeType", doc = "Determines how we should handle records seen at the same site in the VCF, but with different allele types (for example, SNP vs. indel)", required = false, exclusiveOf = "", validation = "")
  var multipleallelesmergetype: org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.MultipleAllelesMergeType = _

  /**
   * Short name of multipleallelesmergetype
   * @return Short name of multipleallelesmergetype
   */
  def multipleAllelesMergeType = this.multipleallelesmergetype

  /**
   * Short name of multipleallelesmergetype
   * @param value Short name of multipleallelesmergetype
   */
  def multipleAllelesMergeType_=(value: org.broadinstitute.gatk.utils.variant.GATKVariantContextUtils.MultipleAllelesMergeType) { this.multipleallelesmergetype = value }

  /** Ordered list specifying priority for merging */
  @Argument(fullName = "rod_priority_list", shortName = "priority", doc = "Ordered list specifying priority for merging", required = false, exclusiveOf = "", validation = "")
  var rod_priority_list: String = _

  /**
   * Short name of rod_priority_list
   * @return Short name of rod_priority_list
   */
  def priority = this.rod_priority_list

  /**
   * Short name of rod_priority_list
   * @param value Short name of rod_priority_list
   */
  def priority_=(value: String) { this.rod_priority_list = value }

  /** Emit interesting sites requiring complex compatibility merging to file */
  @Argument(fullName = "printComplexMerges", shortName = "printComplexMerges", doc = "Emit interesting sites requiring complex compatibility merging to file", required = false, exclusiveOf = "", validation = "")
  var printComplexMerges: Boolean = _

  /** Treat filtered variants as uncalled */
  @Argument(fullName = "filteredAreUncalled", shortName = "filteredAreUncalled", doc = "Treat filtered variants as uncalled", required = false, exclusiveOf = "", validation = "")
  var filteredAreUncalled: Boolean = _

  /** Emit a sites-only file */
  @Argument(fullName = "minimalVCF", shortName = "minimalVCF", doc = "Emit a sites-only file", required = false, exclusiveOf = "", validation = "")
  var minimalVCF: Boolean = _

  /** Exclude sites where no variation is present after merging */
  @Argument(fullName = "excludeNonVariants", shortName = "env", doc = "Exclude sites where no variation is present after merging", required = false, exclusiveOf = "", validation = "")
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

  /** Key name for the set attribute */
  @Argument(fullName = "setKey", shortName = "setKey", doc = "Key name for the set attribute", required = false, exclusiveOf = "", validation = "")
  var setKey: String = _

  /** Assume input VCFs have identical sample sets and disjoint calls */
  @Argument(fullName = "assumeIdenticalSamples", shortName = "assumeIdenticalSamples", doc = "Assume input VCFs have identical sample sets and disjoint calls", required = false, exclusiveOf = "", validation = "")
  var assumeIdenticalSamples: Boolean = _

  /** Minimum number of input files the site must be observed in to be included */
  @Argument(fullName = "minimumN", shortName = "minN", doc = "Minimum number of input files the site must be observed in to be included", required = false, exclusiveOf = "", validation = "")
  var minimumN: Option[Int] = None

  /**
   * Short name of minimumN
   * @return Short name of minimumN
   */
  def minN = this.minimumN

  /**
   * Short name of minimumN
   * @param value Short name of minimumN
   */
  def minN_=(value: Option[Int]) { this.minimumN = value }

  /** Do not output the command line to the header */
  @Argument(fullName = "suppressCommandLineHeader", shortName = "suppressCommandLineHeader", doc = "Do not output the command line to the header", required = false, exclusiveOf = "", validation = "")
  var suppressCommandLineHeader: Boolean = _

  /** Use the INFO content of the record with the highest AC */
  @Argument(fullName = "mergeInfoWithMaxAC", shortName = "mergeInfoWithMaxAC", doc = "Use the INFO content of the record with the highest AC", required = false, exclusiveOf = "", validation = "")
  var mergeInfoWithMaxAC: Boolean = _

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
  }

  override def cmdLine = super.cmdLine +
    repeat("-V", variant, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    optional("-genotypeMergeOptions", genotypemergeoption, spaceSeparated = true, escape = true, format = "%s") +
    optional("-filteredRecordsMergeType", filteredrecordsmergetype, spaceSeparated = true, escape = true, format = "%s") +
    optional("-multipleAllelesMergeType", multipleallelesmergetype, spaceSeparated = true, escape = true, format = "%s") +
    optional("-priority", rod_priority_list, spaceSeparated = true, escape = true, format = "%s") +
    conditional(printComplexMerges, "-printComplexMerges", escape = true, format = "%s") +
    conditional(filteredAreUncalled, "-filteredAreUncalled", escape = true, format = "%s") +
    conditional(minimalVCF, "-minimalVCF", escape = true, format = "%s") +
    conditional(excludeNonVariants, "-env", escape = true, format = "%s") +
    optional("-setKey", setKey, spaceSeparated = true, escape = true, format = "%s") +
    conditional(assumeIdenticalSamples, "-assumeIdenticalSamples", escape = true, format = "%s") +
    optional("-minN", minimumN, spaceSeparated = true, escape = true, format = "%s") +
    conditional(suppressCommandLineHeader, "-suppressCommandLineHeader", escape = true, format = "%s") +
    conditional(mergeInfoWithMaxAC, "-mergeInfoWithMaxAC", escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
