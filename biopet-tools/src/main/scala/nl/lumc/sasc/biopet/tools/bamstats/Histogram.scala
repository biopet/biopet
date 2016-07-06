package nl.lumc.sasc.biopet.tools.bamstats

import java.io.{ File, PrintWriter }

import scala.collection.mutable

/**
 * Created by pjvanthof on 05/07/16.
 */
class Counts[T](implicit ord: Ordering[T]) {
  protected[Counts] val counts: mutable.Map[T, Long] = mutable.Map()

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
  def writeToTsv(file: File): Unit = {
    val writer = new PrintWriter(file)
    writer.println("value\tcount")
    counts.keys.toList.sorted.foreach(x => writer.println(s"$x\t${counts(x)}"))
    writer.close()
  }
}

class Histogram[T](implicit ord: Numeric[T]) extends Counts[T] {

}
