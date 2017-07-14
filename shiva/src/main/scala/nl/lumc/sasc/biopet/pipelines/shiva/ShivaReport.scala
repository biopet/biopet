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

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingReportTrait
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{NoModule, NoSample, SampleId}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

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

  def variantcallingExecuted =
    summary
      .getSettingKeys(runId,
                      "shiva",
                      NoModule,
                      keyValues =
                        Map("multisample_variantcalling" -> List("multisample_variantcalling")))
      .get("multisample_variantcalling")
      .flatten match {
      case Some(true) => true
      case _ => false
    }

  def svCallingExecuted =
    summary
      .getSettingKeys(runId,
                      "shiva",
                      NoModule,
                      keyValues = Map("sv_calling" -> List("sv_calling")))
      .get("sv_calling")
      .flatten match {
      case Some(true) => true
      case _ => false
    }

  override def frontSection = ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/shivaFront.ssp")

  override def pipelineName = "shiva"

  override def additionalSections = {
    val params = Map("showPlot" -> true, "showTable" -> false)
    super.additionalSections ++
      (if (variantcallingExecuted)
         List(
           "SNV Calling" -> ReportSection(
             "/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp",
             params))
       else Nil) ++
      (if (svCallingExecuted)
         List(
           "SV Calling" -> ReportSection(
             "/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariantsSv.ssp",
             params))
       else Nil)
  }

  /** Root page for the shiva report */
  override def indexPage: Future[ReportPage] = Future {
    val oldPage = Await.result(super.indexPage, Duration.Inf)

    oldPage.copy(subPages = oldPage.subPages ++ regionsPage)
  }

  /** Generate a page with all target coverage stats */
  def regionsPage: Option[(String, Future[ReportPage])] = {
    val shivaSettings = Await.result(summary.getSetting(runId, "shiva"), Duration.Inf).get
    val roi = shivaSettings.get("regions_of_interest")
    val amplicon = shivaSettings.get("amplicon_bed")

    var regionPages: Map[String, ReportPage] = Map()

    def createPage(name: String, amplicon: Boolean = false): ReportPage = {
      ReportPage(
        List(),
        List(
          "Coverage" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsMultiTable.ssp")),
        Map("target" -> name)
      )
    }

    amplicon match {
      case Some(x: String) => regionPages += (x + " (Amplicon)") -> createPage(x, amplicon = true)
      case _ =>
    }

    roi match {
      case Some(x: String) => regionPages += x -> createPage(x)
      case Some(x: List[_]) =>
        x.foreach(x => regionPages += x.toString -> createPage(x.toString))
      case _ =>
    }

    if (regionPages.nonEmpty)
      Some(
        "Regions" -> Future(ReportPage(
          regionPages
            .map(p =>
              p._1 -> Future(ReportPage(
                Nil,
                List(
                  "Variants" -> ReportSection(
                    "/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp",
                    Map("showPlot" -> true)),
                  "Coverage" -> ReportSection(
                    "/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsMultiTable.ssp")
                ),
                Map("target" -> Some(p._1.stripSuffix(" (Amplicon)")))
              )))
            .toList
            .sortBy(_._1),
          List(),
          Map()
        )))
    else None
  }

  /** Single sample page */
  override def samplePage(sampleId: Int, args: Map[String, Any]): Future[ReportPage] = Future {
    val variantcallingSection =
      if (variantcallingExecuted)
        List(
          "SNV Calling" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp"))
      else Nil
    val svSection =
      if (svCallingExecuted)
        List(
          "SV Calling" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariantsSv.ssp"))
      else Nil
    val oldPage: ReportPage = super.samplePage(sampleId, args)
    oldPage.copy(sections = variantcallingSection ++ svSection ++ oldPage.sections)
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

    val samples =
      Await.result(summary.getSamples(runId = runId, sampleId = sampleId), Duration.Inf)
    val statsPaths = {
      (for (sample <- Await.result(summary.getSamples(runId = runId), Duration.Inf)) yield {
        field
          .map(f => s"${sample.name};$f" -> List("total", "genotype", "general", sample.name, f))
          .toMap
      }).fold(Map())(_ ++ _)
    }

    val moduleName = target match {
      case Some(t) => s"multisample-vcfstats-$caller-$t"
      case _ => s"multisample-vcfstats-$caller"
    }

    val results = summary.getStatKeys(runId,
                                      "shivavariantcalling",
                                      moduleName,
                                      sampleId.map(SampleId).getOrElse(NoSample),
                                      keyValues = statsPaths)

    for (sample <- samples if sampleId.isEmpty || sample.id == sampleId.get) {
      tsvWriter.println(
        sample.name + "\t" + field
          .map(f => results.getOrElse(s"${sample.name};$f", Some("0")).get)
          .mkString("\t"))
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
