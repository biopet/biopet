package nl.lumc.sasc.biopet.utils.intervals

import java.io.{ PrintWriter, File }

import htsjdk.samtools.reference.FastaSequenceFile

import scala.collection.JavaConversions._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import nl.lumc.sasc.biopet.core.Logging

/**
 * Created by pjvan_thof on 8/20/15.
 */
case class BedRecordList(val chrRecords: Map[String, List[BedRecord]], val header: List[String] = Nil) {
  def allRecords = for (chr <- chrRecords; record <- chr._2) yield record

  def toSamIntervals = allRecords.map(_.toSamInterval)

  lazy val sorted = {
    val sorted = new BedRecordList(chrRecords.map(x => x._1 -> x._2.sortWith((a, b) => a.start < b.start)))
    if (sorted.chrRecords.forall(x => x._2 == chrRecords(x._1))) this else sorted
  }

  lazy val isSorted = sorted.hashCode() == this.hashCode() || sorted.chrRecords.forall(x => x._2 == chrRecords(x._1))

  def overlapWith(record: BedRecord) = sorted.chrRecords
    .getOrElse(record.chr, Nil)
    .dropWhile(_.end <= record.start)
    .takeWhile(_.start < record.end)

  def length = allRecords.foldLeft(0L)((a, b) => a + b.length)

  def squishBed(strandSensitive: Boolean = true, nameSensitive: Boolean = true) = BedRecordList.fromList {
    (for ((chr, records) <- sorted.chrRecords; record <- records) yield {
      val overlaps = overlapWith(record)
        .filterNot(_ == record)
        .filterNot(strandSensitive && _.strand != record.strand)
        .filterNot(nameSensitive && _.name == record.name)
      if (overlaps.isEmpty) {
        List(record)
      } else {
        overlaps
          .foldLeft(List(record))((result, overlap) => {
            (for (r <- result) yield {
              (overlap.start <= r.start, overlap.end >= r.end) match {
                case (true, true) =>
                  Nil
                case (true, false) =>
                  List(r.copy(start = overlap.end, _originals = List(r)))
                case (false, true) =>
                  List(r.copy(end = overlap.start, _originals = List(r)))
                case (false, false) =>
                  List(r.copy(end = overlap.start, _originals = List(r)), r.copy(start = overlap.end, _originals = List(r)))
              }
            }).flatten
          })
      }
    }).flatten
  }

  def combineOverlap: BedRecordList = {
    new BedRecordList(for ((chr, records) <- sorted.chrRecords) yield chr -> {
      def combineOverlap(records: List[BedRecord],
                         newRecords: ListBuffer[BedRecord] = ListBuffer()): List[BedRecord] = {
        if (records.nonEmpty) {
          val chr = records.head.chr
          val start = records.head.start
          val overlapRecords = records.takeWhile(_.start <= records.head.end)
          val end = overlapRecords.map(_.end).max

          newRecords += BedRecord(chr, start, end, _originals = overlapRecords)
          combineOverlap(records.drop(overlapRecords.length), newRecords)
        } else newRecords.toList
      }
      combineOverlap(records)
    })
  }

  def scatter(binSize: Int) = BedRecordList(
    chrRecords.map(x => x._1 -> x._2.flatMap(_.scatter(binSize)))
  )

  def writeToFile(file: File): Unit = {
    val writer = new PrintWriter(file)
    header.foreach(writer.println)
    allRecords.foreach(writer.println)
    writer.close()
  }
}

object BedRecordList {
  def fromListWithHeader(records: Traversable[BedRecord],
                         header: List[String]): BedRecordList = fromListWithHeader(records.toIterator, header)

  def fromListWithHeader(records: TraversableOnce[BedRecord], header: List[String]): BedRecordList = {
    val map = mutable.Map[String, ListBuffer[BedRecord]]()
    for (record <- records) {
      if (!map.contains(record.chr)) map += record.chr -> ListBuffer()
      map(record.chr) += record
    }
    new BedRecordList(map.toMap.map(m => m._1 -> m._2.toList), header)
  }

  def fromList(records: Traversable[BedRecord]): BedRecordList = fromListWithHeader(records.toIterator, Nil)

  def fromList(records: TraversableOnce[BedRecord]): BedRecordList = fromListWithHeader(records, Nil)

  def fromFile(bedFile: File) = {
    val reader = Source.fromFile(bedFile)
    val all = reader.getLines().toList
    val header = all.takeWhile(x => x.startsWith("browser") || x.startsWith("track"))
    var lineCount = header.length
    val content = all.drop(lineCount)
    try {
      fromListWithHeader(content.map(line => {
        lineCount += 1
        BedRecord.fromLine(line).validate
      }), header)
    } catch {
      case e: Exception =>
        Logging.logger.warn(s"Parsing line number $lineCount failed on file: ${bedFile.getAbsolutePath}")
        throw e
    } finally {
      reader.close()
    }
  }

  def fromReference(file: File) = {
    val referenceFile = new FastaSequenceFile(file, true)

    fromList(for (contig <- referenceFile.getSequenceDictionary.getSequences) yield {
      BedRecord(contig.getSequenceName, 0, contig.getSequenceLength)
    })
  }
}