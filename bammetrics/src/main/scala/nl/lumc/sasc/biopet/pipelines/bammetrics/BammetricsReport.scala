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
        "Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp")) ++
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
      val mapped = new SummaryValue(List("bammetrics", "stats", "biopet_flagstat", "Mapped"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val duplicates = new SummaryValue(List("bammetrics", "stats", "biopet_flagstat", "Duplicates"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val total = new SummaryValue(List("bammetrics", "stats", "biopet_flagstat", "All"),
        summary, Some(sample), lib).value.getOrElse(0).toString.toLong
      val secondary = new SummaryValue(List("bammetrics", "stats", "biopet_flagstat", "NotPrimaryAlignment"),
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
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) {
      tsvWriter.println((for (
        sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
        lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
      ) yield s"$sample-$lib")
        .mkString("library\t", "\t", ""))
    } else {
      sampleId match {
        case Some(sample) => tsvWriter.println("\t" + sample)
        case _            => tsvWriter.println(summary.samples.mkString("Sample\t", "\t", ""))
      }
    }

    var map: Map[Int, Map[String, Int]] = Map()

    def fill(sample: String, lib: Option[String]): Unit = {

      val insertSize = new SummaryValue(List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "insert_size"),
        summary, Some(sample), lib).value.getOrElse(List())
      val counts = new SummaryValue(List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "All_Reads.fr_count"),
        summary, Some(sample), lib).value.getOrElse(List())

      (insertSize, counts) match {
        case (l: List[_], l2: List[_]) =>
          l.zip(l2).foreach(i => {
            val insertSize = i._1.toString.toInt
            val count = i._2.toString.toInt
            val old = map.getOrElse(insertSize, Map())
            if (libraryLevel) map += insertSize -> (old + ((s"$sample-" + lib.get) -> count))
            else map += insertSize -> (old + (sample -> count))
          })
        case _ => throw new IllegalStateException("Must be a list")
      }
    }

    if (libraryLevel) {
      for (
        sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
        lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
      ) fill(sample, Some(lib))
    } else if (sampleId.isDefined) fill(sampleId.get, None)
    else summary.samples.foreach(fill(_, None))

    for ((insertSize, counts) <- map) {
      tsvWriter.print(insertSize)
      if (libraryLevel) {
        for (
          sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
          lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
        ) tsvWriter.print("\t" + counts.getOrElse(s"$sample-$lib", "0"))
      } else {
        for (sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample) {
          tsvWriter.print("\t" + counts.getOrElse(sample, "0"))
        }
      }
      tsvWriter.println()
    }

    tsvWriter.close()

    LinePlot(tsvFile, outputDir,
      xlabel = Some("Insert size"),
      ylabel = Some("Reads"),
      title = Some("Insert size"),
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
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) {
      tsvWriter.println((for (
        sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
        lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
      ) yield s"$sample-$lib")
        .mkString("library\t", "\t", ""))
    } else {
      sampleId match {
        case Some(sample) => tsvWriter.println("\t" + sample)
        case _            => tsvWriter.println(summary.samples.mkString("Sample\t", "\t", ""))
      }
    }

    var map: Map[Int, Map[String, Long]] = Map()

    def fill(sample: String, lib: Option[String]): Unit = {

      val insertSize = new SummaryValue(List("bammetrics", "stats", "wgs", "histogram", "coverage"),
        summary, Some(sample), lib).value.getOrElse(List())
      val counts = new SummaryValue(List("bammetrics", "stats", "wgs", "histogram", "count"),
        summary, Some(sample), lib).value.getOrElse(List())

      (insertSize, counts) match {
        case (l: List[_], l2: List[_]) =>
          l.zip(l2).foreach(i => {
            val insertSize = i._1.toString.toInt
            val count = i._2.toString.toLong
            val old = map.getOrElse(insertSize, Map())
            if (libraryLevel) map += insertSize -> (old + ((s"$sample-" + lib.get) -> count))
            else map += insertSize -> (old + (sample -> count))
          })
        case _ => throw new IllegalStateException("Must be a list")
      }
    }

    if (libraryLevel) {
      for (
        sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
        lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
      ) fill(sample, Some(lib))
    } else if (sampleId.isDefined) fill(sampleId.get, None)
    else summary.samples.foreach(fill(_, None))

    for ((insertSize, counts) <- map) {
      tsvWriter.print(insertSize)
      if (libraryLevel) {
        for (
          sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
          lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
        ) {
          tsvWriter.print("\t" + counts.getOrElse(s"$sample-$lib", "0"))
        }
      } else {
        for (sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample) {
          tsvWriter.print("\t" + counts.getOrElse(sample, "0"))
        }
      }
      tsvWriter.println()
    }

    tsvWriter.close()

    LinePlot(tsvFile, outputDir,
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
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) {
      tsvWriter.println((for (
        sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
        lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
      ) yield s"$sample-$lib")
        .mkString("library\t", "\t", ""))
    } else {
      sampleId match {
        case Some(sample) => tsvWriter.println("\t" + sample)
        case _            => tsvWriter.println(summary.samples.mkString("Sample\t", "\t", ""))
      }
    }

    var map: Map[Int, Map[String, Double]] = Map()

    def fill(sample: String, lib: Option[String]): Unit = {

      val insertSize = new SummaryValue(List("bammetrics", "stats", "rna", "histogram", "normalized_position"),
        summary, Some(sample), lib).value.getOrElse(List())
      val counts = new SummaryValue(List("bammetrics", "stats", "rna", "histogram", "All_Reads.normalized_coverage"),
        summary, Some(sample), lib).value.getOrElse(List())

      (insertSize, counts) match {
        case (l: List[_], l2: List[_]) =>
          l.zip(l2).foreach(i => {
            val insertSize = i._1.toString.toInt
            val count = i._2.toString.toDouble
            val old = map.getOrElse(insertSize, Map())
            if (libraryLevel) map += insertSize -> (old + ((s"$sample-" + lib.get) -> count))
            else map += insertSize -> (old + (sample -> count))
          })
        case _ => throw new IllegalStateException("Must be a list")
      }
    }

    if (libraryLevel) {
      for (
        sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
        lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
      ) fill(sample, Some(lib))
    } else if (sampleId.isDefined) fill(sampleId.get, None)
    else summary.samples.foreach(fill(_, None))

    for ((insertSize, counts) <- map) {
      tsvWriter.print(insertSize)
      if (libraryLevel) {
        for (
          sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample;
          lib <- summary.libraries(sample) if libId.isEmpty || libId.get == lib
        ) {
          tsvWriter.print("\t" + counts.getOrElse(s"$sample-$lib", "0"))
        }
      } else {
        for (sample <- summary.samples if sampleId.isEmpty || sampleId.get == sample) {
          tsvWriter.print("\t" + counts.getOrElse(sample, "0"))
        }
      }
      tsvWriter.println()
    }

    tsvWriter.close()

    LinePlot(tsvFile, outputDir,
      xlabel = Some("Relative position"),
      ylabel = Some("Coverage"),
      title = Some("Rna coverage"),
      removeZero = true).runLocal()
  }
}
