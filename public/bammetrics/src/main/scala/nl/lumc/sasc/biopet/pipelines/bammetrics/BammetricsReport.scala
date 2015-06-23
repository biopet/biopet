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

  def indexPage = ReportPage(List(), List(), Map())

  def bamMetricsPage(summary: Summary, sampleId: Option[String], libId: Option[String]) = {
    val targets = (
      summary.getLibraryValue(sampleId, libId, "bammetrics", "settings", "amplicon_name"),
      summary.getLibraryValue(sampleId, libId, "bammetrics", "settings", "roi_name")
    ) match {
        case (Some(amplicon: String), Some(roi: List[_])) => amplicon :: roi.map(_.toString)
        case (_, Some(roi: List[_])) => roi.map(_.toString)
        case _ => Nil
      }

    ReportPage(
      (if (targets.isEmpty) List() else List("Targets" -> ReportPage(
        List(),
        targets.map(t => (t -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/covstatsPlot.ssp", Map("target" -> t)))),
        Map()))),
      List(
        "Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/alignmentSummary.ssp"),
        "Insert Size" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/bammetrics/insertSize.ssp", Map("showPlot" -> true))
      ),
      Map()
    )
  }

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
    if (libraryLevel) {
      plot.width = Some(200 + (summary.libraries.filter(s => sampleId.getOrElse(s._1) == s._1).foldLeft(0)(_ + _._2.size) * 10))
    } else plot.width = Some(200 + (summary.samples.filter(s => sampleId.getOrElse(s) == s).size * 10))
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

      val insertSize = new SummaryValue(List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "insert_size"),
        summary, Some(sample), lib).value.getOrElse(List())
      val counts = new SummaryValue(List("bammetrics", "stats", "CollectInsertSizeMetrics", "histogram", "All_Reads.fr_count"),
        summary, Some(sample), lib).value.getOrElse(List())

      (insertSize, counts) match {
        case (l: List[_], l2: List[_]) => {
          l.zip(l2).foreach(i => {
            val insertSize = i._1.toString.toInt
            val count = i._2.toString.toInt
            val old = map.getOrElse(insertSize, Map())
            if (libraryLevel) map += insertSize -> (old + ((s"$sample-" + lib.get) -> count))
            else map += insertSize -> (old + (sample -> count))
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
    plot.xlabel = Some("Insert size")
    plot.width = Some(1200)
    plot.removeZero = true
    plot.title = Some("Insert size")
    plot.runLocal()
  }
}
