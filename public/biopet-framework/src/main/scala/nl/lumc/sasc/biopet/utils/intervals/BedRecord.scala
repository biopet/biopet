package nl.lumc.sasc.biopet.utils.intervals

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
                     blockSizes: Array[Int] = Array(),
                     blockStarts: Array[Int] = Array()) {

  protected[intervals] var _originals: List[BedRecord] = Nil

  def originals(nested: Boolean = false): List[BedRecord] = {
    if (_originals.isEmpty) List(this)
    else if (nested) _originals.flatMap(_.originals(true))
    else _originals
  }

  def length = end - start + 1

  lazy val exons = if (blockCount.isDefined && blockSizes.length > 0 && blockStarts.length > 0) {
    Some(BedRecordList.fromList(for (i <- 0 to blockCount.get) yield {
      val exonNumber = strand match {
        case Some(false) => blockCount.get - i
        case _           => i + 1
      }
      val record = BedRecord(chr, start + blockStarts(i), start + blockStarts(i) + blockSizes(i),
        name.map(_ + s"_exon-$exonNumber"))
      record._originals :+= this
      record
    }))
  } else None

  lazy val introns = if (blockCount.isDefined && blockSizes.length > 0 && blockStarts.length > 0) {
    Some(BedRecordList.fromList(for (i <- 0 to (blockCount.get - 1)) yield {
      val intronNumber = strand match {
        case Some(false) => blockCount.get - i
        case _           => i + 1
      }
      val record = BedRecord(chr, start + start + blockStarts(i) + blockSizes(i) + 1, start + blockStarts(i + 1) - 1,
        name.map(_ + s"_intron-$intronNumber"))
      record._originals :+= this
      record
    }))
  } else None

  override def toString = {
    def arrayToOption[T](array: Array[T]): Option[Array[T]] = {
      if (array.isEmpty) None
      else Some(array)
    }
    List(Some(chr), Some(start), Some(end),
      name, score, strand.map(if (_) "+" else "-"),
      thickStart, thickEnd, rgbColor.map(x => s"${x._1},${x._2},${x._3}"),
      blockCount, arrayToOption(blockSizes).map(_.mkString(",")), arrayToOption(blockStarts).map(_.mkString(",")))
      .takeWhile(_.isDefined)
      .flatten
      .mkString("\t")
  }
}

object BedRecord {
  def fromLine(line: String): BedRecord = {
    val values = line.split("\t")
    require(values.length >= 3)
    BedRecord(
      values(0),
      values(1).toInt,
      values(2).toInt,
      values.lift(3),
      values.lift(4).map(_.toInt),
      values.lift(5).map {
        case "-" => false
        case "+" => true
        case _ => throw new IllegalStateException("Strand (column 6) must be '+' or '-'")
      },
      values.lift(6).map(_.toInt),
      values.lift(7) map (_.toInt),
      values.lift(8).map(_.split(",", 3).map(_.toInt)).map(x => (x.headOption.getOrElse(0), x.lift(1).getOrElse(0), x.lift(2).getOrElse(0))),
      values.lift(9).map(_.toInt),
      values.lift(10).map(_.split(",").map(_.toInt)).getOrElse(Array()),
      values.lift(11).map(_.split(",").map(_.toInt)).getOrElse(Array())
    )
  }
}