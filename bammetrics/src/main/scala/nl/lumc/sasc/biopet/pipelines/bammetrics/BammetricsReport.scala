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
package nl.lumc.sasc.biopet.pipelines.bammetrics

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report.{ReportBuilder, ReportBuilderExtension, ReportPage, ReportSection}
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.rscript.{LinePlot, StackedBarPlot}
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._
import nl.lumc.sasc.biopet.utils.summary.db.Schema._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class BammetricsReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = BammetricsReport
}

/**
  * Object to create a report for [[BamMetrics]]
  *
  * Created by pjvan_thof on 3/30/15.
  */
object BammetricsReport extends ReportBuilder {

  /** Name of report */
  val reportName = "Bam Metrics"

  def pipelineName = "bammetrics"

  /** Root page for single BamMetrcis report */
  def indexPage: Future[ReportPage] =
    bamMetricsPage(summary, sampleId, libId).map { bamMetricsPage =>
      ReportPage(
        bamMetricsPage.subPages ::: List(
          "Versions" -> Future(
            ReportPage(List(),
                       List("Executables" -> ReportSection(
                         "/nl/lumc/sasc/biopet/core/report/executables.ssp")),
                       Map())),
          "Files" -> filesPage(sampleId, libId)
        ),
        List(
          "Report" -> ReportSection(
            "/nl/lumc/sasc/biopet/pipelines/bammetrics/bamMetricsFront.ssp")
        ) ::: bamMetricsPage.sections,
        Map()
      )
    }

