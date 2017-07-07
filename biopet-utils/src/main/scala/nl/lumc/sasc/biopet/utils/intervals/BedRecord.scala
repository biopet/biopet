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
package nl.lumc.sasc.biopet.utils.intervals

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.samtools.util.Interval
import nl.lumc.sasc.biopet.utils.FastaUtils

import scala.collection.mutable.ListBuffer

/**
  * Created by pjvanthof on 20/08/15.
  */
case class BedRecord(chr: String,
                     start: Int,
                     end: Int,
                     name: Option[String] = None,
                     score: Option[Double] = None,
                     strand: Option[Boolean] = None,
                     thickStart: Option[Int] = None,
                     thickEnd: Option[Int] = None,
                     rgbColor: Option[(Int, Int, Int)] = None,
                     blockCount: Option[Int] = None,
                     blockSizes: IndexedSeq[Int] = IndexedSeq(),
                     blockStarts: IndexedSeq[Int] = IndexedSeq(),
                     protected[intervals] val _originals: List[BedRecord] = Nil) {

  def originals(nested: Boolean = true): List[BedRecord] = {
    if (_originals.isEmpty) List(this)
    else if (nested) _originals.flatMap(_.originals(true))
    else _originals
  }

  def overlapWith(record: BedRecord): Boolean = {
    if (chr != record.chr) false
    else if (start < record.end && record.start < end) true
    else false
  }

  def length = end - start

  def scatter(binSize: Int) = {
    val binNumber = length / binSize
    if (binNumber <= 1) List(this)
    else {
      val size = length / binNumber
      val buffer = ListBuffer[BedRecord]()
      for (i <- 1 until binNumber)
        buffer += BedRecord(chr, start + ((i - 1) * size), start + (i * size))
      buffer += BedRecord(chr, start + ((binNumber - 1) * size), end)
      buffer.toList
    }
  }

  def getGc(referenceFile: IndexedFastaSequenceFile): Double = {
    FastaUtils.getSequenceGc(referenceFile, chr, start, end)
  }

  lazy val exons = if (blockCount.isDefined && blockSizes.length > 0 && blockStarts.length > 0) {
    Some(for (i <- 0 until blockCount.get) yield {
      val exonNumber = strand match {
        case Some(false) => blockCount.get - i
        case _ => i + 1
      }
      BedRecord(chr,
                start + blockStarts(i),
                start + blockStarts(i) + blockSizes(i),
                Some(s"exon-$exonNumber"),
                _originals = List(this))
    })
  } else None

  lazy val introns = if (blockCount.isDefined && blockSizes.length > 0 && blockStarts.length > 0) {
    Some(for (i <- 0 until (blockCount.get - 1)) yield {
      val intronNumber = strand match {
        case Some(false) => blockCount.get - i
        case _ => i + 1
      }
      BedRecord(chr,
                start + blockStarts(i) + blockSizes(i),
                start + blockStarts(i + 1),
                Some(s"intron-$intronNumber"),
                _originals = List(this))
    })
  } else None

  lazy val utr5 = (strand, thickStart, thickEnd) match {
    case (Some(true), Some(tStart), Some(tEnd)) if (tStart > start && tEnd < end) =>
      Some(BedRecord(chr, start, tStart, name.map(_ + "_utr5")))
    case (Some(false), Some(tStart), Some(tEnd)) if (tStart > start && tEnd < end) =>
      Some(BedRecord(chr, tEnd, end, name.map(_ + "_utr5")))
    case _ => None
  }

  lazy val utr3 = (strand, thickStart, thickEnd) match {
    case (Some(false), Some(tStart), Some(tEnd)) if (tStart > start && tEnd < end) =>
      Some(BedRecord(chr, start, tStart, name.map(_ + "_utr3")))
    case (Some(true), Some(tStart), Some(tEnd)) if (tStart > start && tEnd < end) =>
      Some(BedRecord(chr, tEnd, end, name.map(_ + "_utr3")))
    case _ => None
  }

  override def toString = {
    def arrayToOption[T](array: IndexedSeq[T]): Option[IndexedSeq[T]] = {
      if (array.isEmpty) None
      else Some(array)
    }
    List(
      Some(chr),
      Some(start),
      Some(end),
      name,
      score,
      strand.map(if (_) "+" else "-"),
      thickStart,
      thickEnd,
      rgbColor.map(x => s"${x._1},${x._2},${x._3}"),
      blockCount,
      arrayToOption(blockSizes).map(_.mkString(",")),
      arrayToOption(blockStarts).map(_.mkString(","))
    ).takeWhile(_.isDefined)
      .flatten
      .mkString("\t")
  }

  def validate = {
    require(start < end, "Start is greater then end")
    (thickStart, thickEnd) match {
      case (Some(s), Some(e)) => require(s <= e, "Thick start is greater then end")
      case _ =>
    }
    blockCount match {
      case Some(count) => {
        require(count == blockSizes.length, "Number of sizes is not the same as blockCount")
        require(count == blockStarts.length, "Number of starts is not the same as blockCount")
      }
      case _ =>
    }
    this
  }

  def toSamInterval: Interval = (name, strand) match {
    case (Some(name), Some(strand)) => new Interval(chr, start + 1, end, !strand, name)
    case (Some(name), _) => new Interval(chr, start + 1, end, false, name)
    case _ => new Interval(chr, start + 1, end)
  }
}

object BedRecord {
  def fromLine(line: String): BedRecord = {
    val values = line.split("\t")
    require(values.length >= 3, "Not enough columns count for a bed file")
    BedRecord(
      values(0),
      values(1).toInt,
      values(2).toInt,
      values.lift(3),
      values.lift(4).map(_.toDouble),
      values.lift(5).map {
        case "-" => false
        case "+" => true
        case _ => throw new IllegalStateException("Strand (column 6) must be '+' or '-'")
      },
      values.lift(6).map(_.toInt),
      values.lift(7) map (_.toInt),
      values
        .lift(8)
        .map(_.split(",", 3).map(_.toInt))
        .map(x => (x.headOption.getOrElse(0), x.lift(1).getOrElse(0), x.lift(2).getOrElse(0))),
      values.lift(9).map(_.toInt),
      values.lift(10).map(_.split(",").map(_.toInt).toIndexedSeq).getOrElse(IndexedSeq()),
      values.lift(11).map(_.split(",").map(_.toInt).toIndexedSeq).getOrElse(IndexedSeq())
    )
  }
}
