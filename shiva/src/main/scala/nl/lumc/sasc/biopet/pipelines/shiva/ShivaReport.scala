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
import nl.lumc.sasc.biopet.utils.summary.{ Summary, SummaryValue }

/**
 * With this extension the report is executed within a pipeline
 *
 * Created by pjvan_thof on 3/30/15.
 */
class ShivaReport(val root: Configurable) extends ReportBuilderExtension {
  def builder = ShivaReport
}

/** Object for report generation for Shiva pipeline */
object ShivaReport extends ShivaReportTrait

/** Trait for report generation for Shiva pipeline, this can be extended */
trait ShivaReportTrait extends MultisampleMappingReportTrait {

  def variantcallingExecuted = summary.getValue("shiva", "settings", "multisample_variantcalling") match {
    case Some(true) => true
    case _          => false
  }

  def svCallingExecuted = summary.getValue("shiva", "settings", "sv_calling") match {
    case Some(true) => true
    case _          => false
  }

  override def frontSection = ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/shivaFront.ssp")

  override def pipelineName = "shiva"

  override def additionalSections = {
    val params = Map("showPlot" -> true, "showTable" -> false)
    super.additionalSections ++
      (if (variantcallingExecuted) List("SNV Calling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariants.ssp", params)) else Nil) ++
      (if (svCallingExecuted) List("SV Calling" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/sampleVariantsSv.ssp", params)) else Nil)
  }

  /** Root page for the shiva report */
  override def indexPage = {
    val regions = regionsPage
    val oldPage = super.indexPage

    oldPage.copy(subPages = oldPage.subPages ++ regionsPage)
  }

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
  override def filesPage: ReportPage = {
    if (!variantcallingExecuted && !svCallingExecuted) super.filesPage
    else {
      var sections: List[(String, ReportSection)] = List()
      if (variantcallingExecuted) sections = sections :+ ("Result files from SNV calling", ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/outputVcfFiles.ssp", Map("sampleId" -> None)))
      if (svCallingExecuted) sections = sections :+ ("Result files from SV calling", ReportSection("/nl/lumc/sasc/biopet/pipelines/shiva/outputVcfFilesSv.ssp"))

      val oldPage = super.filesPage
      oldPage.copy(sections = oldPage.sections ++ sections)
    }
  }

  /** Single sample page */
  override def samplePage(sampleId: String, args: Map[String, Any]): ReportPage = {
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

    def getLine(summary: Summary, sample: String, summarySample: Option[String], lib: Option[String] = None): String = {
      val path = target match {
        case Some(t) => List("shivavariantcalling", "stats", s"multisample-vcfstats-$caller-$t", "genotype", sample)
        case _       => List("shivavariantcalling", "stats", s"multisample-vcfstats-$caller", "total", "genotype", "general", sample)
      }
      val homVar = new SummaryValue(path :+ "HomVar", summary, summarySample, lib).value.getOrElse(0).toString.toLong
      val homRef = new SummaryValue(path :+ "HomRef", summary, summarySample, lib).value.getOrElse(0).toString.toLong
      val noCall = new SummaryValue(path :+ "NoCall", summary, summarySample, lib).value.getOrElse(0).toString.toLong
      val het = new SummaryValue(path :+ "Het", summary, summarySample, lib).value.getOrElse(0).toString.toLong
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
        tsvWriter.println(getLine(summary, sample, sampleId, Some(lib)))
      }
    } else {
      for (sample <- summary.samples if sampleId.isEmpty || sample == sampleId.get) {
        tsvWriter.println(getLine(summary, sample, sampleId))
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

  def formatVcfFilePath(vcfFilePath: Option[Any]): Any = {
    val prefix = summary.getValue("meta", "output_dir").getOrElse("").toString
    vcfFilePath.collect { case a => "./" + a.toString.stripPrefix(prefix + File.separator) }
  }

}
