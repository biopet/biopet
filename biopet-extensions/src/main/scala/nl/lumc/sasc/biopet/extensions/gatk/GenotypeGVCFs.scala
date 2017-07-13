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

class GenotypeGVCFs(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "GenotypeGVCFs"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** One or more input gVCF files */
  @Input(fullName = "variant", shortName = "V", doc = "One or more input gVCF files", required = true, exclusiveOf = "", validation = "")
  var variant: Seq[File] = Nil

  /** File to which variants should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which variants should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[GatherVcfs])
  var out: File = _

  /** Include loci found to be non-variant after genotyping */
  @Argument(fullName = "includeNonVariantSites", shortName = "allSites", doc = "Include loci found to be non-variant after genotyping", required = false, exclusiveOf = "", validation = "")
  var includeNonVariantSites: Boolean = config("includeNonVariantSites", default = false)

  /** Assume duplicate samples are present and uniquify all names with '.variant' and file number index */
  @Argument(fullName = "uniquifySamples", shortName = "uniquifySamples", doc = "Assume duplicate samples are present and uniquify all names with '.variant' and file number index", required = false, exclusiveOf = "", validation = "")
  var uniquifySamples: Boolean = config("uniquifySamples", default = false)

  /** If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site */
  @Argument(fullName = "annotateNDA", shortName = "nda", doc = "If provided, we will annotate records with the number of alternate alleles that were discovered (but not necessarily genotyped) at a given site", required = false, exclusiveOf = "", validation = "")
  var annotateNDA: Boolean = config("annotateNDA", default = false)

  /** Heterozygosity value used to compute prior likelihoods for any locus */
  @Argument(fullName = "heterozygosity", shortName = "hets", doc = "Heterozygosity value used to compute prior likelihoods for any locus", required = false, exclusiveOf = "", validation = "")
  var heterozygosity: Option[Double] = config("heterozygosity")

  /** Format string for heterozygosity */
  @Argument(fullName = "heterozygosityFormat", shortName = "", doc = "Format string for heterozygosity", required = false, exclusiveOf = "", validation = "")
  var heterozygosityFormat: String = "%s"

  /** Heterozygosity for indel calling */
  @Argument(fullName = "indel_heterozygosity", shortName = "indelHeterozygosity", doc = "Heterozygosity for indel calling", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosity: Option[Double] = config("indel_heterozygosity")

  /** Format string for indel_heterozygosity */
  @Argument(fullName = "indel_heterozygosityFormat", shortName = "", doc = "Format string for indel_heterozygosity", required = false, exclusiveOf = "", validation = "")
  var indel_heterozygosityFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be called */
  @Argument(fullName = "standard_min_confidence_threshold_for_calling", shortName = "stand_call_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be called", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_calling: Option[Double] = config("stand_call_conf")

  /** Format string for standard_min_confidence_threshold_for_calling */
  @Argument(fullName = "standard_min_confidence_threshold_for_callingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_calling", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_callingFormat: String = "%s"

  /** The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold) */
  @Argument(fullName = "standard_min_confidence_threshold_for_emitting", shortName = "stand_emit_conf", doc = "The minimum phred-scaled confidence threshold at which variants should be emitted (and filtered with LowQual if less than the calling threshold)", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emitting: Option[Double] = config("stand_emit_conf")

  /** Format string for standard_min_confidence_threshold_for_emitting */
  @Argument(fullName = "standard_min_confidence_threshold_for_emittingFormat", shortName = "", doc = "Format string for standard_min_confidence_threshold_for_emitting", required = false, exclusiveOf = "", validation = "")
  var standard_min_confidence_threshold_for_emittingFormat: String = "%s"

  /** Maximum number of alternate alleles to genotype */
  @Argument(fullName = "max_alternate_alleles", shortName = "maxAltAlleles", doc = "Maximum number of alternate alleles to genotype", required = false, exclusiveOf = "", validation = "")
  var max_alternate_alleles: Option[Int] = config("max_alternate_alleles")

  /** Input prior for calls */
  @Argument(fullName = "input_prior", shortName = "inputPrior", doc = "Input prior for calls", required = false, exclusiveOf = "", validation = "")
  var input_prior: List[Double] = config("input_prior", default = Nil)

  /** Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy). */
  @Argument(fullName = "sample_ploidy", shortName = "ploidy", doc = "Ploidy (number of chromosomes) per sample. For pooled data, set to (Number of samples in each pool * Sample Ploidy).", required = false, exclusiveOf = "", validation = "")
  var sample_ploidy: Option[Int] = config("sample_ploidy")

  /** One or more specific annotations to recompute.  The single value 'none' removes the default annotations */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to recompute.  The single value 'none' removes the default annotations", required = false, exclusiveOf = "", validation = "")
  var annotation: List[String] = config("annotation", default = Nil, freeVar = false)

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var group: List[String] = config("group", default = Nil)

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: Option[File] = dbsnpVcfFile

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  var useNewAFCalculator: Boolean = config("useNewAFCalculator", default = false)

  @Output
  @Gather(enabled = false)
  private var outputIndex: File = _

  override def beforeGraph() {
    super.beforeGraph()
    deps ++= variant.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
    dbsnp.foreach(x => deps :+= VcfUtils.getVcfIndexFile(x))
  }

  override def cmdLine = super.cmdLine +
    repeat("-V", variant, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    conditional(includeNonVariantSites, "-allSites", escape = true, format = "%s") +
    conditional(uniquifySamples, "-uniquifySamples", escape = true, format = "%s") +
    conditional(annotateNDA, "-nda", escape = true, format = "%s") +
    conditional(useNewAFCalculator, "--useNewAFCalculator") +
    optional("-hets", heterozygosity, spaceSeparated = true, escape = true, format = heterozygosityFormat) +
    optional("-indelHeterozygosity", indel_heterozygosity, spaceSeparated = true, escape = true, format = indel_heterozygosityFormat) +
    optional("-stand_call_conf", standard_min_confidence_threshold_for_calling, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_callingFormat) +
    optional("-maxAltAlleles", max_alternate_alleles, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-inputPrior", input_prior, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ploidy", sample_ploidy, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp.getOrElse(null)), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s") +
    (this.getVersion match {
      case Some(s) if s.contains("3.0") | s.contains("3.1") | s.contains("3.2") | s.contains("3.3") | s.contains("3.4") | s.contains("3.5") | s.contains("3.6") =>
        optional("-stand_emit_conf", standard_min_confidence_threshold_for_emitting, spaceSeparated = true, escape = true, format = standard_min_confidence_threshold_for_emittingFormat)
      case _ => ""
    })
}

object GenotypeGVCFs {
  def apply(root: Configurable, gvcfFiles: List[File], output: File): GenotypeGVCFs = {
    val gg = new GenotypeGVCFs(root)
    gg.variant = gvcfFiles
    gg.out = output
    gg
  }
}
