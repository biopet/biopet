package nl.lumc.sasc.biopet.tools

import java.io.{PrintWriter, File}

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
                  bamFile: File = null,
                  prefix: String = "output") extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('r', "refFlat") required() valueName "<file>" action { (x, c) =>
      c.copy(refFlat = x)
    }
    opt[File]('o', "outputDir") required() valueName "<directory>" action { (x, c) =>
      c.copy(outputDir = x)
    }
    opt[File]('b', "bam") required() valueName "<file>" action { (x, c) =>
      c.copy(bamFile = x)
    }
    opt[String]('p', "prefix") valueName "<prefix>" action { (x, c) =>
      c.copy(prefix = x)
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
    val counts = (for (gene <- geneReader.getAll.par) yield bamToGeneCount(cmdArgs.bamFile, gene)).toList
    logger.info("Done reading bamFile")

    writeGeneCounts(counts, cmdArgs.outputDir, cmdArgs.prefix)
    writeMergeExonCount(counts, cmdArgs.outputDir, cmdArgs.prefix)
    writeMergeIntronCount(counts, cmdArgs.outputDir, cmdArgs.prefix)

    //TODO: Write to files
    /*
    counts.foreach { geneCount =>
      geneCount.transcripts.foreach { transcriptCount =>
        println(transcriptCount.transcript.name + "\t" + transcriptCount.counts.senseBases)
        transcriptCount.exons.zipWithIndex.foreach { exonCounts =>
          println(transcriptCount.transcript.name + "_exon_" + exonCounts._2 + "\t" + exonCounts._1.counts.senseBases)
        }
      }
    }
    */
  }

  /**
    * This function will write all counts that are concatenated on gene level. Each line is 1 gene.
    * Exonic: then it's seen as an exon on 1 of the transcripts
    * Intronic: then it's not seen as an exon on 1 of the transcripts
    * Exonic + Intronic = Total
    */
  def writeGeneCounts(genes: List[GeneCount], outputDir: File, prefix: String): Unit = {
    val geneTotalWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.counts"))
    val geneTotalSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.sense.counts"))
    val geneTotalAntiSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.antisense.counts"))
    val geneExonicWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.exonic.counts"))
    val geneExonicSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.exonic.sense.counts"))
    val geneExonicAntiSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.exonic.antisense.counts"))
    val geneIntronicWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.intronic.counts"))
    val geneIntronicSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.intronic.sense.counts"))
    val geneIntronicAntiSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.gene.intronic.antisense.counts"))

    genes.sortBy(_.gene.getName).foreach { geneCount =>
      geneTotalWriter.println(geneCount.gene.getName + "\t" + geneCount.counts.totalBases)
      geneTotalSenseWriter.println(geneCount.gene.getName + "\t" + geneCount.counts.senseBases)
      geneTotalAntiSenseWriter.println(geneCount.gene.getName + "\t" + geneCount.counts.antiSenseBases)
      geneExonicWriter.println(geneCount.gene.getName + "\t" + geneCount.exonCounts.map(_.counts.totalBases).sum)
      geneExonicSenseWriter.println(geneCount.gene.getName + "\t" + geneCount.exonCounts.map(_.counts.senseBases).sum)
      geneExonicAntiSenseWriter.println(geneCount.gene.getName + "\t" + geneCount.exonCounts.map(_.counts.antiSenseBases).sum)
      geneIntronicWriter.println(geneCount.gene.getName + "\t" + geneCount.intronCounts.map(_.counts.totalBases).sum)
      geneIntronicSenseWriter.println(geneCount.gene.getName + "\t" + geneCount.intronCounts.map(_.counts.senseBases).sum)
      geneIntronicAntiSenseWriter.println(geneCount.gene.getName + "\t" + geneCount.intronCounts.map(_.counts.antiSenseBases).sum)
    }

    geneTotalWriter.close()
    geneTotalSenseWriter.close()
    geneTotalAntiSenseWriter.close()
    geneExonicWriter.close()
    geneExonicSenseWriter.close()
    geneExonicAntiSenseWriter.close()
    geneIntronicWriter.close()
    geneIntronicSenseWriter.close()
    geneIntronicAntiSenseWriter.close()
  }

  /**
    * This function will print all counts that exist on exonic regions,
    * each base withing the gene is only represented once but all regions are separated
    */
  def writeMergeExonCount(genes: List[GeneCount], outputDir: File, prefix: String): Unit = {
    val exonWriter = new PrintWriter(new File(outputDir, s"$prefix.base.exon.merge.counts"))
    val exonSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.exon.merge.sense.counts"))
    val exonAntiSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.exon.merge.antisense.counts"))

    genes.sortBy(_.gene.getName).foreach { geneCount =>
      geneCount.exonCounts.foreach { exonCount =>
        exonWriter.println(geneCount.gene.getName + s"_exon:${exonCount.start}-${exonCount.end}\t" + exonCount.counts.totalBases)
        exonSenseWriter.println(geneCount.gene.getName + s"_exon:${exonCount.start}-${exonCount.end}\t" + exonCount.counts.senseBases)
        exonAntiSenseWriter.println(geneCount.gene.getName + s"_exon:${exonCount.start}-${exonCount.end}\t" + exonCount.counts.antiSenseBases)
      }
    }

    exonWriter.close()
    exonSenseWriter.close()
    exonAntiSenseWriter.close()
  }

  /**
    * This function will print all counts that does *not* exist on exonic regions,
    * each base withing the gene is only represented once but all regions are separated
    */
  def writeMergeIntronCount(genes: List[GeneCount], outputDir: File, prefix: String): Unit = {
    val intronWriter = new PrintWriter(new File(outputDir, s"$prefix.base.intron.merge.counts"))
    val intronSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.intron.merge.sense.counts"))
    val intronAntiSenseWriter = new PrintWriter(new File(outputDir, s"$prefix.base.intron.merge.antisense.counts"))

    genes.sortBy(_.gene.getName).foreach { geneCount =>
      geneCount.intronCounts.foreach { intronCount =>
        intronWriter.println(geneCount.gene.getName + s"_intron:${intronCount.start}-${intronCount.end}\t" + intronCount.counts.totalBases)
        intronSenseWriter.println(geneCount.gene.getName + s"_intron:${intronCount.start}-${intronCount.end}\t" + intronCount.counts.senseBases)
        intronAntiSenseWriter.println(geneCount.gene.getName + s"_intron:${intronCount.start}-${intronCount.end}\t" + intronCount.counts.antiSenseBases)
      }
    }

    intronWriter.close()
    intronSenseWriter.close()
    intronAntiSenseWriter.close()
  }

  def samRecordStrand(samRecord: SAMRecord, gene: Gene): Boolean = {
    if (samRecord.getReadPairedFlag && samRecord.getSecondOfPairFlag)
      samRecord.getReadNegativeStrandFlag != gene.isPositiveStrand
    else samRecord.getReadNegativeStrandFlag == gene.isPositiveStrand
  }

  def bamToGeneCount(bamFile: File, gene: Gene): GeneCount = {
    val counts = new GeneCount(gene)
    val bamReader = SamReaderFactory.makeDefault().open(bamFile)

    for (record <- bamReader.queryOverlapping(gene.getContig, gene.getStart, gene.getEnd) if !record.getNotPrimaryAlignmentFlag) {
      counts.addRecord(record, samRecordStrand(record, gene))
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