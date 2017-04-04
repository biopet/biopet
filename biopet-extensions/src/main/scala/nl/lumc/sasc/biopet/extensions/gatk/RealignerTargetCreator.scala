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
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Input, _ }

class RealignerTargetCreator(val parent: Configurable) extends CommandLineGATK with ScatterGatherableFunction {
  def analysis_type = "RealignerTargetCreator"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** An output file created by the walker.  Will overwrite contents if file exists */
  @Output(fullName = "out", shortName = "o", doc = "An output file created by the walker.  Will overwrite contents if file exists", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var out: File = _

  /** Input VCF file with known indels */
  @Input(fullName = "known", shortName = "known", doc = "Input VCF file with known indels", required = false, exclusiveOf = "", validation = "")
  var known: List[File] = config("known", default = Nil)

  /** window size for calculating entropy or SNP clusters */
  @Argument(fullName = "windowSize", shortName = "window", doc = "window size for calculating entropy or SNP clusters", required = false, exclusiveOf = "", validation = "")
  var windowSize: Option[Int] = config("windowSize")

  /** fraction of base qualities needing to mismatch for a position to have high entropy */
  @Argument(fullName = "mismatchFraction", shortName = "mismatch", doc = "fraction of base qualities needing to mismatch for a position to have high entropy", required = false, exclusiveOf = "", validation = "")
  var mismatchFraction: Option[Double] = config("mismatchFraction")

  /** Format string for mismatchFraction */
  @Argument(fullName = "mismatchFractionFormat", shortName = "", doc = "Format string for mismatchFraction", required = false, exclusiveOf = "", validation = "")
  var mismatchFractionFormat: String = "%s"

  /** minimum reads at a locus to enable using the entropy calculation */
  @Argument(fullName = "minReadsAtLocus", shortName = "minReads", doc = "minimum reads at a locus to enable using the entropy calculation", required = false, exclusiveOf = "", validation = "")
  var minReadsAtLocus: Option[Int] = config("minReadsAtLocus")

  /** maximum interval size; any intervals larger than this value will be dropped */
  @Argument(fullName = "maxIntervalSize", shortName = "maxInterval", doc = "maximum interval size; any intervals larger than this value will be dropped", required = false, exclusiveOf = "", validation = "")
  var maxIntervalSize: Option[Int] = config("maxIntervalSize")

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("", default = false)

  dbsnpVcfFile.foreach(known :+= _)

  override def beforeGraph() {
    super.beforeGraph()
    deps ++= known.filter(orig => orig != null && (!orig.getName.endsWith(".list"))).map(orig => VcfUtils.getVcfIndexFile(orig))
  }

  override def cmdLine = super.cmdLine +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-known", known, formatPrefix = TaggedFile.formatCommandLineParameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-window", windowSize, spaceSeparated = true, escape = true, format = "%s") +
    optional("-mismatch", mismatchFraction, spaceSeparated = true, escape = true, format = mismatchFractionFormat) +
    optional("-minReads", minReadsAtLocus, spaceSeparated = true, escape = true, format = "%s") +
    optional("-maxInterval", maxIntervalSize, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}

object RealignerTargetCreator {
  def apply(root: Configurable, input: File, outputDir: File): RealignerTargetCreator = {
    val re = new RealignerTargetCreator(root)
    re.input_file :+= input
    re.out = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.intervals")
    re
  }
}
