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
import nl.lumc.sasc.biopet.extensions.gatk.gather.CatVariantsGatherer
import nl.lumc.sasc.biopet.extensions.gatk.scatter.{GATKScatterFunction, LocusScatterFunction}
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Output, _}

class VariantAnnotator(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "VariantAnnotator"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** Input VCF file */
  @Input(fullName = "variant", shortName = "V", doc = "Input VCF file", required = true, exclusiveOf = "", validation = "")
  var variant: File = _

  /** SnpEff file from which to get annotations */
  @Input(fullName = "snpEffFile", shortName = "snpEffFile", doc = "SnpEff file from which to get annotations", required = false, exclusiveOf = "", validation = "")
  var snpEffFile: Option[File] = config("snpEffFile")

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: Option[File] = dbsnpVcfFile

  /** Comparison VCF file */
  @Input(fullName = "comp", shortName = "comp", doc = "Comparison VCF file", required = false, exclusiveOf = "", validation = "")
  var comp: List[File] = config("comp", default = Nil)

  /** External resource VCF file */
  @Input(fullName = "resource", shortName = "resource", doc = "External resource VCF file", required = false, exclusiveOf = "", validation = "")
  var resource: List[File] = config("resource")

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /** One or more specific annotations to apply to variant calls */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var annotation: List[String] = config("annotation", default = Nil, freeVar = false)

  /** One or more specific annotations to exclude */
  @Argument(fullName = "excludeAnnotation", shortName = "XA", doc = "One or more specific annotations to exclude", required = false, exclusiveOf = "", validation = "")
  var excludeAnnotation: List[String] = config("excludeAnnotation", default = Nil)

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var group: List[String] = config("group", default = Nil)

  /** One or more specific expressions to apply to variant calls */
  @Argument(fullName = "expression", shortName = "E", doc = "One or more specific expressions to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var expression: List[String] = config("expression", default = Nil)

  /** Check for allele concordances when using an external resource VCF file */
  @Argument(fullName = "resourceAlleleConcordance", shortName = "rac", doc = "Check for allele concordances when using an external resource VCF file", required = false, exclusiveOf = "", validation = "")
  var resourceAlleleConcordance: Boolean = config("resourceAlleleConcordance", default = false)

  /** Use all possible annotations (not for the faint of heart) */
  @Argument(fullName = "useAllAnnotations", shortName = "all", doc = "Use all possible annotations (not for the faint of heart)", required = false, exclusiveOf = "", validation = "")
  var useAllAnnotations: Boolean = config("useAllAnnotations", default = false)

  /** Add dbSNP ID even if one is already present */
  @Argument(fullName = "alwaysAppendDbsnpId", shortName = "alwaysAppendDbsnpId", doc = "Add dbSNP ID even if one is already present", required = false, exclusiveOf = "", validation = "")
  var alwaysAppendDbsnpId: Boolean = config("alwaysAppendDbsnpId", default = false)

  /** GQ threshold for annotating MV ratio */
  @Argument(fullName = "MendelViolationGenotypeQualityThreshold", shortName = "mvq", doc = "GQ threshold for annotating MV ratio", required = false, exclusiveOf = "", validation = "")
  var MendelViolationGenotypeQualityThreshold: Option[Double] = config("MendelViolationGenotypeQualityThreshold")

  /** Format string for MendelViolationGenotypeQualityThreshold */
  @Argument(fullName = "MendelViolationGenotypeQualityThresholdFormat", shortName = "", doc = "Format string for MendelViolationGenotypeQualityThreshold", required = false, exclusiveOf = "", validation = "")
  var MendelViolationGenotypeQualityThresholdFormat: String = "%s"

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
    snpEffFile.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    dbsnp.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    deps ++= comp.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    deps ++= resource.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
  }

  override def cmdLine = super.cmdLine +
    required(TaggedFile.formatCommandLineParameter("-V", variant), variant, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-snpEffFile", snpEffFile.getOrElse()), snpEffFile, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp.getOrElse()), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-comp", comp, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-resource", resource, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-XA", excludeAnnotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-E", expression, spaceSeparated = true, escape = true, format = "%s") +
    conditional(resourceAlleleConcordance, "-rac", escape = true, format = "%s") +
    conditional(useAllAnnotations, "-all", escape = true, format = "%s") +
    conditional(alwaysAppendDbsnpId, "-alwaysAppendDbsnpId", escape = true, format = "%s") +
    optional("-mvq", MendelViolationGenotypeQualityThreshold, spaceSeparated = true, escape = true, format = MendelViolationGenotypeQualityThresholdFormat) +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
