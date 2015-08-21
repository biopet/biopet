package nl.lumc.sasc.biopet.utils.intervals

/**
 * Created by pjvanthof on 20/08/15.
 */
case class BedRecord(chr: String,
                     start: Int,
                     end: Int,
                     name: Option[String] = None,
                     score: Option[Double] = None,
                     strand: Boolean = true,
                     thickStart: Option[Int] = None,
                     thickEnd: Option[Int] = None,
                     rgbColor: Option[(Int, Int, Int)] = None,
                     blockCount: Option[Int] = None,
                     blockSizes: Array[Int] = Array(),
                     blockStarts: Array[Int] = Array()) {

  //TODO: Complete bed line output
  override def toString = {
    s"$chr\t$start\t$end"
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
      values.lift(5) match {
        case Some("-") => false
        case Some("+") => true
        case _ => throw new IllegalStateException("Strand (column 6) must be '+' or '-'")
      },
      values.lift(6).map(_.toInt),
      values.lift(7)map(_.toInt),
      values.lift(8).map(_.split(",", 3).map(_.toInt)).map(x => (x.lift(0).getOrElse(0),x.lift(1).getOrElse(0),x.lift(2).getOrElse(0))),
      values.lift(9).map(_.toInt),
      values.lift(10).map(_.split(",").map(_.toInt)).getOrElse(Array()),
      values.lift(11).map(_.split(",").map(_.toInt)).getOrElse(Array())
    )
  }
}