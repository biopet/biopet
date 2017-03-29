package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.rscript.LinePlot
import nl.lumc.sasc.biopet.utils.summary.Summary

object ShivaSvCallingReport {

  def parseSummaryForSvCounts(summary: Summary): Map[String, Map[String, Array[Any]]] = {
    var result: Map[String, Map[String, Array[Any]]] = Map()
    for (sampleName <- summary.samples) {
      var sampleCounts: Map[String, Any] = summary.getSampleValue(sampleName, "shivasvcalling", "stats", "variantsBySizeAndType").get.asInstanceOf[Map[String, Any]]
      result = result + (sampleName -> sampleCounts.collect({ case (k, v: List[_]) => (k, v.toArray[Any]) }))
    }
    result
  }

  def writeTsvForPlots(counts: Map[String, Map[String, Array[Any]]], svTypes: List[SvTypeForReport], outDir: File): Unit = {
    val sampleNames = counts.keys
    for (sv <- svTypes) {
      val tsvFile = new File(outDir, sv.tsvFileName)
      val tsvWriter = new PrintWriter(tsvFile)

      tsvWriter.print("binMax")
      sampleNames.foreach(sampleName => tsvWriter.print("\t" + sampleName))
      tsvWriter.println()

      for (i <- ShivaSvCalling.histogramBinBoundaries.indices) {
        tsvWriter.print(ShivaSvCalling.histogramBinBoundaries(i))
        sampleNames.foreach(sampleName => tsvWriter.print("\t" + counts.get(sampleName).get.get(sv.svType).get(i)))
        tsvWriter.println()
      }
      val i = ShivaSvCalling.histogramBinBoundaries.length
      tsvWriter.print(ShivaSvCalling.histogramBinBoundaries(i - 1) * 10)
      sampleNames.foreach(sampleName => tsvWriter.print("\t" + counts.get(sampleName).get.get(sv.svType).get(i)))
      tsvWriter.println()

      tsvWriter.close()
    }
  }

  def createPlots(svTypes: List[SvTypeForReport], outDir: File): Unit = {
    for (sv <- svTypes) {
      val tsvFile = new File(outDir, sv.tsvFileName)
      val pngFile: File = new File(outDir, sv.pngFileName)
      val plot = LinePlot(tsvFile, pngFile,
        xlabel = Some(s"${sv.displayText} size"),
        ylabel = Some("Number of loci"),
        title = Some(sv.displayText + "s"),
        width = 600,
        removeZero = false)
      plot.llabel = Some("Sample")
      plot.xLog10 = true
      plot.yLog10 = true
      plot.runLocal()
    }

  }

}

case class SvTypeForReport(svType: String, displayText: String, tsvFileName: String, pngFileName: String)