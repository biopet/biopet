package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.report.{ ReportSection, ReportPage, ReportBuilder }

/**
 * Created by pjvan_thof on 3/30/15.
 */
object FlexiprepReport extends ReportBuilder {
  val reportName = "Flexiprep"

  def indexPage = {
    ReportPage(Map(), List(
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
    ), Map())
  }

  protected def fastqcPlotSection(name: String, tag: String) = {
    name -> ReportSection("/nl/lumc/sasc/biopet/pipelines/flexiprep/flexiprepFastaqcPlot.ssp", Map("plot" -> tag))
  }

  // FIXME: Not yet finished
}
