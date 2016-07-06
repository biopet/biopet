package nl.lumc.sasc.biopet.tools.bamstats

import java.io.{ File, PrintWriter }

import scala.collection.mutable

/**
 * Created by pjvanthof on 05/07/16.
 */
case class Histogram() {
  protected[Histogram] val histrogram: mutable.Map[Int, Long] = mutable.Map()

  /** This will add an other histogram to `this` */
  def +=(other: Histogram): Histogram = {
    other.histrogram.foreach(x => this.histrogram += x._1 -> (this.histrogram.getOrElse(x._1, 0L) + x._2))
    this
  }

  /** With this a value can be added to the histogram */
  def add(value: Int): Unit = {
    histrogram += value -> (histrogram.getOrElse(value, 0L) + 1)
  }

  /** Write histogram to a tsv/count file */
  def writeToTsv(file: File): Unit = {
    val writer = new PrintWriter(file)
    writer.println("value\tcount")
    histrogram.keys.toList.sorted.foreach(x => writer.println(s"$x\t${histrogram(x)}"))
    writer.close()
  }
}
