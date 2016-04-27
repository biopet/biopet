/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.{ BamGatherFunction, GATKScatterFunction, ReadScatterFunction }
import nl.lumc.sasc.biopet.core.ScatterGatherableFunction
import org.broadinstitute.gatk.utils.commandline._

class PrintReads(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "PrintReads"
  scatterClass = classOf[ReadScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = true }

  /** Write output to this BAM filename instead of STDOUT */
  @Output(fullName = "out", shortName = "o", doc = "Write output to this BAM filename instead of STDOUT", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[BamGatherFunction])
  var out: File = _

  /** Exclude all reads with this read group from the output */
  @Argument(fullName = "readGroup", shortName = "readGroup", doc = "Exclude all reads with this read group from the output", required = false, exclusiveOf = "", validation = "")
  var readGroup: Option[String] = config("readGroup", default = false)

  /** Exclude all reads with this platform from the output */
  @Argument(fullName = "platform", shortName = "platform", doc = "Exclude all reads with this platform from the output", required = false, exclusiveOf = "", validation = "")
  var platform: Option[String] = config("platform")

  /** Print the first n reads from the file, discarding the rest */
  @Argument(fullName = "number", shortName = "n", doc = "Print the first n reads from the file, discarding the rest", required = false, exclusiveOf = "", validation = "")
  var number: Option[Int] = config("number")

  /** File containing a list of samples (one per line). Can be specified multiple times */
  @Argument(fullName = "sample_file", shortName = "sf", doc = "File containing a list of samples (one per line). Can be specified multiple times", required = false, exclusiveOf = "", validation = "")
  var sample_file: List[File] = config("sample_file", default = Nil)

  /** Sample name to be included in the analysis. Can be specified multiple times. */
  @Argument(fullName = "sample_name", shortName = "sn", doc = "Sample name to be included in the analysis. Can be specified multiple times.", required = false, exclusiveOf = "", validation = "")
  var sample_name: List[String] = config("sample_name", default = Nil)

  /** Simplify all reads */
  @Argument(fullName = "simplify", shortName = "s", doc = "Simplify all reads", required = false, exclusiveOf = "", validation = "")
  var simplify: Boolean = config("simplify", default = false)

  /** Don't output a program tag */
  @Argument(fullName = "no_pg_tag", shortName = "npt", doc = "Don't output a program tag", required = false, exclusiveOf = "", validation = "")
  var no_pg_tag: Boolean = config("no_pg_tag", default = false)

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  override def freezeFieldValues() {
    super.freezeFieldValues()
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (!disable_bam_indexing)
        outputFiles :+= new File(out.getPath.stripSuffix(".bam") + ".bai")
    if (out != null && !org.broadinstitute.gatk.utils.io.IOUtils.isSpecialFile(out))
      if (generate_md5)
        outputFiles :+= new File(out.getPath + ".md5")
  }

  override def cmdLine = super.cmdLine +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    optional("-readGroup", readGroup, spaceSeparated = true, escape = true, format = "%s") +
    optional("-platform", platform, spaceSeparated = true, escape = true, format = "%s") +
    optional("-n", number, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-sf", sample_file, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-sn", sample_name, spaceSeparated = true, escape = true, format = "%s") +
    conditional(simplify, "-s", escape = true, format = "%s") +
    conditional(no_pg_tag, "-npt", escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object PrintReads {
  def apply(root: Configurable, input: File, output: File): PrintReads = {
    val br = new PrintReads(root)
    br.input_file :+= input
    br.out = output
    br
  }
}
