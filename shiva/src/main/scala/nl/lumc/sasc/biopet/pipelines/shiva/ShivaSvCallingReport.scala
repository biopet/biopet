package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.rscript.LinePlot
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.{ ModuleName, PipelineName, SampleName }

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ShivaSvCallingReport extends ShivaSvCallingReportTrait

trait ShivaSvCallingReportTrait extends Logging {

  val histogramBinBoundaries: Array[Int] = Array(100, 1000, 10000, 100000, 1000000, 10000000)
  val histogramPlotTicks: Array[Int] = Array(100, 1000, 10000, 100000, 1000000, 10000000, 100000000)
  val histogramText: List[String] = List("<=100bp", "0.1-1kb", "1-10kb", "10-100kb", "0.1-1Mb", "1-10Mb", ">10Mb")

  def parseSummaryForSvCounts(summary: SummaryDb, runId: Int, sampleNames: Seq[String]): Map[String, Map[String, Array[Long]]] = {
    var delCounts, insCounts, dupCounts, invCounts: Map[String, Array[Long]] = Map()

    for (sampleName <- sampleNames) {
      val sampleCounts: Map[String, Any] = Await.result(summary.getStat(runId, PipelineName("shivasvcalling"), ModuleName("parse_sv_vcf"), SampleName(sampleName)), Duration.Inf).get
      for ((svType, counts) <- sampleCounts.collect({ case (k, v: List[_]) => (k, v.toArray[Any]) })) {
        val elem: Tuple2[String, Array[Long]] = (sampleName, counts.collect({ case x: Long => x }))
        svType match {
          case "DEL" => delCounts += elem
          case "INS" => insCounts += elem
          case "DUP" => dupCounts += elem
          case "INV" => invCounts += elem
        }
      }
    }

    var result: Map[String, Map[String, Array[Long]]] = Map()
    if (delCounts.exists(elem => (elem._2.sum > 0))) result = Map("DEL" -> delCounts)
    if (insCounts.exists(elem => (elem._2.sum > 0))) result += ("INS" -> insCounts)
    if (dupCounts.exists(elem => (elem._2.sum > 0))) result += ("DUP" -> dupCounts)
    if (invCounts.exists(elem => (elem._2.sum > 0))) result += ("INV" -> invCounts)
    result
  }

  def parseSummaryForTranslocations(summary: SummaryDb, runId: Int, sampleNames: Seq[String]): Map[String, Long] = {
    var traCounts: Map[String, Long] = Map()
    for (sampleName <- sampleNames) {
      val counts: Map[String, Any] = Await.result(summary.getStat(runId, PipelineName("shivasvcalling"), ModuleName("parse_sv_vcf"), SampleName(sampleName)), Duration.Inf).get
      counts.get("TRA") match {
        case Some(c: Long) => traCounts += (sampleName -> c)
        case Some(c)       => logger.error(s"Unable to parse translocation counts from summary db for sample $sampleName (type mismatch, type in the db: ${c.getClass})")
        case _             => logger.error(s"Summary db doesn't have translocation counts for sample $sampleName")
      }

    }
    if (traCounts.exists(elem => elem._2 > 0)) traCounts else Map.empty
  }

  def writeTsvFiles(sampleNames: Seq[String], counts: Map[String, Map[String, Array[Long]]], svTypes: List[SvTypeForReport], outFileAllTypes: String, outDir: File): Unit = {

    val tsvWriter = new PrintWriter(new File(outDir, outFileAllTypes))
    tsvWriter.print("sv_type\tsample")
    histogramText.foreach(bin => tsvWriter.print("\t" + bin))
    tsvWriter.println()

    val missingCounts: Array[String] = Array.fill(ShivaSvCallingReport.histogramText.size) { "-" }

    for (sv <- svTypes) {
      val countsForSvType: Map[String, Array[Long]] = counts.getOrElse(sv.svType, Map.empty)

      if (countsForSvType.nonEmpty) {

        writeTsvFileForSvType(sv, countsForSvType, sampleNames, outDir)

        for (sampleName <- sampleNames) {
          val sampleCounts: Array[String] = countsForSvType.get(sampleName) match {
            case Some(c) => c.collect({ case x => x.toString() })
            case None => {
              logger.error(s"Internal error, missing sv counts, sample-$sampleName, sv type-${sv.svType}")
              missingCounts
            }
          }

          tsvWriter.print(sv.svType + "\t" + sampleName + "\t")
          tsvWriter.println(sampleCounts.mkString("\t"))
        }

      } else {
        logger.error(s"Internal error, skipping writing the tsv-file for sv type ${sv.svType}")
      }

    }
    tsvWriter.close()
  }

  def writeTsvFileForSvType(svType: SvTypeForReport, counts: Map[String, Array[Long]], sampleNames: Seq[String], outDir: File): Unit = {
    val tsvWriter = new PrintWriter(new File(outDir, svType.tsvFileName))

    tsvWriter.print("histogramBin")
    val samplesWithCounts: Seq[String] = sampleNames.filter(x => counts.contains(x))
    samplesWithCounts.foreach(sampleName => tsvWriter.print("\t" + sampleName))
    tsvWriter.println()

    for (i <- histogramPlotTicks.indices) {
      tsvWriter.print(histogramPlotTicks(i))
      samplesWithCounts.foreach(sampleName => tsvWriter.print("\t" + counts.get(sampleName).get(i)))
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