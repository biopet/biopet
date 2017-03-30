package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.rscript.LinePlot
import nl.lumc.sasc.biopet.utils.summary.Summary

object ShivaSvCallingReport {

  val histogramBinBoundaries: Array[Int] = Array(100, 1000, 10000, 100000, 1000000, 10000000)
  val histogramPlotTicks: Array[Int] = Array(100, 1000, 10000, 100000, 1000000, 10000000, 100000000)
  val histogramText: List[String] = List("<=100bp", "0.1-1kb", "1-10kb", "10-100kb", "0.1-1Mb", "1-10Mb", ">10Mb")

  def parseSummaryForSvCounts(summary: Summary): Map[String, Map[String, Array[Any]]] = {
    var delCounts, insCounts, dupCounts, invCounts: Map[String, Array[Any]] = Map()

    for (sampleName <- summary.samples) {
      var sampleCounts: Map[String, Any] = summary.getSampleValue(sampleName, "shivasvcalling", "stats", "variantsBySizeAndType").get.asInstanceOf[Map[String, Any]]
      for ((svType, counts) <- sampleCounts.collect({ case (k, v: List[_]) => (k, v.toArray[Any]) })) {
        svType match {
          case "DEL" => delCounts = delCounts + (sampleName -> counts)
          case "INS" => insCounts = insCounts + (sampleName -> counts)
          case "DUP" => dupCounts = dupCounts + (sampleName -> counts)
          case "INV" => invCounts = invCounts + (sampleName -> counts)
        }
      }
    }

    var result: Map[String, Map[String, Array[Any]]] = Map()
    if (!delCounts.isEmpty) result = Map("DEL" -> delCounts)
    if (!insCounts.isEmpty) result = result + ("INS" -> insCounts)
    if (!dupCounts.isEmpty) result = result + ("DUP" -> dupCounts)
    if (!invCounts.isEmpty) result = result + ("INV" -> invCounts)
    result
  }

  def writeTsvFiles(sampleNames: List[String], counts: Map[String, Map[String, Array[Any]]], svTypes: List[SvTypeForReport], outFileAllTypes: String, outDir: File): Unit = {

    val tsvWriter = new PrintWriter(new File(outDir, outFileAllTypes))
    tsvWriter.print("sv_type\tsample")
    histogramText.foreach(bin => tsvWriter.print("\t" + bin))
    tsvWriter.println()

    for (sv <- svTypes) {
      val countsForSvType: Map[String, Array[Any]] = counts.get(sv.svType).get

      writeTsvFileForSvType(sv, countsForSvType, sampleNames, outDir)

      for (sampleName <- sampleNames) {
        tsvWriter.print(sv.svType + "\t" + sampleName + "\t")
        tsvWriter.println(countsForSvType.get(sampleName).get.mkString("\t"))
      }
    }
    tsvWriter.close()
  }

  def writeTsvFileForSvType(svType: SvTypeForReport, counts: Map[String, Array[Any]], sampleNames: List[String], outDir: File): Unit = {
    val tsvWriter = new PrintWriter(new File(outDir, svType.tsvFileName))

    tsvWriter.print("histogramBin")
    sampleNames.foreach(sampleName => tsvWriter.print("\t" + sampleName))
    tsvWriter.println()

    for (i <- histogramPlotTicks.indices) {
      tsvWriter.print(histogramPlotTicks(i))
      sampleNames.foreach(sampleName => tsvWriter.print("\t" + counts.get(sampleName).get(i)))
      tsvWriter.println()
    }

    tsvWriter.close()
  }

  def createPlots(svTypes: List[SvTypeForReport], outDir: File): Unit = {
    for (sv <- svTypes) {
      val tsvFile = new File(outDir, sv.tsvFileName)
      val pngFile: File = new File(outDir, sv.pngFileName)
      val plot = LinePlot(tsvFile, pngFile,
        xlabel = Some(s"${sv.displayText.substring(0, sv.displayText.length - 1)} size"),
        ylabel = Some("Number of loci"),
        title = Some(sv.displayText),
        width = 400,
        removeZero = false)
      plot.height = Some(300)
      plot.llabel = Some("Sample")
      plot.xLog10 = true
      plot.yLog10 = true
      plot.xLog10AxisTicks = histogramPlotTicks.collect({ case x => x.toString() })
      plot.xLog10AxisLabels = histogramText
      plot.runLocal()
    }

  }

}

case class SvTypeForReport(svType: String, displayText: String, tsvFileName: String, pngFileName: String)