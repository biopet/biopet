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
import org.broadinstitute.gatk.utils.commandline.{ Gather, Input, Output, _ }

class CombineGVCFs(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "CombineGVCFs"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** One or more specific annotations to recompute.  The single value 'none' removes the default annotations */
  @Argument(fullName = "annotation", shortName = "A", doc = "One or more specific annotations to recompute.  The single value 'none' removes the default annotations", required = false, exclusiveOf = "", validation = "")
  var annotation: List[String] = config("annotation", default = Nil, freeVar = false)

  /** One or more classes/groups of annotations to apply to variant calls */
  @Argument(fullName = "group", shortName = "G", doc = "One or more classes/groups of annotations to apply to variant calls", required = false, exclusiveOf = "", validation = "")
  var group: List[String] = config("group", default = Nil)

  /** dbSNP file */
  @Input(fullName = "dbsnp", shortName = "D", doc = "dbSNP file", required = false, exclusiveOf = "", validation = "")
  var dbsnp: Option[File] = dbsnpVcfFile

  /** One or more input gVCF files */
  @Input(fullName = "variant", shortName = "V", doc = "One or more input gVCF files", required = true, exclusiveOf = "", validation = "")
  var variant: Seq[File] = Nil

  /** File to which the combined gVCF should be written */
  @Output(fullName = "out", shortName = "o", doc = "File to which the combined gVCF should be written", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /** If specified, convert banded gVCFs to all-sites gVCFs */
  @Argument(fullName = "convertToBasePairResolution", shortName = "bpResolution", doc = "If specified, convert banded gVCFs to all-sites gVCFs", required = false, exclusiveOf = "", validation = "")
  var convertToBasePairResolution: Boolean = config("convertToBasePairResolution", default = false)

  /** If > 0, reference bands will be broken up at genomic positions that are multiples of this number */
  @Argument(fullName = "breakBandsAtMultiplesOf", shortName = "breakBandsAtMultiplesOf", doc = "If > 0, reference bands will be broken up at genomic positions that are multiples of this number", required = false, exclusiveOf = "", validation = "")
  var breakBandsAtMultiplesOf: Option[Int] = config("breakBandsAtMultiplesOf")

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
    dbsnp.foreach(deps :+= VcfUtils.getVcfIndexFile(_))
    deps ++= variant.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      outputIndex = VcfUtils.getVcfIndexFile(out)
  }

  override def cmdLine = super.cmdLine +
    repeat("-A", annotation, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-G", group, spaceSeparated = true, escape = true, format = "%s") +
    optional(TaggedFile.formatCommandLineParameter("-D", dbsnp.getOrElse(null)), dbsnp, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-V", variant, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    conditional(convertToBasePairResolution, "-bpResolution", escape = true, format = "%s") +
    optional("-breakBandsAtMultiplesOf", breakBandsAtMultiplesOf, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object CombineGVCFs {
  def apply(root: Configurable, input: List[File], output: File): CombineGVCFs = {
    val cg = new CombineGVCFs(root)
    cg.variant = input
    cg.out = output
    cg
  }
}
