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
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Output, _ }
import org.broadinstitute.gatk.utils.report.{ GATKReport, GATKReportTable }

class GenotypeConcordance(val root: Configurable) extends CommandLineGATK with ScatterGatherableFunction with Summarizable {
  analysisName = "GenotypeConcordance"
  val analysis_type = "GenotypeConcordance"
  scatterClass = classOf[LocusScatterFunction]
  setupScatterFunction = { case scatter: GATKScatterFunction => scatter.includeUnmapped = false }

  /** The variants and genotypes to evaluate */
  @Input(fullName = "eval", shortName = "eval", doc = "The variants and genotypes to evaluate", required = true, exclusiveOf = "", validation = "")
  var eval: File = _

  /** The variants and genotypes to compare against */
  @Input(fullName = "comp", shortName = "comp", doc = "The variants and genotypes to compare against", required = true, exclusiveOf = "", validation = "")
  var comp: File = _

  /** Filters will be ignored */
  @Argument(fullName = "ignoreFilters", shortName = "", doc = "Filters will be ignored", required = false, exclusiveOf = "", validation = "")
  var ignoreFilters: Boolean = config("ignoreFilters", default = false)

  /** One or more criteria to use to set EVAL genotypes to no-call. These genotype-level filters are only applied to the EVAL rod. */
  @Argument(fullName = "genotypeFilterExpressionEval", shortName = "gfe", doc = "One or more criteria to use to set EVAL genotypes to no-call. These genotype-level filters are only applied to the EVAL rod.", required = false, exclusiveOf = "", validation = "")
  var genotypeFilterExpressionEval: List[String] = config("genotypeFilterExpressionEval", default = Nil)

  /** One or more criteria to use to set COMP genotypes to no-call. These genotype-level filters are only applied to the COMP rod. */
  @Argument(fullName = "genotypeFilterExpressionComp", shortName = "gfc", doc = "One or more criteria to use to set COMP genotypes to no-call. These genotype-level filters are only applied to the COMP rod.", required = false, exclusiveOf = "", validation = "")
  var genotypeFilterExpressionComp: Seq[String] = config("genotypeFilterExpressionComp", default = Nil)

  /** Molten rather than tabular output */
  @Argument(fullName = "moltenize", shortName = "moltenize", doc = "Molten rather than tabular output", required = false, exclusiveOf = "", validation = "")
  var moltenize: Boolean = config("moltenize", default = true)

  /** File to output the discordant sites and genotypes. */
  @Output(fullName = "printInterestingSites", shortName = "sites", doc = "File to output the discordant sites and genotypes.", required = false, exclusiveOf = "", validation = "")
  var printInterestingSites: Option[File] = None

  /** An output file created by the walker.  Will overwrite contents if file exists */
  @Output(fullName = "out", shortName = "o", doc = "An output file created by the walker.  Will overwrite contents if file exists", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var out: File = _

  /** Filter out reads with CIGAR containing the N operator, instead of failing with an error */
  @Argument(fullName = "filter_reads_with_N_cigar", shortName = "filterRNC", doc = "Filter out reads with CIGAR containing the N operator, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_reads_with_N_cigar: Boolean = config("filter_reads_with_N_cigar", default = false)

  /** Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error */
  @Argument(fullName = "filter_mismatching_base_and_quals", shortName = "filterMBQ", doc = "Filter out reads with mismatching numbers of bases and base qualities, instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_mismatching_base_and_quals: Boolean = config("filter_mismatching_base_and_quals", default = false)

  /** Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error */
  @Argument(fullName = "filter_bases_not_stored", shortName = "filterNoBases", doc = "Filter out reads with no stored bases (i.e. '*' where the sequence should be), instead of failing with an error", required = false, exclusiveOf = "", validation = "")
  var filter_bases_not_stored: Boolean = config("filter_bases_not_stored", default = false)

  def summaryFiles = Map("output" -> out)

  def summaryStats = {
    val report = new GATKReport(out)
    val compProportions = report.getTable("GenotypeConcordance_CompProportions")
    val counts = report.getTable("GenotypeConcordance_Counts")
    val evalProportions = report.getTable("GenotypeConcordance_EvalProportions")
    val genotypeSummary = report.getTable("GenotypeConcordance_Summary")
    val siteSummary = report.getTable("SiteConcordance_Summary")

    val samples = for (i <- 0 until genotypeSummary.getNumRows) yield genotypeSummary.get(i, "Sample").toString

    def getMap(table: GATKReportTable, column: String) = samples.distinct.map(sample => sample -> {
      (for (i <- 0 until table.getNumRows if table.get(i, "Sample") == sample) yield s"${table.get(i, "Eval_Genotype")}__${table.get(i, "Comp_Genotype")}" -> table.get(i, column)).toMap
    }).toMap

    Map(
      "compProportions" -> getMap(compProportions, "Proportion"),
      "counts" -> getMap(counts, "Count"),
      "evalProportions" -> getMap(evalProportions, "Proportion"),
      "genotypeSummary" -> samples.distinct.map(sample => {
        val i = samples.indexOf(sample)
        sample -> Map(
          "Non-Reference_Discrepancy" -> genotypeSummary.get(i, "Non-Reference_Discrepancy"),
          "Non-Reference_Sensitivity" -> genotypeSummary.get(i, "Non-Reference_Sensitivity"),
          "Overall_Genotype_Concordance" -> genotypeSummary.get(i, "Overall_Genotype_Concordance")
        )
      }).toMap,
      "siteSummary" -> Map(
        "ALLELES_MATCH" -> siteSummary.get(0, "ALLELES_MATCH"),
        "EVAL_SUPERSET_TRUTH" -> siteSummary.get(0, "EVAL_SUPERSET_TRUTH"),
        "EVAL_SUBSET_TRUTH" -> siteSummary.get(0, "EVAL_SUBSET_TRUTH"),
        "ALLELES_DO_NOT_MATCH" -> siteSummary.get(0, "ALLELES_DO_NOT_MATCH"),
        "EVAL_ONLY" -> siteSummary.get(0, "EVAL_ONLY"),
        "TRUTH_ONLY" -> siteSummary.get(0, "TRUTH_ONLY")
      )
    )
  }

  override def beforeGraph() {
    super.beforeGraph()
    if (eval != null) deps :+= VcfUtils.getVcfIndexFile(eval)
    if (comp != null) deps :+= VcfUtils.getVcfIndexFile(comp)
  }

  override def cmdLine = super.cmdLine +
    required(TaggedFile.formatCommandLineParameter("-eval", eval), eval, spaceSeparated = true, escape = true, format = "%s") +
    required(TaggedFile.formatCommandLineParameter("-comp", comp), comp, spaceSeparated = true, escape = true, format = "%s") +
    conditional(ignoreFilters, "--ignoreFilters", escape = true, format = "%s") +
    repeat("-gfe", genotypeFilterExpressionEval, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-gfc", genotypeFilterExpressionComp, spaceSeparated = true, escape = true, format = "%s") +
    conditional(moltenize, "-moltenize", escape = true, format = "%s") +
    optional("-sites", printInterestingSites, spaceSeparated = true, escape = true, format = "%s") +
    optional("-o", out, spaceSeparated = true, escape = true, format = "%s") +
    conditional(filter_reads_with_N_cigar, "-filterRNC", escape = true, format = "%s") +
    conditional(filter_mismatching_base_and_quals, "-filterMBQ", escape = true, format = "%s") +
    conditional(filter_bases_not_stored, "-filterNoBases", escape = true, format = "%s")
}
