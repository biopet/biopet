package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.report.{ ReportSection, ReportPage, ReportBuilder }
import nl.lumc.sasc.biopet.core.summary.{ SummaryValue, Summary }
import nl.lumc.sasc.biopet.extensions.rscript.StackedBarPlot

/**
 * Created by pjvan_thof on 3/30/15.
 */
object FlexiprepReport extends ReportBuilder {
  val reportName = "Flexiprep"

  def indexPage = ReportPage(List(
    "QC" -> flexiprepPage
  ), List(
    "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFront.ssp")
  ),
    Map()
  )

  def flexiprepPage = ReportPage(
    List(),
    List(
      "Read Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "Base Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp"),
      fastqcPlotSection("Base quality", "plot_per_base_quality"),
      fastqcPlotSection("Sequence quality", "plot_per_sequence_quality"),
      fastqcPlotSection("Base GC content", "plot_per_base_gc_content"),
      fastqcPlotSection("Sequence GC content", "plot_per_sequence_gc_content"),
      fastqcPlotSection("Base seqeunce content", "plot_per_base_sequence_content"),
      fastqcPlotSection("Duplication", "plot_duplication_levels"),
      fastqcPlotSection("Kmers", "plot_kmer_profiles"),
      fastqcPlotSection("Length distribution", "plot_sequence_length_distribution")
    ),
    Map()
  )

  protected def fastqcPlotSection(name: String, tag: String) = {
    name -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFastaqcPlot.ssp", Map("plot" -> tag))
  }

  // FIXME: Not yet finished

  def readSummaryPlot(outputDir: File,
                      prefix: String,
                      read: String,
                      summary: Summary,
                      sampleId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.println("Library\tAfter_QC\tClipping\tTrimming\tSynced")

    def getLine(summary: Summary, sample: String, lib: String): String = {
      val beforeTotal = new SummaryValue(List("flexiprep", "stats", "seqstat_" + read, "reads", "num_total"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong
      val afterTotal = new SummaryValue(List("flexiprep", "stats", "seqstat_" + read + "_after", "reads", "num_total"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong
      val clippingDiscardedToShort = new SummaryValue(List("flexiprep", "stats", "clipping_" + read, "num_reads_discarded_too_short"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong
      val clippingDiscardedToLong = new SummaryValue(List("flexiprep", "stats", "clipping_" + read, "num_reads_discarded_too_long"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong
      val trimmingDiscarded = new SummaryValue(List("flexiprep", "stats", "trimming", "num_reads_discarded_" + read),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(sample + "-" + lib + "\t")
      sb.append(afterTotal + "\t")
      sb.append((clippingDiscardedToShort + clippingDiscardedToLong) + "\t")
      sb.append(trimmingDiscarded + "\t")
      sb.append(beforeTotal - afterTotal - trimmingDiscarded - clippingDiscardedToShort - clippingDiscardedToLong)
      sb.toString
    }

    for (
      sample <- summary.samples if (sampleId.isEmpty || sample == sampleId.get);
      lib <- summary.libraries(sample)
    ) {
      tsvWriter.println(getLine(summary, sample, lib))
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Reads")
    plot.width = Some(200 + (summary.libraries.filter(s => sampleId.getOrElse(s._1) == s._1).foldLeft(0)(_ + _._2.size) * 10))
    plot.title = Some("QC summary on " + read + " reads")
    plot.runLocal()
  }

  def baseSummaryPlot(outputDir: File,
                      prefix: String,
                      read: String,
                      summary: Summary,
                      sampleId: Option[String] = None): Unit = {
    val tsvFile = new File(outputDir, prefix + ".tsv")
    val pngFile = new File(outputDir, prefix + ".png")
    val tsvWriter = new PrintWriter(tsvFile)
    tsvWriter.println("Library\tAfter_QC\tDiscarded")

    def getLine(summary: Summary, sample: String, lib: String): String = {
      val beforeTotal = new SummaryValue(List("flexiprep", "stats", "seqstat_" + read, "bases", "num_total"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong
      val afterTotal = new SummaryValue(List("flexiprep", "stats", "seqstat_" + read + "_after", "bases", "num_total"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(sample + "-" + lib + "\t")
      sb.append(afterTotal + "\t")
      sb.append(beforeTotal - afterTotal)
      sb.toString
    }

    for (
      sample <- summary.samples if (sampleId.isEmpty || sample == sampleId.get);
      lib <- summary.libraries(sample)
    ) {
      tsvWriter.println(getLine(summary, sample, lib))
    }

    tsvWriter.close()

    val plot = new StackedBarPlot(null)
    plot.input = tsvFile
    plot.output = pngFile
    plot.ylabel = Some("Bases")
    plot.width = Some(200 + (summary.libraries.filter(s => sampleId.getOrElse(s._1) == s._1).foldLeft(0)(_ + _._2.size) * 10))
    plot.title = Some("QC summary on " + read + " bases")
    plot.runLocal()
  }
}
