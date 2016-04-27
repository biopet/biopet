/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

//import java.io.File
//
//import nl.lumc.sasc.biopet.utils.config.Configurable
//import org.broadinstitute.gatk.queue.extensions.gatk.{CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile}
//import org.broadinstitute.gatk.queue.function.scattergather.ScatterGatherableFunction
//import org.broadinstitute.gatk.utils.commandline.{Argument, Gather, Input, Output}
//
//class ApplyRecalibration(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.ApplyRecalibration with GatkGeneral {
//  scatterCount = config("scattercount", default = 0)
//
//  override val defaultThreads = 3
//
//  override def freezeFieldValues() {
//    super.freezeFieldValues()
//
//    nt = Option(getThreads)
//    memoryLimit = Option(nt.getOrElse(1) * 2)
//
//    import org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode
//    if (mode == Mode.INDEL) ts_filter_level = config("ts_filter_level", default = 99.0)
//    else if (mode == Mode.SNP) ts_filter_level = config("ts_filter_level", default = 99.5)
//    ts_filter_level = config("ts_filter_level")
//  }
//}
//
//object ApplyRecalibration {
//  def apply(root: Configurable, input: File, output: File, recal_file: File, tranches_file: File, indel: Boolean = false): ApplyRecalibration = {
//    val ar = if (indel) new ApplyRecalibration(root) {
//      mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
//    }
//    else new ApplyRecalibration(root) {
//      mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
//    }
//    ar.input :+= input
//    ar.recal_file = recal_file
//    ar.tranches_file = tranches_file
//    ar.out = output
//    ar
//  }
//}

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ CatVariantsGatherer, GATKScatterFunction, LocusScatterFunction, TaggedFile }
import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline.Argument
import org.broadinstitute.gatk.utils.commandline.Gather
import org.broadinstitute.gatk.utils.commandline.Input
import org.broadinstitute.gatk.utils.commandline.Output

class ApplyRecalibration(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "ApplyRecalibration"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** The raw input variants to be recalibrated */
  @Input(fullName = "input", shortName = "input", doc = "The raw input variants to be recalibrated", required = true, exclusiveOf = "", validation = "")
  var input: Seq[File] = Nil

  /** Dependencies on any indexes of input */
  @Input(fullName = "inputIndexes", shortName = "", doc = "Dependencies on any indexes of input", required = false, exclusiveOf = "", validation = "")
  private var inputIndexes: Seq[File] = Nil

  /** The input recal file used by ApplyRecalibration */
  @Input(fullName = "recal_file", shortName = "recalFile", doc = "The input recal file used by ApplyRecalibration", required = true, exclusiveOf = "", validation = "")
  var recal_file: File = _

  /** Dependencies on the index of recal_file */
  @Input(fullName = "recal_fileIndex", shortName = "", doc = "Dependencies on the index of recal_file", required = false, exclusiveOf = "", validation = "")
  private var recal_fileIndex: Seq[File] = Nil

  /** The input tranches file describing where to cut the data */
  @Input(fullName = "tranches_file", shortName = "tranchesFile", doc = "The input tranches file describing where to cut the data", required = false, exclusiveOf = "", validation = "")
  var tranches_file: File = _

  /** The output filtered and recalibrated VCF file in which each variant is annotated with its VQSLOD value */
  @Output(fullName = "out", shortName = "o", doc = "The output filtered and recalibrated VCF file in which each variant is annotated with its VQSLOD value", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[CatVariantsGatherer])
  var out: File = _

  /** Automatically generated index for out */
  @Output(fullName = "outIndex", shortName = "", doc = "Automatically generated index for out", required = false, exclusiveOf = "", validation = "")
  @Gather(enabled = false)
  private var outIndex: File = _

  /** The truth sensitivity level at which to start filtering */
  @Argument(fullName = "ts_filter_level", shortName = "ts_filter_level", doc = "The truth sensitivity level at which to start filtering", required = false, exclusiveOf = "", validation = "")
  var ts_filter_level: Option[Double] = None

  /** Format string for ts_filter_level */
  @Argument(fullName = "ts_filter_levelFormat", shortName = "", doc = "Format string for ts_filter_level", required = false, exclusiveOf = "", validation = "")
  var ts_filter_levelFormat: String = "%s"

  /** The VQSLOD score below which to start filtering */
  @Argument(fullName = "lodCutoff", shortName = "lodCutoff", doc = "The VQSLOD score below which to start filtering", required = false, exclusiveOf = "", validation = "")
  var lodCutoff: Option[Double] = None

  /** Format string for lodCutoff */
  @Argument(fullName = "lodCutoffFormat", shortName = "", doc = "Format string for lodCutoff", required = false, exclusiveOf = "", validation = "")
  var lodCutoffFormat: String = "%s"

  /** If specified, the recalibration will be applied to variants marked as filtered by the specified filter name in the input VCF file */
  @Argument(fullName = "ignore_filter", shortName = "ignoreFilter", doc = "If specified, the recalibration will be applied to variants marked as filtered by the specified filter name in the input VCF file", required = false, exclusiveOf = "", validation = "")
  var ignore_filter: Seq[String] = Nil

  /** If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file. */
  @Argument(fullName = "ignore_all_filters", shortName = "ignoreAllFilters", doc = "If specified, the variant recalibrator will ignore all input filters. Useful to rerun the VQSR from a filtered output file.", required = false, exclusiveOf = "", validation = "")
  var ignore_all_filters: Boolean = _

  /** Don't output filtered loci after applying the recalibration */
  @Argument(fullName = "excludeFiltered", shortName = "ef", doc = "Don't output filtered loci after applying the recalibration", required = false, exclusiveOf = "", validation = "")
  var excludeFiltered: Boolean = _

  /** Recalibration mode to employ: 1.) SNP for recalibrating only SNPs (emitting indels untouched in the output VCF); 2.) INDEL for indels; and 3.) BOTH for recalibrating both SNPs and indels simultaneously. */
  @Argument(fullName = "mode", shortName = "mode", doc = "Recalibration mode to employ: 1.) SNP for recalibrating only SNPs (emitting indels untouched in the output VCF); 2.) INDEL for indels; and 3.) BOTH for recalibrating both SNPs and indels simultaneously.", required = false, exclusiveOf = "", validation = "")
  var mode: String = _

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
    inputIndexes ++= input.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => new File(orig.getPath + ".idx"))
    if (recal_file != null)
      recal_fileIndex :+= new File(recal_file.getPath + ".idx")
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!org.broadinstitute.gatk.utils.commandline.ArgumentTypeDescriptor.isCompressed(out.getPath))
        outIndex = new File(out.getPath + ".idx")
  }

  override def cmdLine = super.cmdLine +
    repeat("-input", input, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    required(TaggedFile.formatCommandLineParameter("-recalFile", recal_file), recal_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-tranchesFile", tranches_file, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    optional("-ts_filter_level", ts_filter_level, spaceSeparated = true, escape = true, format = ts_filter_levelFormat) +
    optional("-lodCutoff", lodCutoff, spaceSeparated = true, escape = true, format = lodCutoffFormat) +
    repeat("-ignoreFilter", ignore_filter, spaceSeparated = true, escape = true, format = "%s") + conditional(ignore_all_filters, "-ignoreAllFilters", escape = true, format = "%s") +
    conditional(excludeFiltered, "-ef", escape = true, format = "%s") +
    optional("-mode", mode, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
