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
import nl.lumc.sasc.biopet.extensions.gatk.gather.GatherVcfs
import nl.lumc.sasc.biopet.extensions.gatk.scatter.{GATKScatterFunction, LocusScatterFunction}
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Output, _}

class CombineVariants(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "CombineVariants"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** VCF files to merge together */
  @Input(fullName = "variant", shortName = "V", doc = "VCF files to merge together", required = true, exclusiveOf = "", validation = "")
  var variant: Seq[File] = Nil

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[GatherVcfs])
  var out: File = _

  /** Determines how we should merge genotype records for samples shared across the ROD files */
  @Argument(fullName = "genotypemergeoption", shortName = "genotypeMergeOptions", doc = "Determines how we should merge genotype records for samples shared across the ROD files", required = false, exclusiveOf = "", validation = "")
  var genotypemergeoption: Option[String] = config("genotypemergeoption")

  /** Determines how we should handle records seen at the same site in the VCF, but with different FILTER fields */
  @Argument(fullName = "filteredrecordsmergetype", shortName = "filteredRecordsMergeType", doc = "Determines how we should handle records seen at the same site in the VCF, but with different FILTER fields", required = false, exclusiveOf = "", validation = "")
  var filteredrecordsmergetype: Option[String] = config("filteredrecordsmergetype")

  /** Determines how we should handle records seen at the same site in the VCF, but with different allele types (for example, SNP vs. indel) */
  @Argument(fullName = "multipleallelesmergetype", shortName = "multipleAllelesMergeType", doc = "Determines how we should handle records seen at the same site in the VCF, but with different allele types (for example, SNP vs. indel)", required = false, exclusiveOf = "", validation = "")
  var multipleallelesmergetype: Option[String] = config("multipleallelesmergetype")

  /** Ordered list specifying priority for merging */
  @Argument(fullName = "rod_priority_list", shortName = "priority", doc = "Ordered list specifying priority for merging", required = false, exclusiveOf = "", validation = "")
  var rod_priority_list: Option[String] = config("rod_priority_list")

  /** Emit interesting sites requiring complex compatibility merging to file */
  @Argument(fullName = "printComplexMerges", shortName = "printComplexMerges", doc = "Emit interesting sites requiring complex compatibility merging to file", required = false, exclusiveOf = "", validation = "")
  var printComplexMerges: Boolean = config("printComplexMerges", default = false)

  /** Treat filtered variants as uncalled */
  @Argument(fullName = "filteredAreUncalled", shortName = "filteredAreUncalled", doc = "Treat filtered variants as uncalled", required = false, exclusiveOf = "", validation = "")
  var filteredAreUncalled: Boolean = config("filteredAreUncalled", default = false)

  /** Emit a sites-only file */
  @Argument(fullName = "minimalVCF", shortName = "minimalVCF", doc = "Emit a sites-only file", required = false, exclusiveOf = "", validation = "")
  var minimalVCF: Boolean = config("minimalVCF", default = false)

  /** Exclude sites where no variation is present after merging */
  @Argument(fullName = "excludeNonVariants", shortName = "env", doc = "Exclude sites where no variation is present after merging", required = false, exclusiveOf = "", validation = "")
  var excludeNonVariants: Boolean = config("excludeNonVariants", default = false)

  /** Key name for the set attribute */
  @Argument(fullName = "setKey", shortName = "setKey", doc = "Key name for the set attribute", required = false, exclusiveOf = "", validation = "")
  var setKey: Option[String] = config("set_key")

  /** Assume input VCFs have identical sample sets and disjoint calls */
  @Argument(fullName = "assumeIdenticalSamples", shortName = "assumeIdenticalSamples", doc = "Assume input VCFs have identical sample sets and disjoint calls", required = false, exclusiveOf = "", validation = "")
  var assumeIdenticalSamples: Boolean = config("assumeIdenticalSamples", default = false)

  /** Minimum number of input files the site must be observed in to be included */
  @Argument(fullName = "minimumN", shortName = "minN", doc = "Minimum number of input files the site must be observed in to be included", required = false, exclusiveOf = "", validation = "")
  var minimumN: Option[Int] = config("minimumN")

  /** Do not output the command line to the header */
  @Argument(fullName = "suppressCommandLineHeader", shortName = "suppressCommandLineHeader", doc = "Do not output the command line to the header", required = false, exclusiveOf = "", validation = "")
  var suppressCommandLineHeader: Boolean = config("suppressCommandLineHeader", default = false)

  /** Use the INFO content of the record with the highest AC */
  @Argument(fullName = "mergeInfoWithMaxAC", shortName = "mergeInfoWithMaxAC", doc = "Use the INFO content of the record with the highest AC", required = false, exclusiveOf = "", validation = "")
  var mergeInfoWithMaxAC: Boolean = config("mergeInfoWithMaxAC", default = false)

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
    deps ++= variant.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
  }

  override def cmdLine: String = super.cmdLine +
    repeat("-V", variant, formatPrefix = TaggedFile.formatCommandLineParameter) +
    optional("-o", out) +
    optional("-genotypeMergeOptions", genotypemergeoption) +
    optional("-filteredRecordsMergeType", filteredrecordsmergetype) +
    optional("-multipleAllelesMergeType", multipleallelesmergetype) +
    optional("-priority", rod_priority_list) +
    conditional(printComplexMerges, "-printComplexMerges") +
    conditional(filteredAreUncalled, "-filteredAreUncalled") +
    conditional(minimalVCF, "-minimalVCF") +
    conditional(excludeNonVariants, "-env") +
    optional("-setKey", setKey) +
    conditional(assumeIdenticalSamples, "-assumeIdenticalSamples") +
    optional("-minN", minimumN) +
    conditional(suppressCommandLineHeader, "-suppressCommandLineHeader") +
    conditional(mergeInfoWithMaxAC, "-mergeInfoWithMaxAC") +
    conditional(filter_reads_with_N_cigar, "-filterRNC") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ") +
    conditional(filter_bases_not_stored, "-filterNoBases")
}

object CombineVariants {
  def apply(root: Configurable, input: List[File], output: File): CombineVariants = {
    val cv = new CombineVariants(root)
    cv.variant = input
    cv.out = output
    cv
  }
}
