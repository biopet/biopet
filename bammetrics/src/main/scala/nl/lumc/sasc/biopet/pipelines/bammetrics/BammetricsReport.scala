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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
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

  /** Root page for single BamMetrcis report */
  def indexPage = {
    val bamMetricsPage = this.bamMetricsPage(summary, sampleId, libId)
    ReportPage(bamMetricsPage.subPages ::: List(
      "Versions" -> ReportPage(List(), List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
      )), Map()),
      "Files" -> ReportPage(List(), List(
        "Input fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/bammetricsInputFile.ssp")
      ), Map())
    ), List(
      "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/bamMetricsFront.ssp")
    ) ::: bamMetricsPage.sections,
      Map()
    )
  }

  /** Generates a page with alignment stats */
  def bamMetricsPage(summary: SummaryDb,
                     sampleId: Option[Int],
                     libId: Option[Int],
                     metricsTag: String = "bammetrics") = {

    //val pipelineId: Int = summary.getPipelineId(runId, metricsTag).map(_.get)

    val wgsExecuted = summary.getStatsSize(runId, Right(metricsTag), Some(Right("wgs")), sample = sampleId.map(Left(_)), library = libId.map(Left(_))) >= 1
    val rnaExecuted = summary.getStatsSize(runId, Right(metricsTag), Some(Right("rna")), sample = sampleId.map(Left(_)), library = libId.map(Left(_))) >= 1

    val insertsizeMetrics = summary.getStatKeys(runId, Right(metricsTag), Some(Right("CollectInsertSizeMetrics")),
      sample = sampleId.map(Left(_)), library = libId.map(Left(_)), Map("metrics" -> List("metrics")))
        .exists(_._2.isDefined)


    val targetSettings = summary.getSettingKeys(runId, Right(metricsTag),None, sample = sampleId.map(Left(_)), library = libId.map(Left(_)),
      Map("amplicon_name" -> List("amplicon_name"), "roi_name" -> List("roi_name")))
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
      else List("Targets" -> ReportPage(
        List(),
        targets.map(t => t -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsPlot.ssp", Map("target" -> Some(t)))),
        Map())),
      List(
        "Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
        "Mapping Quality" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/mappingQuality.ssp", Map("showPlot" -> true)),
        "Clipping" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/clipping.ssp", Map("showPlot" -> true))) ++
        (if (insertsizeMetrics) List("Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp", Map("showPlot" -> true))
        )
        else Nil) ++ (if (wgsExecuted) List("Whole genome coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/wgsHistogram.ssp",
          Map("showPlot" -> true)))
        else Nil) ++
        (if (rnaExecuted) List("Rna coverage" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/rnaHistogram.ssp",
          Map("showPlot" -> true)))
        else Nil),
      Map("metricsTag" -> metricsTag)
    )
  }

  /**
   * Generate a stackbar plot for alignment stats
   *
   * @param outputDir OutputDir for the tsv and png file
   * @param prefix Prefix of the tsv and png file
   * @param summary Summary class
   * @param libraryLevel Default false, when set true plot will be based on library stats instead of sample stats
   * @param sampleId Default it selects all sampples, when sample is giving it limits to selected sample
   */
  def alignmentSummaryPlot(outputDir: File,
                           prefix: String,
                           summary: SummaryDb,
                           libraryLevel: Boolean = false,
                           sampleId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) tsvWriter.print("Library") else tsvWriter.print("Sample")
    tsvWriter.println("\tMapped\tDuplicates\tUnmapped\tSecondary")

    val statsPaths = Map(
      "Mapped" -> List("flagstats", "Mapped"),
      "Duplicates" -> List("flagstats", "Duplicates"),
      "All" -> List("flagstats", "All"),
      "NotPrimaryAlignment" -> List("flagstats", "NotPrimaryAlignment")
    )

    val pipelineId: Int = summary.getPipelineId(runId, "bammetrics").map(_.get)
    val moduleId: Option[Int] = summary.getmoduleId(runId, "bamstats", pipelineId)

    val results: Map[(Int, Option[Int]), Map[String, Option[Any]]] = if (libraryLevel) {
      summary.getStatsForLibraries(runId = runId, pipelineName = "bammetrics", moduleName = Some("bamstats"), sampleId = sampleId, keyValues = statsPaths).map(x => (x._1._1, Some(x._1._2)) -> x._2)
    } else summary.getStatsForSamples(runId, pipelineId, moduleId, sample = sampleId, keyValues = statsPaths).map(x => (x._1, None) -> x._2)

    for (((s,l),result) <- results) {
      val sampleName: String = summary.getSampleName(s).map(_.get)
      val libName: Option[String] = l.flatMap(x => Await.result(summary.getLibraryName(x), Duration.Inf))
      val sb = new StringBuffer()
      if (libName.isDefined) sb.append(sampleName + "-" + libName.get + "\t") else sb.append(sampleName + "\t")
      val mapped = ConfigUtils.any2long(result("Mapped"))
      val duplicates = ConfigUtils.any2long(result("Duplicates"))
      val total = ConfigUtils.any2long(result("All"))
      val secondary = ConfigUtils.any2long(result("NotPrimaryAlignment"))
      sb.append((mapped - duplicates - secondary) + "\t")
      sb.append(duplicates + "\t")
      sb.append((total - mapped) + "\t")
      sb.append(secondary)
      tsvWriter.println(sb.toString)
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Reads")
    if (libraryLevel) {
      plot.width = Some(200 + (libraries.filter(s => sampleId.getOrElse(s.id) == s.id).size) * 10)
    } else plot.width = Some(200 + (samples.count(s => sampleId.getOrElse(s) == s) * 10))
    plot.title = Some("Aligned reads")
    plot.runLocal()
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
                     summary: SummaryDb,
                     libraryLevel: Boolean = false,
                     sampleId: Option[Int] = None,
                     libId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    val statsPaths = Map(
      "insert_size" -> List("histogram", "insert_size"),
      "All_Reads.fr_count" -> List("histogram", "All_Reads.fr_count")
    )

    val pipelineId: Int = summary.getPipelineId(runId, "bammetrics").map(_.get)
    val moduleId: Option[Int] = summary.getmoduleId(runId, "CollectInsertSizeMetrics", pipelineId)

    val results: Map[(Int, Option[Int]), Map[String, Option[Array[Any]]]] = if (libraryLevel) {
      summary.getStatsForLibraries(runId, pipelineId, moduleId, sampleId = sampleId, keyValues = statsPaths)
        .map(x => (x._1._1, Some(x._1._2)) -> x._2.map(x => x._1 -> x._2.map(ConfigUtils.any2list(_).toArray)))
    } else summary.getStatsForSamples(runId, pipelineId, moduleId, sample = sampleId, keyValues = statsPaths)
      .map(x => (x._1, None) -> x._2.map(x => x._1 -> x._2.map(ConfigUtils.any2list(_).toArray)))

    val tables = getSampleLibraries(summary, sampleId, libId, libraryLevel)
      .map {
        case (sample, lib) =>
          getTableFromSummary(summary, paths(lib.map(l => s"$sample-$l").getOrElse(sample)), Some(sample), lib)
      }
    writeTableToTsv(tsvFile, mergeTables(tables.toArray, "insert_size"), "insert_size")

    LinePlot(tsvFile, pngFile,
      xlabel = Some("Insert size"),
      ylabel = Some("Reads"),
      title = Some("Insert size"),
      removeZero = true).runLocal()
  }

  def mappingQualityPlot(outputDir: File,
                         prefix: String,
                         summary: SummaryDb,
                         libraryLevel: Boolean = false,
                         sampleId: Option[Int] = None,
                         libId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "mapping_quality" -> List("bammetrics", "stats", "bamstats", "mapping_quality", "histogram", "values"),
      name -> List("bammetrics", "stats", "bamstats", "mapping_quality", "histogram", "counts")
    )

    val tables = getSampleLibraries(summary, sampleId, libId, libraryLevel)
      .map {
        case (sample, lib) =>
          getTableFromSummary(summary, paths(lib.map(l => s"$sample-$l").getOrElse(sample)), Some(sample), lib)
      }
    writeTableToTsv(tsvFile, mergeTables(tables.toArray, "mapping_quality"), "mapping_quality")

    LinePlot(tsvFile, pngFile,
      xlabel = Some("Mapping Quality"),
      ylabel = Some("Reads"),
      title = Some("Mapping Quality"),
      removeZero = true).runLocal()
  }

  def clippingPlot(outputDir: File,
                   prefix: String,
                   summary: SummaryDb,
                   libraryLevel: Boolean = false,
                   sampleId: Option[Int] = None,
                   libId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "clipping" -> List("bammetrics", "stats", "bamstats", "clipping", "histogram", "values"),
      name -> List("bammetrics", "stats", "bamstats", "clipping", "histogram", "counts")
    )

    val tables = getSampleLibraries(summary, sampleId, libId, libraryLevel)
      .map {
        case (sample, lib) =>
          getTableFromSummary(summary, paths(lib.map(l => s"$sample-$l").getOrElse(sample)), Some(sample), lib)
      }
    writeTableToTsv(tsvFile, mergeTables(tables.toArray, "clipping"), "clipping")

    LinePlot(tsvFile, pngFile,
      xlabel = Some("Clipping"),
      ylabel = Some("Reads"),
      title = Some("Clipping"),
      removeZero = true).runLocal()
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
                       summary: SummaryDb,
                       libraryLevel: Boolean = false,
                       sampleId: Option[Int] = None,
                       libId: Option[Int] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "coverage" -> List("bammetrics", "stats", "wgs", "histogram", "coverage"),
      name -> List("bammetrics", "stats", "wgs", "histogram", "count")
    )

    val tables = getSampleLibraries(summary, sampleId, libId, libraryLevel)
      .map {
        case (sample, lib) =>
          getTableFromSummary(summary, paths(lib.map(l => s"$sample-$l").getOrElse(sample)), Some(sample), lib)
      }
    writeTableToTsv(tsvFile, mergeTables(tables.toArray, "coverage"), "coverage")

    LinePlot(tsvFile, pngFile,
      xlabel = Some("Coverage"),
      ylabel = Some("Bases"),
      title = Some("Whole genome coverage"),
      removeZero = true).runLocal()
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
                       summary: SummaryDb,
                       libraryLevel: Boolean = false,
                       sampleId: Option[String] = None,
                       libId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "normalized_position" -> List("bammetrics", "stats", "rna", "histogram", "normalized_position"),
      name -> List("bammetrics", "stats", "rna", "histogram", "All_Reads.normalized_coverage")
    )

    val tables = getSampleLibraries(summary, sampleId, libId, libraryLevel)
      .map {
        case (sample, lib) =>
          getTableFromSummary(summary, paths(lib.map(l => s"$sample-$l").getOrElse(sample)), Some(sample), lib)
      }
    writeTableToTsv(tsvFile, mergeTables(tables.toArray, "normalized_position"), "normalized_position")

    LinePlot(tsvFile, pngFile,
      xlabel = Some("Relative position"),
      ylabel = Some("Coverage"),
      title = Some("Rna coverage"),
      removeZero = true).runLocal()
  }

  private def getSampleLibraries(summary: SummaryDb,
                                 sampleId: Option[Int] = None,
                                 LibId: Option[Int] = None,
                                 libraryLevel: Boolean = false): List[(Int, Option[Int])] = {
    if (LibId.isDefined) require(sampleId.isDefined)
    if (libraryLevel || LibId.isDefined)
      for ((sample, libs) <- summary.libraries.toList; lib <- libs) yield (sample, Some(lib))
    else for ((sample, libs) <- summary.libraries.toList) yield (sample, None)
  }

  def getTableFromSummary(summary: SummaryDb,
                          paths: Map[String, List[String]],
                          sampleId: Option[Int] = None,
                          libId: Option[Int] = None): Map[String, Array[Any]] = {
    val pathValues: Map[String, Array[Any]] = paths.map {
      case (key, path) =>
        val value = summary.getValueAsArray(sampleId, libId, path: _*)
        key -> value.getOrElse(Array())
    }
    require(pathValues.map(_._2.size).toList.distinct.size == 1, s"Arrays in summary does not have the same number of values, $paths")
    pathValues
  }

  def mergeTables(tables: Array[Map[String, Array[Any]]],
                  mergeColumn: String, defaultValue: Any = 0): Map[String, Array[Any]] = {
    val keys = tables.flatMap(x => x(mergeColumn)).distinct
    (for (table <- tables; (columnKey, columnValues) <- table if columnKey != mergeColumn) yield {
      columnKey -> keys.map(x => table(mergeColumn).zip(columnValues).toMap.getOrElse(x, defaultValue))
    }).toMap + (mergeColumn -> keys)
  }

  def writeTableToTsv(tsvFile: File, table: Map[String, Array[Any]], firstColumn: String): Unit = {
    require(table.map(_._2.size).toList.distinct.size == 1, "Not all values has the same number or rows")
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
