package nl.lumc.sasc.biopet.tools.bamstats

import java.io.{ File, PrintWriter }

import scala.collection.mutable

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
  def writeToTsv(file: File): Unit = {
    val writer = new PrintWriter(file)
    writer.println("value\tcount")
    counts.keys.toList.sorted.foreach(x => writer.println(s"$x\t${counts(x)}"))
    writer.close()
  }

  override def equals(other: Any): Boolean = {
    other match {
      case c: Counts[T] => this.counts == c.counts
      case _            => false
    }
  }
}

class Histogram[T](_counts: Map[T, Long] = Map[T, Long]())(implicit ord: Numeric[T]) extends Counts[T](_counts) {

}
