package nl.lumc.sasc.biopet.utils.intervals

import java.io.{ PrintWriter, File }

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * Created by pjvan_thof on 8/20/15.
 */
class BedRecordList(val chrRecords: Map[String, List[BedRecord]]) {
  def allRecords = for (chr <- chrRecords; record <- chr._2) yield record

  def sort = new BedRecordList(chrRecords.map(x => x._1 -> x._2.sortWith((a, b) => a.start < b.start)))

  lazy val isSorted = this == this.sort

  def overlapWith(record: BedRecord) = (if (isSorted) this else sort).chrRecords
    .getOrElse(record.chr, Nil)
    .dropWhile(_.end < record.start)
    .takeWhile(_.start <= record.end)

  def squishBed(strandSensitive: Boolean = true) = BedRecordList.fromList {
    (for ((chr, records) <- chrRecords; record <- records) yield {
      val overlaps = overlapWith(record)
        .filterNot(strandSensitive && _.strand != record.strand)
        .filterNot(_.name == record.name)
      if (overlaps.isEmpty) {
        List(record)
      } else {
        overlaps
          .foldLeft(List(record))((result, overlap) => {
          (for (r <- result) yield {
            (overlap.start < r.start, overlap.end > r.end) match {
              case (true, true) => Nil
              case (true, false) => List(r.copy(start = overlap.end + 1))
              case (false, true) => List(r.copy(end = overlap.start - 1))
              case (false, false) => List(r.copy(end = overlap.start - 1), r.copy(start = overlap.end + 1))
            }
          }).flatten
        })
      }
    }).flatten
  }

  def writeToFile(file: File): Unit = {
    val writer = new PrintWriter(file)
    allRecords.foreach(writer.println)
    writer.close()
  }
}

object BedRecordList {
  def fromList(records: Traversable[BedRecord]): BedRecordList = fromList(records.toIterator)

  def fromList(records: TraversableOnce[BedRecord]): BedRecordList = {
    val map = mutable.Map[String, List[BedRecord]]()
    for (record <- records)
      map += record.chr -> (record :: map.getOrElse(record.chr, List()))
    new BedRecordList(map.toMap)
  }

  def fromFile(bedFile: File) = {
    fromList(Source.fromFile(bedFile).getLines().map(BedRecord.fromLine(_)))
  }

  def combineOverlap(list: BedRecordList): BedRecordList = {
    new BedRecordList(for ((chr, records) <- list.sort.chrRecords) yield chr -> {
      def combineOverlap(records: List[BedRecord],
                         newRecords: ListBuffer[BedRecord] = ListBuffer()): List[BedRecord] = {
        if (records.nonEmpty) {
          val chr = records.head.chr
          val start = records.head.start
          val overlapRecords = records.takeWhile(_.start <= records.head.end)
          val end = overlapRecords.map(_.end).max

          val newRecord = BedRecord(chr, start, end)
          newRecord._originals = overlapRecords
          newRecords += newRecord
          combineOverlap(records.drop(overlapRecords.length), newRecords)
        } else newRecords.toList
      }
      combineOverlap(records)
    })
  }
}