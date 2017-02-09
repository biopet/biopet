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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.report.{ ReportBuilderExtension, ReportBuilder, ReportPage, ReportSection }
import nl.lumc.sasc.biopet.utils.rscript.StackedBarPlot
import nl.lumc.sasc.biopet.utils.summary.{ Summary, SummaryValue }

class FlexiprepReport(val parent: Configurable) extends ReportBuilderExtension {
  def builder = FlexiprepReport
}

/**
 * Class to generate a report for [[Flexiprep]]
 *
 * Created by pjvan_thof on 3/30/15.
 */
object FlexiprepReport extends ReportBuilder {
  val reportName = "Flexiprep"

  override def pageArgs = Map("multisample" -> false)

  /** Index page for a flexiprep report */
  def indexPage = {
    val flexiprepPage = this.flexiprepPage
    ReportPage(List("Versions" -> ReportPage(List(), List("Executables" -> ReportSection("/nl/lumc/sasc/biopet/core/report/executables.ssp"
    )), Map()),
      "Files" -> ReportPage(List(), List(
        "Input fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepInputfiles.ssp"),
        "After QC fastq files" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepOutputfiles.ssp")
      ), Map())
    ), List(
      "Report" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFront.ssp")
    ) ::: flexiprepPage.sections,
      Map()
    )
  }

  /** Generate a QC report page for 1 single library, sampleId and libId must be defined in the arguments */
  def flexiprepPage: ReportPage = ReportPage(
    List(),
    List(
      "Read Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepReadSummary.ssp"),
      "Base Summary" -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepBaseSummary.ssp"),
      fastqcPlotSection("Base quality", "plot_per_base_quality"),
      fastqcPlotSection("Sequence quality", "plot_per_sequence_quality"),
      fastqcPlotSection("Base GC content", "plot_per_base_gc_content"),
      fastqcPlotSection("Sequence GC content", "plot_per_sequence_gc_content"),
      fastqcPlotSection("Base sequence content", "plot_per_base_sequence_content"),
      fastqcPlotSection("Duplication", "plot_duplication_levels"),
      fastqcPlotSection("Kmers", "plot_kmer_profiles"),
      fastqcPlotSection("Length distribution", "plot_sequence_length_distribution")
    ),
    Map()
  )

  protected def fastqcPlotSection(name: String, tag: String) = {
    name -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFastQcPlot.ssp", Map("plot" -> tag))
  }

  /**
   * Generated a stacked bar plot for reads QC
   * @param outputDir OutputDir for plot
   * @param prefix prefix for tsv and png file
   * @param read Must give "R1" or "R2"
   * @param summary Summary class
   * @param sampleId Default selects all samples, when given plot is limits on given sample
   */
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
      val afterTotal = new SummaryValue(List("flexiprep", "stats", "seqstat_" + read + "_qc", "reads", "num_total"),
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
      sample <- summary.samples if sampleId.isEmpty || sample == sampleId.get;
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

  /**
   * Generated a stacked bar plot for bases QC
   * @param outputDir OutputDir for plot
   * @param prefix prefix for tsv and png file
   * @param read Must give "R1" or "R2"
   * @param summary Summary class
   * @param sampleId Default selects all samples, when given plot is limits on given sample
   */
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
      val afterTotal = new SummaryValue(List("flexiprep", "stats", "seqstat_" + read + "_qc", "bases", "num_total"),
        summary, Some(sample), Some(lib)).value.getOrElse(0).toString.toLong

      val sb = new StringBuffer()
      sb.append(sample + "-" + lib + "\t")
      sb.append(afterTotal + "\t")
      sb.append(beforeTotal - afterTotal)
      sb.toString
    }

    for (
      sample <- summary.samples if sampleId.isEmpty || sample == sampleId.get;
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
