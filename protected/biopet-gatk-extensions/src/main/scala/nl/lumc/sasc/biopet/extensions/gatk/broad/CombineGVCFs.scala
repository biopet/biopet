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
//class CombineGVCFs(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.CombineGVCFs with GatkGeneral {
//  if (config.contains("scattercount")) scatterCount = config("scattercount")
//}
//
//object CombineGVCFs {
//  def apply(root: Configurable, input: List[File], output: File): CombineGVCFs = {
//    val cg = new CombineGVCFs(root)
//    cg.variant = input
//    cg.o = output
//    cg
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.{ Gather, Input, Output, _ }

class CombineGVCFs(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "CombineGVCFs"
  analysis_type = "CombineGVCFs"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

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

  /** File to which the combined gVCF should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which the combined gVCF should be written", required = false, exclusiveOf = "", validation = "")
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

  /** If specified, convert banded gVCFs to all-sites gVCFs */
  @Argument(fullName = "convertToBasePairResolution", shortName = "bpResolution", doc = "If specified, convert banded gVCFs to all-sites gVCFs", required = false, exclusiveOf = "", validation = "")
  var convertToBasePairResolution: Boolean = _

  /**
   * Short name of convertToBasePairResolution
   * @return Short name of convertToBasePairResolution
   */
  def bpResolution = this.convertToBasePairResolution

  /**
   * Short name of convertToBasePairResolution
   * @param value Short name of convertToBasePairResolution
   */
  def bpResolution_=(value: Boolean) { this.convertToBasePairResolution = value }

  /** If > 0, reference bands will be broken up at genomic positions that are multiples of this number */
  @Argument(fullName = "breakBandsAtMultiplesOf", shortName = "breakBandsAtMultiplesOf", doc = "If > 0, reference bands will be broken up at genomic positions that are multiples of this number", required = false, exclusiveOf = "", validation = "")
  var breakBandsAtMultiplesOf: Option[Int] = None

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
    if (dbsnp != null)
      dbsnpIndex :+= new File(dbsnp.getPath + ".idx")
    variantIndexes ++= variant.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(out.getPath))
        outIndex = new File(out.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-V", variant, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    conditional(convertToBasePairResolution, "-bpResolution", escape = true, format = "%s") +
    optional("-breakBandsAtMultiplesOf", breakBandsAtMultiplesOf, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
