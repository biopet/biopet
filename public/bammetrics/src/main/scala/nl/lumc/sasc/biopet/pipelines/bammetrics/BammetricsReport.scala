package nl.lumc.sasc.biopet.pipelines.bammetrics

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.report.{ ReportBuilder, ReportPage, ReportSection }
import nl.lumc.sasc.biopet.core.summary.{ SummaryValue, Summary }
import nl.lumc.sasc.biopet.extensions.rscript.{ XYPlot, StackedBarPlot }

/**
 * Created by pjvan_thof on 3/30/15.
 */
object BammetricsReport extends ReportBuilder {
  // FIXME: Not yet finished

  val reportName = "Bam Metrics"

  def indexPage = ReportPage(Map(
    "Bam Metrics" -> bamMetricsPage
  ), List(
    "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/bamMetricsFront.ssp")
  ),
    Map()
  )

  def bamMetricsPage = ReportPage(
    Map(),
    List(
      "Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
      "Bam Stats" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/bamStats.ssp"),
      "Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp"),
      "RNA (optional)" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/rna.ssp"),
      "Target (optional)" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/target.ssp"),
      "GC Bias" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/gcBias.ssp")
    ),
    Map()
  )

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
      sb.append((mapped - duplicates) + "\t")
      sb.append(duplicates + "\t")
      sb.append((total - mapped - secondary) + "\t")
      sb.append(secondary)
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
    plot.ylabel = Some("Reads")
    plot.width = Some(1200)
    plot.title = Some("Aligned reads")
    plot.runLocal()
  }

  def insertSizePlot(outputDir: File,
                     prefix: String,
                     summary: Summary,
                     libraryLevel: Boolean = false,
                     sampleId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    if (libraryLevel) {
      tsvWriter.println((for (
        sample <- summary.samples if (sampleId.isEmpty || sampleId.get == sample);
        lib <- summary.libraries(sample)
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
      new SummaryValue(List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "content"),
        summary, Some(sample), lib).value.getOrElse(List(List("insert_size", "All_Reads.fr_count"))) match {
        case l: List[_] => {
          l.tail.foreach(_ match {
            case l: List[_] => {
              val insertSize = l.head.toString.toInt
              val count = l.tail.head.toString.toInt
              val old = map.getOrElse(insertSize, Map())
              if (libraryLevel) map += insertSize -> (old + ((s"$sample-" + lib.get) -> count))
              else map += insertSize -> (old + (sample -> count))
            }
            case _ => throw new IllegalStateException("Must be a list")
          })
        }
        case _ => throw new IllegalStateException("Must be a list")
      }
    }

    if (libraryLevel) {
      for (
        sample <- summary.samples if (sampleId.isEmpty || sampleId.get == sample);
        lib <- summary.libraries(sample)
      ) fill(sample, Some(lib))
    } else if (sampleId.isDefined) fill(sampleId.get, None)
    else summary.samples.foreach(fill(_, None))

    for ((insertSize, counts) <- map) {
      tsvWriter.print(insertSize)
      if (libraryLevel) {
        for (
          sample <- summary.samples if (sampleId.isEmpty || sampleId.get == sample);
          lib <- summary.libraries(sample)
        ) tsvWriter.print("\t" + counts.getOrElse(s"$sample-$lib", "0"))
      } else {
        for (sample <- summary.samples if (sampleId.isEmpty || sampleId.get == sample)) {
          tsvWriter.print("\t" + counts.getOrElse(sample, "0"))
        }
      }
      tsvWriter.println()
    }

    tsvWriter.close()

    val plot = new XYPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Reads")
    plot.xlabel = Some("Insertsize")
    plot.width = Some(1200)
    plot.removeZero = true
    plot.title = Some("Insertsize")
    plot.runLocal()

  }
}