  /** Generates a page with alignment stats */
  def bamMetricsPage(summary: SummaryDb,
                     sampleId: Option[Int],
                     libId: Option[Int],
                     metricsTag: String = "bammetrics"): Future[ReportPage] = Future {
    val wgsExecuted = summary.getStatsSize(runId,
                                           metricsTag,
                                           "wgs",
                                           sample = sampleId.map(SampleId),
                                           library = libId.map(LibraryId)) >= 1
    val rnaExecuted = summary.getStatsSize(runId,
                                           metricsTag,
                                           "rna",
                                           sample = sampleId.map(SampleId),
                                           library = libId.map(LibraryId)) >= 1

    val insertsizeMetrics = summary
      .getStatKeys(
        runId,
        metricsTag,
        "CollectInsertSizeMetrics",
        sampleId.map(SampleId).getOrElse(NoSample),
        libId.map(LibraryId).getOrElse(NoLibrary),
        Map("metrics" -> List("metrics"))
      )
      .exists(_._2.isDefined)

    val targetSettings = summary.getSettingKeys(
      runId,
      metricsTag,
      NoModule,
      sample = sampleId.map(SampleId).getOrElse(NoSample),
      library = libId.map(LibraryId).getOrElse(NoLibrary),
      Map("amplicon_name" -> List("amplicon_name"), "roi_name" -> List("roi_name"))
    )
    val targets = (
      targetSettings("amplicon_name"),
      targetSettings("roi_name")
    ) match {
      case (Some(amplicon: String), Some(roi: List[_])) => amplicon :: roi.map(_.toString)
      case (_, Some(roi: List[_])) => roi.map(_.toString)
      case _ => Nil
    }

    ReportPage(
      if (targets.isEmpty) List()
      else
        List(
          "Targets" -> Future.successful(
            ReportPage(
              List(),
              targets.map(t =>
                t -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsPlot.ssp",
                                   Map("target" -> Some(t)))),
              Map()))),
      List(
        "Summary" -> ReportSection(
          "/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
        "Mapping Quality" -> ReportSection(
          "/nl/lumc/sasc/biopet/pipelines/bammetrics/mappingQuality.ssp",
          Map("showPlot" -> true)),
        "Clipping" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/clipping.ssp",
                                    Map("showPlot" -> true))
      ) ++
        (if (insertsizeMetrics)
           List(
             "Insert Size" -> ReportSection(
               "/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp",
               Map("showPlot" -> true)))
         else Nil) ++ (if (wgsExecuted)
                         List(
                           "Whole genome coverage" -> ReportSection(
                             "/nl/lumc/sasc/biopet/pipelines/bammetrics/wgsHistogram.ssp",
                             Map("showPlot" -> true)))
                       else Nil) ++
        (if (rnaExecuted)
           List(
             "Rna coverage" -> ReportSection(
               "/nl/lumc/sasc/biopet/pipelines/bammetrics/rnaHistogram.ssp",
               Map("showPlot" -> true)))
         else Nil),
      Map("metricsTag" -> metricsTag)
    )
  }

  /**
    * Generates the lines for alignmentSummaryPlot
    *
    * @param summary Summary class
    * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
    *                     * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
    */
  def alignmentSummaryPlotLines(summary: SummaryDb,
                       sampleId: Option[Int] = None,
                                libraryLevel: Boolean = false): Seq[String] = {
    val statsPaths = Map(
      "Mapped" -> List("flagstats", "Mapped"),
      "Duplicates" -> List("flagstats", "Duplicates"),
      "All" -> List("flagstats", "All"),
      "NotPrimaryAlignment" -> List("flagstats", "NotPrimaryAlignment")
    )

    val results: Map[(Int, Option[Int]), Map[String, Option[Any]]] = if (libraryLevel) {
      summary
        .getStatsForLibraries(runId,
          "bammetrics",
          "bamstats",
          sampleId = sampleId,
          keyValues = statsPaths)
        .map(x => (x._1._1, Some(x._1._2)) -> x._2)
    } else
      summary
        .getStatsForSamples(runId,
          "bammetrics",
          "bamstats",
          sample = sampleId.map(SampleId),
          keyValues = statsPaths)
        .map(x => (x._1, None) -> x._2)
    val summaryPlotLines = ArrayBuffer[String]()

    for (((s, l), result) <- results) {
      val sampleName: String = summary.getSampleName(s).map(_.get)
      val libName: Option[String] =
        l.flatMap(x => Await.result(summary.getLibraryName(x), Duration.Inf))
      val sb = new StringBuffer()
      if (libName.isDefined) sb.append(sampleName + "-" + libName.get + "\t")
      else sb.append(sampleName + "\t")
      val mapped = ConfigUtils.any2long(result("Mapped"))
      val duplicates = ConfigUtils.any2long(result("Duplicates"))
      val total = ConfigUtils.any2long(result("All"))
      val secondary = ConfigUtils.any2long(result("NotPrimaryAlignment"))
      sb.append((mapped - duplicates - secondary) + "\t")
      sb.append(duplicates + "\t")
      sb.append((total - mapped) + "\t")
      sb.append(secondary)
      summaryPlotLines += sb.toString
    }
    summaryPlotLines
  }

  /**
    * Generate a stackbar plot for alignment stats
    *
    * @param outputDir OutputDir for the tsv and png file
    * @param prefix Prefix of the tsv and png file
    * @param summaryPlotLines A sequence of strings written to the summary tsv
    * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
    */
  def alignmentSummaryPlot(outputDir: File,
                           prefix: String,
                           summaryPlotLines: Seq[String],
                           libraryLevel: Boolean = false
                           ): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) tsvWriter.print("Library") else tsvWriter.print("Sample")
    tsvWriter.println("\tMapped\tDuplicates\tUnmapped\tSecondary")

    for (line <- summaryPlotLines) {
      tsvWriter.println(line)
    }
    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Reads")
    plot.width = Some(200 + (summaryPlotLines.size * 10))
    plot.title = Some("Aligned_reads")
    plot.runLocal()
  }
  /**
    * This is a generic method to create plots
    * @param libraryLevel If enabled the plots will show data per library
    * @param sampleId If set only this sample is shown
    * @param libraryId If set onlt this library is shown
    * @param statsPaths Paths in summary where the tables can be found
    * @param yKeyList Keys to search from, first has prio over second one
    * @param xKeyList Keys to search from, first has prio over second one
    * @param pipeline Query for the pipeline
    * @param module Query for the module
    */
  def summaryForPlot(                   statsPaths: Map[String, List[String]],
                                        summary: SummaryDb,
                                        yKeyList: List[String],
                                        xKeyList: List[String],
                                        pipeline: PipelineQuery,
                                        module: ModuleQuery,
                                        libraryLevel: Boolean = false,
                     sampleId: Option[Int] = None,
                     libraryId: Option[Int] = None
                      ): Any = {
    val results: Map[(Int, Option[Int]), Map[String, Option[Array[Any]]]] = if (libraryLevel) {
      summary
        .getStatsForLibraries(runId, pipeline, module, sampleId = sampleId, keyValues = statsPaths)
        .map(x =>
          (x._1._1, Some(x._1._2)) -> x._2.map(x =>
            x._1 -> x._2.map(ConfigUtils.any2list(_).toArray)))
    } else
      summary
        .getStatsForSamples(runId,
          pipeline,
          module,
          sample = sampleId.map(SampleId),
          keyValues = statsPaths)
        .map(x => (x._1, None) -> x._2.map(x => x._1 -> x._2.map(ConfigUtils.any2list(_).toArray)))
    val tables: Array[Map[String, Array[Any]]] = results.map {
      case ((sample, library), map) =>
        val sampleName = Await
          .result(summary.getSampleName(sample), Duration.Inf)
          .getOrElse(throw new IllegalStateException("Sample must be there"))
        val libraryName =
          library.flatMap(l => Await.result(summary.getLibraryName(l), Duration.Inf))
        val yKey = yKeyList.find(x => map.contains(x) && map(x).isDefined).getOrElse("none")
        val xKey = xKeyList.find(x => map.contains(x) && map(x).isDefined).getOrElse("none")
        Map(
          yKeyList.head -> map.getOrElse(yKey, None).getOrElse(Array()),
          (sampleName + libraryName.map("-" + _).getOrElse("")) -> map
            .getOrElse(xKey, None)
            .getOrElse(Array())
        )
    }.toArray
    tables
  }

  /**
    * This is a generic method to create plots
    * @param outputDir Outputdir of the plot
    * @param prefix Files will start with this name
    * @param tables Tables to be written
    * @param yKeyList Keys to search from, first has prio over second one
    * @param xKeyList Keys to search from, first has prio over second one
    * @param xlabel X label shown on the plot
    * @param ylabel Y label shown on the plot
    * @param title Title of the plot
    * @param removeZero
    */

  def writePlotFromSummary(outputDir: File,
                           prefix: String,
                           tables: Array[Map[String, Array[Any]]],
                           xKeyList: List[String],
                           yKeyList: List[String],
                           xlabel: Option[String] = None,
                           ylabel: Option[String] = None,
                           title: Option[String] = None,
                           removeZero: Boolean = true): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    writeTableToTsv(tsvFile, mergeTables(tables, yKeyList.head), yKeyList.head)

    LinePlot(tsvFile,
             pngFile,
             xlabel = xlabel,
             ylabel = ylabel,
             title = title,
             hideLegend = tables.size > 40, /* changed from results.size. Original results in summaryForPlot*/
             removeZero = removeZero).runLocal()
  }

  /**
    * Generate a line plot for insertsize
    *
    * @param outputDir OutputDir for the tsv and png file
    * @param prefix Prefix of the tsv and png file
    * @param summary Summary class
    * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
    * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
    */
  def insertSizePlot(outputDir: File,
                     prefix: String,
                     tables: Array[Map[String, Array[Any]]],
                     libraryLevel: Boolean = false,
                     sampleId: Option[Int] = None,
                     libraryId: Option[Int] = None): Unit = {
    val statsPaths = Map(
      "insert_size" -> List("histogram", "insert_size"),
      "count" -> List("histogram", "All_Reads.fr_count")
    )

    writePlotFromSummary(
      outputDir,
      prefix,
      tables,
      libraryLevel,
      sampleId,
      libraryId,
      statsPaths,
      "insert_size" :: Nil,
      "count" :: Nil,
      "bammetrics",
      "CollectInsertSizeMetrics",
      "Insert size",
      "Reads",
      "Insert size"
    )
  }

  def mappingQualityPlot(outputDir: File,
                         prefix: String,
                         summary: SummaryDb,
                         libraryLevel: Boolean = false,
                         sampleId: Option[Int] = None,
                         libraryId: Option[Int] = None): Unit = {
    val statsPaths = Map(
      "mapping_quality" -> List("mapping_quality", "histogram", "values"),
      "count" -> List("mapping_quality", "histogram", "counts")
    )
    summaryForPlot(statsPaths,
      "mapping_quality" :: Nil,
      "count" :: Nil,
      "bammetrics",
      "bamstats"
      )
    writePlotFromSummary(
      outputDir,
      prefix,
      summary,
      libraryLevel,
      sampleId,
      libraryId,
      statsPaths,

,
      "Mapping quality",
      "Reads",
      "Mapping quality"
    )
  }

  def clippingPlot(outputDir: File,
                   prefix: String,
                   tables: Array[Map[String, Array[Any]]],
                   libraryLevel: Boolean = false,
                   sampleId: Option[Int] = None,
                   libraryId: Option[Int] = None): Unit = {
    val statsPaths = Map(
      "clipping" -> List("clipping", "histogram", "values"),
      "count" -> List("clipping", "histogram", "counts")
    )

    writePlotFromSummary(
      outputDir,
      prefix,
      summary,
      libraryLevel,
      sampleId,
      libraryId,
      statsPaths,
      "clipping" :: Nil,
      "count" :: Nil,
      "bammetrics",
      "bamstats",
      "Clipping",
      "Reads",
      "Clipping"
    )
  }

  /**
    * Generate a line plot for wgs coverage
    *
    * @param outputDir OutputDir for the tsv and png file
    * @param prefix Prefix of the tsv and png file
    * @param summary Summary class
    * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
    * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
    */
  def wgsHistogramPlot(outputDir: File,
                       prefix: String,
                       tables: Array[Map[String, Array[Any]]],
                       libraryLevel: Boolean = false,
                       sampleId: Option[Int] = None,
                       libraryId: Option[Int] = None): Unit = {
    val statsPaths = Map(
      "coverage" -> List("histogram", "coverage"),
      "count" -> List("histogram", "count"),
      "high_quality_coverage_count" -> List("histogram", "high_quality_coverage_count")
    )

    writePlotFromSummary(
      outputDir,
      prefix,
      summary,
      libraryLevel,
      sampleId,
      libraryId,
      statsPaths,
      "coverage" :: Nil,
      "count" :: "high_quality_coverage_count" :: Nil,
      "bammetrics",
      "wgs",
      "Coverage",
      "Bases",
      "Whole genome coverage"
    )
  }

  /**
    * Generate a line plot for rna coverage
    *
    * @param outputDir OutputDir for the tsv and png file
    * @param prefix Prefix of the tsv and png file
    * @param summary Summary class
    * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
    * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
    */
  def rnaHistogramPlot(outputDir: File,
                       prefix: String,
                       tables: Array[Map[String, Array[Any]]],
                       libraryLevel: Boolean = false,
                       sampleId: Option[Int] = None,
                       libraryId: Option[Int] = None): Unit = {
    val statsPaths = Map(
      "position" -> List("histogram", "normalized_position"),
      "count" -> List("histogram", "All_Reads.normalized_coverage")
    )

    writePlotFromSummary(
      outputDir,
      prefix,
      summary,
      libraryLevel,
      sampleId,
      libraryId,
      statsPaths,
      "position" :: Nil,
      "count" :: Nil,
      "bammetrics",
      "rna",
      "Relative position",
      "Coverage",
      "Rna coverage"
    )
  }

  def mergeTables(tables: Array[Map[String, Array[Any]]],
                  mergeColumn: String,
                  defaultValue: Any = 0): Map[String, Array[Any]] = {
    val keys = tables.flatMap(x => x(mergeColumn)).distinct
    (for (table <- tables; (columnKey, columnValues) <- table if columnKey != mergeColumn) yield {
      columnKey -> keys.map(x =>
        table(mergeColumn).zip(columnValues).toMap.getOrElse(x, defaultValue))
    }).toMap + (mergeColumn -> keys)
  }

  def writeTableToTsv(tsvFile: File, table: Map[String, Array[Any]], firstColumn: String): Unit = {
    require(table.map(_._2.length).toList.distinct.size == 1,
            "Not all values has the same number or rows")
    val keys = table.keys.filterNot(_ == firstColumn).toList.sorted
    val writer = new PrintWriter(tsvFile)
    writer.println((firstColumn :: keys).mkString("\t"))
    table(firstColumn).zipWithIndex.foreach {
      case (c, i) =>
        writer.println((c :: keys.map(x => table(x)(i))).mkString("\t"))
    }
    writer.close()
  }
}

