package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.report._
import nl.lumc.sasc.biopet.core.summary.{ SummaryValue, Summary }
import nl.lumc.sasc.biopet.extensions.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.pipelines.bammetrics.BammetricsReport
import nl.lumc.sasc.biopet.pipelines.flexiprep.FlexiprepReport

/**
 * Created by pjvan_thof on 3/30/15.
 */
class ShivaReport(val root: Configurable) extends ReportBuilderExtension {
  val builder = ShivaReport

  override val defaultCoreMemory = 3.0
}

object ShivaReport extends MultisampleReportBuilder {

  // FIXME: Not yet finished

  def indexPage = {
    val regions = regionsPage
    ReportPage(
      List("Samples" -> generateSamplesPage(pageArgs)) ++
        (if (regions.isDefined) Map(regions.get) else Map()) ++
        Map("Files" -> filesPage,
          "Versions" -> ReportPage(List(), List((
            "Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
            ))), Map())
        ),
      List(
        "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/shivaFront.ssp"),
        "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp",
          Map("showPlot" -> true, "showTable" -> false)),
        "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
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

  def regionsPage = {
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
      case Some(x: String) => regionPages += (x + " (Amplicon)") -> createPage(x, true)
      case _               =>
    }

    roi match {
      case Some(x: String)  => regionPages += x -> createPage(x, false)
      case Some(x: List[_]) => x.foreach(x => regionPages += x.toString -> createPage(x.toString, false))
      case _                =>
    }

    if (regionPages.nonEmpty) Some("Regions" -> ReportPage(
      List(),
      regionPages.map(p => (p._1 -> ReportSection(
        "/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsMultiTable.ssp",
        Map("target" -> p._1.stripSuffix(" (Amplicon)"))
      ))).toList.sortBy(_._1),
      Map())
    )
    else None
  }

  def filesPage = ReportPage(List(), List(
    "Input fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepInputfiles.ssp"),
    "After QC fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepOutputfiles.ssp"),
    "Bam files per lib" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp", Map("sampleLevel" -> false)),
    "Preprocessed bam files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/mapping/outputBamfiles.ssp",
      Map("pipelineName" -> "shiva", "fileTag" -> "preProcessBam")),
    "VCF files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/outputVcfFiles.ssp", Map("sampleId" -> None))
  ), Map())

  def samplePage(sampleId: String, args: Map[String, Any]) = {
    ReportPage(List(
      "Libraries" -> generateLibraryPage(args),
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), None),
      "Files" -> filesPage
    ), List(
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp",
        if (summary.libraries(sampleId).size > 1) Map("showPlot" -> true) else Map()),
      "Preprocessing" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp", Map("sampleLevel" -> true)),
      "Variantcalling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp"),
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  def libraryPage(sampleId: String, libId: String, args: Map[String, Any]) = {
    ReportPage(List(
      "Alignment" -> BammetricsReport.bamMetricsPage(summary, Some(sampleId), Some(libId)),
      "QC" -> FlexiprepReport.flexiprepPage
    ), List(
      "Alignment" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
      "QC reads" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "QC bases" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp")
    ), args)
  }

  def reportName = "Shiva Report"

  def variantSummaryPlot(outputDir: File,
                         prefix: String,
                         summary: Summary,
                         libraryLevel: Boolean = false,
                         sampleId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) tsvWriter.print("Library") else tsvWriter.print("Sample")
    tsvWriter.println("\tHomVar\tHet\tHomRef\tNoCall")

    def getLine(summary: Summary, sample: String, lib: Option[String] = None): String = {
      val homVar = new SummaryValue(List("shivavariantcalling", "stats", "multisample-vcfstats-final", "genotype", "HomVar"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val homRef = new SummaryValue(List("shivavariantcalling", "stats", "multisample-vcfstats-final", "genotype", "HomRef"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val noCall = new SummaryValue(List("shivavariantcalling", "stats", "multisample-vcfstats-final", "genotype", "NoCall"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val het = new SummaryValue(List("shivavariantcalling", "stats", "multisample-vcfstats-final", "genotype", "Het"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
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
        sample <- summary.samples if (sampleId.isEmpty || sample == sampleId.get);
        lib <- summary.libraries(sample)
      ) {
        tsvWriter.println(getLine(summary, sample, Some(lib)))
      }
    } else {
      for (sample <- summary.samples if (sampleId.isEmpty || sample == sampleId.get)) {
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
    } else plot.width = Some(200 + (summary.samples.filter(s => sampleId.getOrElse(s) == s).size * 10))
    plot.runLocal()
  }
}
