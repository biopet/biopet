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
//class VariantAnnotator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantAnnotator with GatkGeneral {
//  if (config.contains("scattercount")) scatterCount = config("scattercount")
//  dbsnp = config("dbsnp")
//}
//
//object VariantAnnotator {
//  def apply(root: Configurable, input: File, bamFiles: List[File], output: File): VariantAnnotator = {
//    val va = new VariantAnnotator(root)
//    va.variant = input
//    va.input_file = bamFiles
//    va.out = output
//    va
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }

class VariantAnnotator(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  analysisName = "VariantAnnotator"
  analysis_type = "VariantAnnotator"
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

  /** SnpEff file from which to get annotations */
  @Input(fullName = "snpEffFile", shortName = "snpEffFile", doc = "SnpEff file from which to get annotations", required = false, exclusiveOf = "", validation = "")
  var snpEffFile: File = _

  /** Dependencies on the index of snpEffFile */
  @Input(fullName = "snpEffFileIndex", shortName = "", doc = "Dependencies on the index of snpEffFile", required = false, exclusiveOf = "", validation = "")
  private var snpEffFileIndex: Seq[File] = Nil

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

  /** Comparison VCF file */
  @Input(fullName = "comp", shortName = "comp", doc = "Comparison VCF file", required = false, exclusiveOf = "", validation = "")
  var comp: Seq[File] = Nil

  /** Dependencies on any indexes of comp */
  @Input(fullName = "compIndexes", shortName = "", doc = "Dependencies on any indexes of comp", required = false, exclusiveOf = "", validation = "")
  private var compIndexes: Seq[File] = Nil

  /** External resource VCF file */
  @Input(fullName = "resource", shortName = "resource", doc = "External resource VCF file", required = false, exclusiveOf = "", validation = "")
  var resource: Seq[File] = Nil

  /** Dependencies on any indexes of resource */
  @Input(fullName = "resourceIndexes", shortName = "", doc = "Dependencies on any indexes of resource", required = false, exclusiveOf = "", validation = "")
  private var resourceIndexes: Seq[File] = Nil

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

  /** One or more specific annotations to apply to variant calls */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
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

  /** One or more specific annotations to exclude */
  @Argument(fullName = "excludeAnnotation", shortName = "XA", doc = "One or more specific annotations to exclude", required = false, exclusiveOf = "", validation = "")
  var excludeAnnotation: Seq[String] = Nil

  /**
   * Short name of excludeAnnotation
   * @return Short name of excludeAnnotation
   */
  def XA = this.excludeAnnotation

  /**
   * Short name of excludeAnnotation
   * @param value Short name of excludeAnnotation
   */
  def XA_=(value: Seq[String]) { this.excludeAnnotation = value }

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

  /** One or more specific expressions to apply to variant calls */
  @Argument(fullName = "expression", shortName = "E", doc = "One or more specific expressions to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var expression: Seq[String] = Nil

  /**
   * Short name of expression
   * @return Short name of expression
   */
  def E = this.expression

  /**
   * Short name of expression
   * @param value Short name of expression
   */
  def E_=(value: Seq[String]) { this.expression = value }

  /** Check for allele concordances when using an external resource VCF file */
  @Argument(fullName = "resourceAlleleConcordance", shortName = "rac", doc = "Check for allele concordances when using an external resource VCF file", required = false, exclusiveOf = "", validation = "")
  var resourceAlleleConcordance: Boolean = _

  /**
   * Short name of resourceAlleleConcordance
   * @return Short name of resourceAlleleConcordance
   */
  def rac = this.resourceAlleleConcordance

  /**
   * Short name of resourceAlleleConcordance
   * @param value Short name of resourceAlleleConcordance
   */
  def rac_=(value: Boolean) { this.resourceAlleleConcordance = value }

  /** Use all possible annotations (not for the faint of heart) */
  @Argument(fullName = "useAllAnnotations", shortName = "all", doc = "Use all possible annotations (not for the faint of heart)", required = false, exclusiveOf = "", validation = "")
  var useAllAnnotations: Boolean = _

  /**
   * Short name of useAllAnnotations
   * @return Short name of useAllAnnotations
   */
  def all = this.useAllAnnotations

  /**
   * Short name of useAllAnnotations
   * @param value Short name of useAllAnnotations
   */
  def all_=(value: Boolean) { this.useAllAnnotations = value }

  /** List the available annotations and exit */
  @Argument(fullName = "list", shortName = "ls", doc = "List the available annotations and exit", required = false, exclusiveOf = "", validation = "")
  var list: Boolean = _

  /**
   * Short name of list
   * @return Short name of list
   */
  def ls = this.list

  /**
   * Short name of list
   * @param value Short name of list
   */
  def ls_=(value: Boolean) { this.list = value }

  /** Add dbSNP ID even if one is already present */
  @Argument(fullName = "alwaysAppendDbsnpId", shortName = "alwaysAppendDbsnpId", doc = "Add dbSNP ID even if one is already present", required = false, exclusiveOf = "", validation = "")
  var alwaysAppendDbsnpId: Boolean = _

  /** GQ threshold for annotating MV ratio */
  @Argument(fullName = "MendelViolationGenotypeQualityThreshold", shortName = "mvq", doc = "GQ threshold for annotating MV ratio", required = false, exclusiveOf = "", validation = "")
  var MendelViolationGenotypeQualityThreshold: Option[Double] = None

  /**
   * Short name of MendelViolationGenotypeQualityThreshold
   * @return Short name of MendelViolationGenotypeQualityThreshold
   */
  def mvq = this.MendelViolationGenotypeQualityThreshold

  /**
   * Short name of MendelViolationGenotypeQualityThreshold
   * @param value Short name of MendelViolationGenotypeQualityThreshold
   */
  def mvq_=(value: Option[Double]) { this.MendelViolationGenotypeQualityThreshold = value }

  /** Format string for MendelViolationGenotypeQualityThreshold */
  @Argument(fullName = "MendelViolationGenotypeQualityThresholdFormat", shortName = "", doc = "Format string for MendelViolationGenotypeQualityThreshold", required = false, exclusiveOf = "", validation = "")
  var MendelViolationGenotypeQualityThresholdFormat: String = "%s"

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
    if (snpEffFile != null)
      snpEffFileIndex :+= new File(snpEffFile.getPath + ".idx")
    if (dbsnp != null)
      dbsnpIndex :+= new File(dbsnp.getPath + ".idx")
    compIndexes ++= comp.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    resourceIndexes ++= resource.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(out.getPath))
        outIndex = new File(out.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine +
    required(TaggedFile.formatCommandLineParameter("-V", variant), variant, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-snpEffFile", snpEffFile), snpEffFile, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-comp", comp, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-resource", resource, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-XA", excludeAnnotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-E", expression, spaceSeparated = true, escape = true, format = "%s") +
    conditional(resourceAlleleConcordance, "-rac", escape = true, format = "%s") +
    conditional(useAllAnnotations, "-all", escape = true, format = "%s") +
    conditional(list, "-ls", escape = true, format = "%s") +
    conditional(alwaysAppendDbsnpId, "-alwaysAppendDbsnpId", escape = true, format = "%s") +
    optional("-mvq", MendelViolationGenotypeQualityThreshold, spaceSeparated = true, escape = true, format = MendelViolationGenotypeQualityThresholdFormat) +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
