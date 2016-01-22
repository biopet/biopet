package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.{SAMRecord, SamReaderFactory}
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.{BedRecordList, BedRecord}
import picard.annotation.{Gene, GeneAnnotationReader}

import scala.collection.JavaConversions._

/**
  * Created by pjvanthof on 22/01/16.
  */
object BaseCounter extends ToolCommand {

  case class Args(refFlat: File = null,
                  outputDir: File = null,
                  bamFile: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]("refFlat") required() valueName "<file>" action { (x, c) =>
      c.copy(refFlat = x)
    }
    opt[File]('o', "outputDir") required() valueName "<file>" action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[File]('b', "bam") required() valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) match {
      case Some(x) => x
      case _ => throw new IllegalArgumentException
    }

    //Sets picard logging level
    htsjdk.samtools.util.Log.setGlobalLogLevel(htsjdk.samtools.util.Log.LogLevel.valueOf(logger.getLevel.toString))

    logger.info("Start reading RefFlat file")
    val bamReader = SamReaderFactory.makeDefault().open(cmdArgs.bamFile)
    val geneReader = GeneAnnotationReader.loadRefFlat(cmdArgs.refFlat, bamReader.getFileHeader.getSequenceDictionary)
    bamReader.close()
    logger.info("Done reading RefFlat file")

    logger.info("Start reading bamFile")
    val counts = for (gene <- geneReader.getAll.par) yield bamToGeneCount(cmdArgs.bamFile, gene)
    logger.info("Done reading bamFile")

    //TODO: Write to files
    counts.foreach { geneCount =>
      println(geneCount.exonCounts.head.start)
      println(geneCount.gene.getName + "\t" + geneCount.counts.totalReads)
      println(geneCount.gene.getName + "\t" + geneCount.exonCounts.map(_.counts.totalReads).sum)
      println(geneCount.gene.getName + "\t" + geneCount.intronCounts.map(_.counts.totalReads).sum)
      geneCount.transcripts.foreach { transcriptCount =>
        println(transcriptCount.transcript.name + "\t" + transcriptCount.counts.senseBases)
        transcriptCount.exons.zipWithIndex.foreach { exonCounts =>
          println(transcriptCount.transcript.name + "_exon_" + exonCounts._2 + "\t" + exonCounts._1.counts.senseBases)
        }
      }
      geneCount.exonCounts.zipWithIndex.foreach { exonCounts =>
        println(geneCount.gene.getName + "_exon_" + exonCounts._2 + "\t" + exonCounts._1.counts.senseBases)
      }
    }
  }

  def bamToGeneCount(bamFile: File, gene: Gene): GeneCount = {
    val counts = new GeneCount(gene)
    val bamReader = SamReaderFactory.makeDefault().open(bamFile)

    for (record <- bamReader.queryOverlapping(gene.getContig, gene.getStart, gene.getEnd) if !record.getNotPrimaryAlignmentFlag) {
      //TODO: check if sense this is correct with normal paired end
      val sense = if (record.getReadPairedFlag && record.getSecondOfPairFlag)
        record.getReadNegativeStrandFlag != gene.isPositiveStrand
      else record.getReadNegativeStrandFlag == gene.isPositiveStrand
      counts.addRecord(record, sense)
    }

    bamReader.close()
    logger.debug(s"Done gene '${gene.getName}'")
    counts
  }

  def bamRecordBasesOverlap(samRecord: SAMRecord, start: Int, end: Int): Int = {
    samRecord.getAlignmentBlocks
      .map { block =>
        val blockStart = block.getReferenceStart
        val blockEnd = blockStart + block.getLength - 1
        if (start <= blockEnd && end >= blockStart) {
          (if (end < blockEnd) end else blockEnd) - (if (start > blockStart) start else blockStart)
        } else 0
      }.sum
  }

  def bamRecordBasesOverlap(samRecord: SAMRecord, start: Int, end: Int, counts: Counts, sense: Boolean): Int = {
    val overlap = bamRecordBasesOverlap(samRecord, start, end)
    if (overlap > 0) {
      if (sense) {
        counts.senseBases += overlap
        counts.senseReads += 1
      } else {
        counts.antiSenseBases += overlap
        counts.antiSenseReads += 1
      }
    }
    overlap
  }

  class Counts {
    var senseBases = 0L
    var antiSenseBases = 0L
    def totalBases = senseBases + antiSenseBases
    var senseReads = 0L
    var antiSenseReads = 0L
    def totalReads = senseReads + antiSenseReads
  }

  class GeneCount(val gene: Gene) {
    val counts = new Counts
    val transcripts = gene.iterator().map(new TranscriptCount(_)).toList
    def exonRegions = BedRecordList.fromList(gene.iterator()
      .flatMap(_.exons)
      .map(e => BedRecord(gene.getContig, e.start - 1, e.end)))
      .combineOverlap
    def intronRegions = BedRecordList.fromList(BedRecord(gene.getContig, gene.getStart - 1, gene.getEnd) :: exonRegions.allRecords.toList)
        .squishBed(false, false)

    val exonCounts = exonRegions.allRecords.map(e => new RegionCount(e.start + 1, e.end))
    val intronCounts = intronRegions.allRecords.map(e => new RegionCount(e.start + 1, e.end))

    def addRecord(samRecord: SAMRecord, sense: Boolean): Unit = {
      bamRecordBasesOverlap(samRecord, gene.getStart, gene.getEnd, counts, sense)
      transcripts.foreach(_.addRecord(samRecord, sense))
      exonCounts.foreach(_.addRecord(samRecord, sense))
      intronCounts.foreach(_.addRecord(samRecord, sense))
    }
  }

  class TranscriptCount(val transcript: Gene#Transcript) {
    val counts = new Counts
    val exons = transcript.exons.map(new RegionCount(_))
    def addRecord(samRecord: SAMRecord, sense: Boolean): Unit = {
      bamRecordBasesOverlap(samRecord, transcript.start, transcript.end, counts, sense)
      exons.foreach(_.addRecord(samRecord, sense))
    }
  }

  class RegionCount(val start: Int, val end: Int) {
    def this(exon: Gene#Transcript#Exon) = this(exon.start, exon.end)
    val counts = new Counts
    def addRecord(samRecord: SAMRecord, sense: Boolean): Unit = {
      bamRecordBasesOverlap(samRecord, start, end, counts, sense)
    }
  }
}