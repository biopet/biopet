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

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report.{ ReportBuilderExtension, ReportBuilder, ReportPage, ReportSection }
import nl.lumc.sasc.biopet.utils.summary.{ Summary, SummaryValue }
import nl.lumc.sasc.biopet.utils.rscript.{ StackedBarPlot, LinePlot }

class BammetricsReport(val root: Configurable) extends ReportBuilderExtension {
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
  def bamMetricsPage(summary: Summary,
                     sampleId: Option[String],
                     libId: Option[String],
                     metricsTag: String = "bammetrics") = {

    val wgsExecuted = summary.getValue(sampleId, libId, metricsTag, "stats", "wgs").isDefined
    val rnaExecuted = summary.getValue(sampleId, libId, metricsTag, "stats", "rna").isDefined

    val insertsizeMetrics = summary.getValue(sampleId, libId, metricsTag, "stats", "CollectInsertSizeMetrics", "metrics") match {
      case Some(None) => false
      case Some(_)    => true
      case _          => false
    }

    val targets = (
      summary.getValue(sampleId, libId, metricsTag, "settings", "amplicon_name"),
      summary.getValue(sampleId, libId, metricsTag, "settings", "roi_name")
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
                           summary: Summary,
                           libraryLevel: Boolean = false,
                           sampleId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) tsvWriter.print("Library") else tsvWriter.print("Sample")
    tsvWriter.println("\tMapped\tDuplicates\tUnmapped\tSecondary")

    def getLine(summary: Summary, sample: String, lib: Option[String] = None): String = {
      val mapped = new SummaryValue(List("bammetrics", "stats", "bamstats", "flagstats", "Mapped"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val duplicates = new SummaryValue(List("bammetrics", "stats", "bamstats", "flagstats", "Duplicates"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val total = new SummaryValue(List("bammetrics", "stats", "bamstats", "flagstats", "All"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val secondary = new SummaryValue(List("bammetrics", "stats", "bamstats", "flagstats", "NotPrimaryAlignment"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val sb = new StringBuffer()
      if (lib.isDefined) sb.append(sample + "-" + lib.get + "\t") else sb.append(sample + "\t")
      sb.append((mapped - duplicates - secondary) + "\t")
      sb.append(duplicates + "\t")
      sb.append((total - mapped) + "\t")
      sb.append(secondary)
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
    plot.ylabel = Some("Reads")
    if (libraryLevel) {
      plot.width = Some(200 + (summary.libraries.filter(s => sampleId.getOrElse(s._1) == s._1).foldLeft(0)(_ + _._2.size) * 10))
    } else plot.width = Some(200 + (summary.samples.count(s => sampleId.getOrElse(s) == s) * 10))
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
                     summary: Summary,
                     libraryLevel: Boolean = false,
                     sampleId: Option[String] = None,
                     libId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "insert_size" -> List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "insert_size"),
      name -> List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "All_Reads.fr_count")
    )

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
                         summary: Summary,
                         libraryLevel: Boolean = false,
                         sampleId: Option[String] = None,
                         libId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "mapping_quality" -> List("bammetrics", "stats", "bamstats", "mapping_quality", "histogram", "value"),
      name -> List("bammetrics", "stats", "bamstats", "mapping_quality", "histogram", "count")
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
                   summary: Summary,
                   libraryLevel: Boolean = false,
                   sampleId: Option[String] = None,
                   libId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")

    def paths(name: String) = Map(
      "clipping" -> List("bammetrics", "stats", "bamstats", "clipping", "histogram", "value"),
      name -> List("bammetrics", "stats", "bamstats", "clipping", "histogram", "count")
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
                       summary: Summary,
                       libraryLevel: Boolean = false,
                       sampleId: Option[String] = None,
                       libId: Option[String] = None): Unit = {
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
                       summary: Summary,
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

  private def getSampleLibraries(summary: Summary,
                                 sampleId: Option[String] = None,
                                 LibId: Option[String] = None,
                                 libraryLevel: Boolean = false): List[(String, Option[String])] = {
    if (LibId.isDefined) require(sampleId.isDefined)
    if (libraryLevel || LibId.isDefined)
      for ((sample, libs) <- summary.libraries.toList; lib <- libs) yield (sample, Some(lib))
    else for ((sample, libs) <- summary.libraries.toList) yield (sample, None)
  }

  def getTableFromSummary(summary: Summary,
                          paths: Map[String, List[String]],
                          sampleId: Option[String] = None,
                          libId: Option[String] = None): Map[String, Array[Any]] = {
    val pathValues: Map[String, Array[Any]] = paths.map {
      case (key, path) =>
        val value = summary.getValueAsArray(sampleId, libId, path: _*)
        require(value.isDefined, s"Sample: $sampleId, library: $libId on path: '${path.mkString(",")}' does not exist in summary")
        key -> value.get
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
