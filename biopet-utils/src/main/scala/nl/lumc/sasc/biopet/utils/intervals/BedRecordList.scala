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

import java.io.{File, PrintWriter}

import htsjdk.samtools.SAMSequenceDictionary
import htsjdk.samtools.reference.IndexedFastaSequenceFile
import htsjdk.samtools.util.Interval

import scala.collection.JavaConversions._
import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import nl.lumc.sasc.biopet.utils.{FastaUtils, Logging}

/**
  * Created by pjvan_thof on 8/20/15.
  */
case class BedRecordList(chrRecords: Map[String, List[BedRecord]], header: List[String] = Nil) {
  def allRecords: immutable.Iterable[BedRecord] =
    for (chr <- chrRecords; record <- chr._2) yield record

  def toSamIntervals: immutable.Iterable[Interval] = allRecords.map(_.toSamInterval)

  lazy val sorted: BedRecordList = {
    val sorted = new BedRecordList(
      chrRecords.map(x => x._1 -> x._2.sortWith((a, b) => a.start < b.start)))
    if (sorted.chrRecords.forall(x => x._2 == chrRecords(x._1))) this else sorted
  }

  lazy val isSorted: Boolean = sorted.hashCode() == this.hashCode() || sorted.chrRecords.forall(
    x => x._2 == chrRecords(x._1))

  def overlapWith(record: BedRecord): List[BedRecord] =
    sorted.chrRecords
      .getOrElse(record.chr, Nil)
      .dropWhile(_.end <= record.start)
      .takeWhile(_.start < record.end)

  def length: Long = allRecords.foldLeft(0L)((a, b) => a + b.length)

  def squishBed(strandSensitive: Boolean = true, nameSensitive: Boolean = true): BedRecordList =
    BedRecordList.fromList {
      (for ((_, records) <- sorted.chrRecords; record <- records) yield {
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
                if (r.overlapWith(overlap)) {
                  (overlap.start <= r.start, overlap.end >= r.end) match {
                    case (true, true) =>
                      Nil
                    case (true, false) =>
                      List(r.copy(start = overlap.end, _originals = List(r)))
                    case (false, true) =>
                      List(r.copy(end = overlap.start, _originals = List(r)))
                    case (false, false) =>
                      List(r.copy(end = overlap.start, _originals = List(r)),
                           r.copy(start = overlap.end, _originals = List(r)))
                  }
                } else List(r)
              }).flatten
            })
        }
      }).flatten
    }

  def combineOverlap: BedRecordList = {
    new BedRecordList(
      for ((chr, records) <- sorted.chrRecords)
        yield
          chr -> {
            def combineOverlap(
                records: List[BedRecord],
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

  def scatter(binSize: Int,
              combineContigs: Boolean = true,
              maxContigsInSingleJob: Option[Int] = None): List[List[BedRecord]] = {
    val list = allRecords
      .flatMap(_.scatter(binSize))
      .toList
      .sortBy(_.length)
      .reverse
      .foldLeft((List[List[BedRecord]](), List[BedRecord]())) {
        case ((finalList, buffer), record) =>
          if (buffer.isEmpty) (finalList, record :: buffer)
          else {
            val bufferSize = buffer.map(_.length).sum
            if (!combineContigs && buffer.head.chr != record.chr)
              (buffer :: finalList, List(record))
            else if (bufferSize < (binSize / 2) &&
                     buffer.size < maxContigsInSingleJob.getOrElse(Int.MaxValue))
              (finalList, record :: buffer)
            else (buffer :: finalList, List(record))
          }
      }
    list._2 :: list._1
  }

  def validateContigs(reference: File): BedRecordList = {
    val dict = FastaUtils.getCachedDict(reference)
    val notExisting = chrRecords.keys.filter(dict.getSequence(_) == null).toList
    require(
      notExisting.isEmpty,
      s"Contigs found in bed records but are not existing in reference: ${notExisting.mkString(",")}")
    this
  }

  def writeToFile(file: File): Unit = {
    val writer = new PrintWriter(file)
    header.foreach(writer.println)
    allRecords.foreach(writer.println)
    writer.close()
  }

  def getGc(referenceFile: IndexedFastaSequenceFile): Double = {
    allRecords.map(r => r.getGc(referenceFile) * r.length).sum / length
  }

  /** This return the fraction of the regions comparing to a length */
  def fractionOf(length: Long): Double = this.length.toDouble / length.toDouble

  /** This return the fraction of the regions comparing to a reference */
  def fractionOfReference(dict: SAMSequenceDictionary): Double =
    fractionOf(dict.getReferenceLength)

  /** This return the fraction of the regions comparing to a reference */
  def fractionOfReference(file: File): Double = fractionOfReference(FastaUtils.getCachedDict(file))
}

object BedRecordList {
  def fromListWithHeader(records: Traversable[BedRecord], header: List[String]): BedRecordList =
    fromListWithHeader(records.toIterator, header)

  def fromListWithHeader(records: TraversableOnce[BedRecord],
                         header: List[String]): BedRecordList = {
    val map = mutable.Map[String, ListBuffer[BedRecord]]()
    for (record <- records) {
      if (!map.contains(record.chr)) map += record.chr -> ListBuffer()
      map(record.chr) += record
    }
    new BedRecordList(map.toMap.map(m => m._1 -> m._2.toList), header)
  }

  def fromList(records: Traversable[BedRecord]): BedRecordList =
    fromListWithHeader(records.toIterator, Nil)

  def fromList(records: TraversableOnce[BedRecord]): BedRecordList =
    fromListWithHeader(records, Nil)

  /**
    * This creates a [[BedRecordList]] based on multiple files. This method combines overlapping regions
    *
    * @param bedFiles Input bed files
    * @return
    */
  def fromFilesCombine(bedFiles: File*): BedRecordList = {
    fromFiles(bedFiles, combine = true)
  }

  /**
    * This creates a [[BedRecordList]] based on multiple files
    *
    * @param bedFiles Input bed files
    * @param combine When true overlaping regions are merged
    * @return
    */
  def fromFiles(bedFiles: Seq[File], combine: Boolean = false): BedRecordList = {
    val list = bedFiles.foldLeft(empty)((a, b) => fromList(fromFile(b).allRecords ++ a.allRecords))
    if (combine) list.combineOverlap
    else list
  }

  /** This created a empty [[BedRecordList]] */
  def empty: BedRecordList = fromList(Nil)

  def fromFile(bedFile: File): BedRecordList = {
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
        Logging.logger.warn(
          s"Parsing line number $lineCount failed on file: ${bedFile.getAbsolutePath}")
        throw e
    } finally {
      reader.close()
    }
  }

  def fromReference(file: File): BedRecordList = fromDict(FastaUtils.getCachedDict(file))

  def fromDict(dict: SAMSequenceDictionary): BedRecordList = {
    fromList(for (contig <- dict.getSequences) yield {
      BedRecord(contig.getSequenceName, 0, contig.getSequenceLength)
    })
  }
}
