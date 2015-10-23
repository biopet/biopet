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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import org.broadinstitute.gatk.utils.report.{ GATKReportTable, GATKReport }

/**
 * Extension for CombineVariants from GATK
 *
 * Created by pjvan_thof on 2/26/15.
 */
class GenotypeConcordance(val root: Configurable) extends Gatk with Summarizable {
  val analysisType = "GenotypeConcordance"

  @Input(required = true)
  var evalFile: File = null

  @Input(required = true)
  var compFile: File = null

  @Output(required = true)
  var outputFile: File = null

  var moltenize = true

  def summaryFiles = Map("output" -> outputFile)

  def summaryStats = {
    val report = new GATKReport(outputFile)
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

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    deps :::= (evalFile :: compFile :: Nil).filter(_.getName.endsWith("vcf.gz")).map(x => new File(x.getAbsolutePath + ".tbi"))
    deps = deps.distinct
  }

  override def cmdLine = super.cmdLine +
    required("--eval", evalFile) +
    required("--comp", compFile) +
    required("-o", outputFile) +
    conditional(moltenize, "--moltenize")
}
