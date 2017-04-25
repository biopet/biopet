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
package nl.lumc.sasc.biopet.tools.bamstats

import java.io.{ File, IOException, PrintWriter }

import nl.lumc.sasc.biopet.utils.rscript.LinePlot
import nl.lumc.sasc.biopet.utils.{ Logging, sortAnyAny }

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pjvanthof on 05/07/16.
 */
class Counts[T](_counts: Map[T, Long] = Map[T, Long]())(implicit ord: Ordering[T]) {
  protected[Counts] val counts: mutable.Map[T, Long] = mutable.Map() ++ _counts

  /** Returns histogram as map */
  def countsMap = counts.toMap

  /** Returns value if it does exist */
  def get(key: T) = counts.get(key)

  /** This will add an other histogram to `this` */
  def +=(other: Counts[T]): Counts[T] = {
    other.counts.foreach(x => this.counts += x._1 -> (this.counts.getOrElse(x._1, 0L) + x._2))
    this
  }

  /** With this a value can be added to the histogram */
  def add(value: T): Unit = {
    counts += value -> (counts.getOrElse(value, 0L) + 1)
  }

  /** Write histogram to a tsv/count file */
  def writeHistogramToTsv(file: File): Unit = {
    val writer = new PrintWriter(file)
    writer.println("value\tcount")
    counts.keys.toList.sorted.foreach(x => writer.println(s"$x\t${counts(x)}"))
    writer.close()
  }

  def toSummaryMap = {
    val values = counts.keySet.toList.sortWith(sortAnyAny)
    Map("values" -> values, "counts" -> values.map(counts(_)))
  }

  override def equals(other: Any): Boolean = {
    other match {
      case c: Counts[T] => this.counts == c.counts
      case _            => false
    }
  }
}

class Histogram[T](_counts: Map[T, Long] = Map[T, Long]())(implicit ord: Numeric[T]) extends Counts[T](_counts) {
  def aggregateStats: Map[String, Any] = {
    val values = this.counts.keys.toList
    val counts = this.counts.values.toList
    require(values.size == counts.size)
    if (values.nonEmpty) {
      val modal = values(counts.indexOf(counts.max))
      val totalCounts = counts.sum
      val mean: Double = values.zip(counts).map(x => ord.toDouble(x._1) * x._2).sum / totalCounts
      val median = values(values.zip(counts).zipWithIndex.sortBy(_._1._1).foldLeft((0L, 0)) {
        case (a, b) =>
          val total = a._1 + b._1._2
          if (total >= totalCounts / 2) (total, a._2)
          else (total, b._2)
      }._2)
      Map("min" -> values.min, "max" -> values.max, "median" -> median, "mean" -> mean, "modal" -> modal)
    } else Map()
  }

  /** Write histogram to a tsv/count file */
  def writeAggregateToTsv(file: File): Unit = {
    val writer = new PrintWriter(file)
    aggregateStats.foreach(x => writer.println(x._1 + "\t" + x._2))
    writer.close()
  }

  def writeFilesAndPlot(outputDir: File, prefix: String, xlabel: String, ylabel: String, title: String)(implicit ec: ExecutionContext): Unit = {
    writeHistogramToTsv(new File(outputDir, prefix + ".histogram.tsv"))
    writeAggregateToTsv(new File(outputDir, prefix + ".stats.tsv"))
    val plot = new LinePlot(null)
    plot.input = new File(outputDir, prefix + ".histogram.tsv")
    plot.output = new File(outputDir, prefix + ".histogram.png")
    plot.xlabel = Some(xlabel)
    plot.ylabel = Some(ylabel)
    plot.title = Some(title)
    try {
      plot.runLocal()
    } catch {
      // If plotting fails the tools should not fail, this depens on R to be installed
      case e: IOException => Logging.logger.warn(s"Error found while plotting ${plot.output}: ${e.getMessage}")
    }
  }

}
