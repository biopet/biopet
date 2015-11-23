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
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.utils.summary.{ Summary, SummaryValue }
import nl.lumc.sasc.biopet.utils.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport

/**
 * With this extension the report is executed within a pipeline
 *
 * Created by pjvan_thof on 3/30/15.
 */
class ShivaReport(val root: Configurable) extends ReportBuilderExtension {
  def builder = ShivaReport
}

/** Object for report generation for Shiva pipeline */
object ShivaReport extends MultisampleReportBuilder {

  def variantcallingExecuted = summary.getValue("shiva", "settings", "multisample_variantcalling") match {
    case Some(true) => true
    case _          => false
  }

  override def extFiles = super.extFiles ++ List("js/gears.js")
    .map(x => ExtFile("/nl/lumc/sasc/biopet/pipelines/gears/report/ext/" + x, x))

  /** Root page for the shiva report */
  def indexPage = {
    val regions = regionsPage
    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        (if (regions.isDefined) Map(regions.get) else Map()) ++
        Map("Reference" -> ReportPage(List(), List(
          "Reference" -> ReportSection("/nl/lumc/sasc/biopet/core/report/reference.ssp", Map("pipeline" -> "shiva"))
        ), Map()),
          "Files" -> filesPage,
          "Versions" -> ReportPage(List(), List(
            "Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp")
          ), Map())
        ),
      List(
        "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/shivaFront.ssp")) ++
        (if (variantcallingExecuted) List("Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp",
          Map("showPlot" -> true, "showTable" -> false)))
        else Nil) ++
        List("Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
          Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)
        ),
          "Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp",
            Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)),
          "Whole genome coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/wgsHistogram.ssp",
            Map("sampleLevel" -> true, "showPlot" -> true, "showTable" -> false)),
          "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false)),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp",
            Map("showPlot" -> true, "showTable" -> false))
        ),
      pageArgs
    )
  }

  //TODO: Add variants per target
  /** Generate a page with all target coverage stats */
  def regionsPage: Option[(String, ReportPage)] = {
    val roi = summary.getValue("shiva", "settings", "regions_of_interest")
    val amplicon = summary.getValue("shiva", "settings", "amplicon_bed")

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
          "Variants" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp"),
          "Coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsMultiTable.ssp")
        ),
        Map("target" -> Some(p._1.stripSuffix(" (Amplicon)")))
      )).toList.sortBy(_._1),
      List(),
      Map())
    ) else None
  }

  /** Files page, can be used general or at sample level */
  def filesPage: ReportPage = ReportPage(List(), List(
    "Input fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepInputfiles.ssp"),
    "After QC fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepOutputfiles.ssp"),
    "Bam files per lib" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp", Map("sampleLevel" -> false)),
    "Preprocessed bam files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp",
      Map("pipelineName" -> "shiva", "fileTag" -> "preProcessBam"))) ++
    (if (variantcallingExecuted) List("VCF files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/outputVcfFiles.ssp",
      Map("sampleId" -> None)))
    else Nil), Map())

  /** Single sample page */
  def samplePage(sampleId: String, args: Map[String, Any]): ReportPage = {
    ReportPage(List(
      "Libraries" -> generateLibraryPage(args),
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), None),
      "Files" -> filesPage
    ), List(
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
        if (summary.libraries(sampleId).size > 1) Map("showPlot" -> true) else Map()),
      "Preprocessing" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp", Map("sampleLevel" -> true))) ++
      (if (variantcallingExecuted) List("Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp")) else Nil) ++
      List("QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
        "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
      ), args)
  }

  /** Library page */
  def libraryPage(sampleId: String, libId: String, args: Map[String, Any]): ReportPage = {
    val flexiprepExecuted = summary.getLibraryValue(sampleId, libId, "flexiprep").isDefined
    val krakenExecuted = summary.getValue(Some(sampleId), Some(libId), "gears", "stats", "krakenreport").isDefined

    ReportPage(
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), Some(libId)) ::
        (if (flexiprepExecuted) List("QC" -> FlexiprepReport.flexiprepPage) else Nil
        ) ::: (if (krakenExecuted) List("Gears - Metagenomics" -> ReportPage(List(), List(
          "Sunburst analysis" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/gears/gearsSunburst.ssp"
          )), Map()))
        else Nil), "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp") ::
        (if (flexiprepExecuted) List(
          "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
          "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
        )
        else Nil), args)
  }

  /** Name of the report */
  def reportName = "Shiva Report"

  /**
   * Generate a stackbar plot for found variants
   * @param outputDir OutputDir for the tsv and png file
   * @param prefix Prefix of the tsv and png file
   * @param summary Summary class
   * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
   * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
   */
  def variantSummaryPlot(outputDir: File,
                         prefix: String,
                         summary: Summary,
                         libraryLevel: Boolean = false,
                         sampleId: Option[String] = None,
                         caller: String = "final",
                         target: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) tsvWriter.print("Library") else tsvWriter.print("Sample")
    tsvWriter.println("\tHomVar\tHet\tHomRef\tNoCall")

    def getLine(summary: Summary, sample: String, lib: Option[String] = None): String = {
      val path = target match {
        case Some(t) => List("shivavariantcalling", "stats", s"multisample-vcfstats-$caller-$t", "genotype")
        case _ => List("shivavariantcalling", "stats", s"multisample-vcfstats-$caller", "genotype")
      }
      val homVar = new SummaryValue(path :+ "HomVar", summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val homRef = new SummaryValue(path :+ "HomRef", summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val noCall = new SummaryValue(path :+ "NoCall", summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val het = new SummaryValue(path :+ "Het", summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val sb = new StringBuffer()
      if (lib.isDefined) sb.append(sample + "-" + lib.get + "\t") else sb.append(sample + "\t")
      sb.append(homVar + "\t")
      sb.append(het + "\t")
      sb.append(homRef + "\t")
      sb.append(noCall)
      sb.toString
    }

    if (libraryLevel) {
      for (
        sample <- summary.samples if sampleId.isEmpty || sample == sampleId.get;
        lib <- summary.libraries(sample)
      ) {
        tsvWriter.println(getLine(summary, sample, Some(lib)))
      }
    } else {
      for (sample <- summary.samples if sampleId.isEmpty || sample == sampleId.get) {
        tsvWriter.println(getLine(summary, sample))
      }
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("VCF records")
    if (libraryLevel) {
      plot.width = Some(200 + (summary.libraries.filter(s => sampleId.getOrElse(s._1) == s._1).foldLeft(0)(_ + _._2.size) * 10))
    } else plot.width = Some(200 + (summary.samples.count(s => sampleId.getOrElse(s) == s) * 10))
    plot.runLocal()
  }
}
