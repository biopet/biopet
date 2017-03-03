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
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingReportTrait
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.{ Summary, SummaryValue }

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._
import Scalaz._

/**
 * With this extension the report is executed within a pipeline
 *
 * Created by pjvan_thof on 3/30/15.
 */
class ShivaReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = ShivaReport
}

/** Object for report generation for Shiva pipeline */
object ShivaReport extends ShivaReportTrait

/** Trait for report generation for Shiva pipeline, this can be extended */
trait ShivaReportTrait extends MultisampleMappingReportTrait {

  def variantcallingExecuted = summary.getSettingKeys(runId, "shiva".right, None, keyValues = Map("multisample_variantcalling" -> List("multisample_variantcalling"))).get("multisample_variantcalling")
    .flatten match {
      case Some(true) => true
      case _          => false
    }

  override def frontSection = ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/shivaFront.ssp")

  override def pipelineName = "shiva"

  override def additionalSections = super.additionalSections ++ (if (variantcallingExecuted) List("Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp",
    Map("showPlot" -> true, "showTable" -> false)))
  else Nil)

  /** Root page for the shiva report */
  override def indexPage = {
    val regions = regionsPage
    val oldPage = super.indexPage

    oldPage.copy(subPages = oldPage.subPages ++ regionsPage)
  }

  /** Generate a page with all target coverage stats */
  def regionsPage: Option[(String, ReportPage)] = {
    val roi = summary.getSetting(runId, "shiva".right).get("regions_of_interest")
    val amplicon = summary.getSetting(runId, "shiva".right).get("amplicon_bed")

    var regionPages: Map[String, ReportPage] = Map()

    def createPage(name: String, amplicon: Boolean = false): ReportPage = {
      ReportPage(
        List(),
        List("Coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsMultiTable.ssp")),
        Map("target" -> name)
      )
    }

    amplicon match {
      case Some(x: String) => regionPages += (x + " (Amplicon)") -> createPage(x, amplicon = true)
      case _               =>
    }

    roi match {
      case Some(x: String)  => regionPages += x -> createPage(x, amplicon = false)
      case Some(x: List[_]) => x.foreach(x => regionPages += x.toString -> createPage(x.toString, amplicon = false))
      case _                =>
    }

    if (regionPages.nonEmpty) Some("Regions" -> ReportPage(
      regionPages.map(p => p._1 -> ReportPage(Nil,
        List(
          "Variants" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp", Map("showPlot" -> true)),
          "Coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsMultiTable.ssp")
        ),
        Map("target" -> Some(p._1.stripSuffix(" (Amplicon)")))
      )).toList.sortBy(_._1),
      List(),
      Map())
    )
    else None
  }

  /** Files page, can be used general or at sample level */
  override def filesPage(sampleId: Option[Int] = None, libraryId: Option[Int] = None): ReportPage = {
    val vcfFilesSection = if (variantcallingExecuted) List("VCF files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/outputVcfFiles.ssp",
      Map("sampleId" -> sampleId)))
    else Nil
    val oldPage = super.filesPage(sampleId, libraryId)
    oldPage.copy(sections = oldPage.sections ++ vcfFilesSection)
  }

  /** Single sample page */
  override def samplePage(sampleId: Int, args: Map[String, Any]): ReportPage = {
    val variantcallingSection = if (variantcallingExecuted) List("Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp")) else Nil
    val oldPage = super.samplePage(sampleId, args)
    oldPage.copy(sections = variantcallingSection ++ oldPage.sections)
  }

  /** Name of the report */
  def reportName = "Shiva Report"

  /**
   * Generate a stackbar plot for found variants
   *
   * @param outputDir OutputDir for the tsv and png file
   * @param prefix Prefix of the tsv and png file
   * @param summary Summary class
   * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
   */
  def variantSummaryPlot(outputDir: File,
                         prefix: String,
                         summary: SummaryDb,
                         sampleId: Option[Int] = None,
                         caller: String = "final",
                         target: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.print("Sample")
    val field = List("HomVar", "Het", "HomRef", "NoCall")
    tsvWriter.println(s"\t${field.mkString("\t")}")

    val samples = Await.result(summary.getSamples(runId = runId, sampleId = sampleId), Duration.Inf)
    val statsPaths = {
      (for (sample <- Await.result(summary.getSamples(runId = runId), Duration.Inf)) yield {
        field.map(f => s"${sample.name};HomVar" -> List("total", "genotype", "general", sample.name, f)).toMap
      }).fold(Map())(_ ++ _)
    }

    val moduleName = target match {
      case Some(t) => s"multisample-vcfstats-$caller-$t"
      case _       => s"multisample-vcfstats-$caller"
    }

    val results = summary.getStatKeys(runId, "shivavariantcalling".right, Some(moduleName.right), sampleId.map(_.left), keyValues = statsPaths)

    for (sample <- samples if sampleId.isEmpty || sample.id == sampleId.get) {
      tsvWriter.println(sample.name + "\t" + field.map(f => results(s"${sample.name};$f").getOrElse("")).mkString("\t"))
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("VCF records")
    plot.width = Some(200 + (samples.count(s => sampleId.getOrElse(s) == s) * 10))
    plot.runLocal()
  }
}