object BamMetricsAlignmentReport {
  def values(summary: SummaryDb,
             runId: Int,
             allSamples: Seq[Sample],
             allLibraries: Seq[Library],
             sampleId: Option[Int] = None,
             libId: Option[Int] = None,
             sampleLevel: Boolean = false,
             showPlot: Boolean = false,
             showIntro: Boolean = true,
             showTable: Boolean = true): Map[String,Any] = {

    val statsPaths = Map(
        "All" -> List("flagstats", "All"),
        "Mapped" -> List("flagstats", "Mapped"),
        "Duplicates" -> List("flagstats", "Duplicates"),
        "NotPrimaryAlignment" -> List("flagstats", "NotPrimaryAlignment")
      )
    val alignmentSummaryResults = summary.getStatsForLibraries(runId,"bammetrics","bamstats", sampleId, statsPaths)
    val alignmentSummaryPlotLines: Option[Seq[String]] =
      if (showPlot)
        Some(BammetricsReport.alignmentSummaryPlotLines(summary,sampleId,!sampleLevel))
      else None
  Map(
    "alignmentSummaryResults" -> alignmentSummaryResults,
    "alignmentSummaryPlotLines" -> alignmentSummaryPlotLines,
    "sampleLevel" -> sampleLevel,
    "showPlot" -> showPlot,
    "showIntro" -> showIntro,
    "showTable" -> showTable,
    "sampleId" -> sampleId,
    "libId" -> libId
  )
  }
}
object BamMetricsMappingQuality {
  def values(summary: SummaryDb,
             runId: Int,
             allSamples: Seq[Sample],
             allLibraries: Seq[Library],
             sampleId: Option[Int],
             libId: Option[Int],
             sampleLevel: Boolean= false,
             showPlot: Boolean = false,
             showIntro: Boolean = true,
             showTable: Boolean = true): Map[String,Any] = {

    val samples = sampleId match {
      case Some(id) => allSamples.filter(_.id == id).toList
      case _ => allSamples.toList
    }
    val
  }
}