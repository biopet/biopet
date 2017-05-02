package nl.lumc.sasc.biopet.tools.refflatstats

import java.io.{File, PrintWriter}

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import nl.lumc.sasc.biopet.utils.{FastaUtils, ToolCommand}
import picard.annotation.{Gene, GeneAnnotationReader}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by pjvan_thof on 1-5-17.
  */
object RefflatStats extends ToolCommand {

  case class Args(refflatFile: File = null, referenceFasta: File = null,
                  geneOutput: File = null, transcriptOutput: File = null,
                  exonOutput: File = null, intronOutput: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('a', "annotation_refflat") required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(refflatFile = x)
    }
    opt[File]('R', "reference_fasta") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    }
    opt[File]('g', "geneOutput") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(geneOutput = x)
    }
    opt[File]('t', "transcriptOutput") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(transcriptOutput = x)
    }
    opt[File]('e', "exonOutput") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(exonOutput = x)
    }
    opt[File]('i', "intronOutput") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(intronOutput = x)
    }
  }

  /**
    * Program will prefix reads with a given seq
    *
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args =
      argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    //Sets picard logging level
    htsjdk.samtools.util.Log
      .setGlobalLogLevel(htsjdk.samtools.util.Log.LogLevel.valueOf(logger.getLevel.toString))

    val geneReader = GeneAnnotationReader.loadRefFlat(
      cmdArgs.refflatFile,
      FastaUtils.getCachedDict(cmdArgs.referenceFasta))

    logger.info("Reading refflat file done")

    val futures = geneReader.getAll.map(generateGeneStats(_, cmdArgs.referenceFasta))

    val geneStats = Await.result(Future.sequence(futures), Duration.Inf)

    val geneWriter = new PrintWriter(cmdArgs.geneOutput)
    val transcriptWriter = new PrintWriter(cmdArgs.transcriptOutput)
    val exonWriter = new PrintWriter(cmdArgs.exonOutput)
    val intronWriter = new PrintWriter(cmdArgs.intronOutput)

    geneWriter.println("gene\ttotalGC\texonGc\tintronGc")
    transcriptWriter.println("gene\ttranscript\ttotalGC\texonGc\tintronGc")
    exonWriter.println("gene\ttranscript\tstart\tend\tgc")
    intronWriter.println("gene\ttranscript\tstart\tend\tgc")

    for (geneStat <- geneStats) {
      geneWriter.println(s"${geneStat.name}\t${geneStat.totalGc}\t${geneStat.exonGc}\t${geneStat.intronGc.getOrElse("")}")
      for (transcriptStat <- geneStat.transcripts) {
        transcriptWriter.println(s"${geneStat.name}\t${transcriptStat.name}\t${transcriptStat.totalGc}\t${transcriptStat.exonGc}\t${transcriptStat.intronGc.getOrElse("")}")
        for (stat <- transcriptStat.exons) {
          exonWriter.println(s"${geneStat.name}\t${transcriptStat.name}\t${stat.start}\t${stat.end}\t${stat.gc}")
        }
        for (stat <- transcriptStat.introns) {
          intronWriter.println(s"${geneStat.name}\t${transcriptStat.name}\t${stat.start}\t${stat.end}\t${stat.gc}")
        }
      }
    }

    geneWriter.close()
    transcriptWriter.close()
    exonWriter.close()

    logger.info("Done")
  }

  def generateGeneStats(gene: Gene, fastaFile: File): Future[GeneStats] = Future {
    val referenceFile = new IndexedFastaSequenceFile(fastaFile)
    val contig = gene.getContig
    val start = List(gene.getStart, gene.getEnd).min
    val end = List(gene.getStart, gene.getEnd).max
    val gcCompleteGene = FastaUtils.getSequenceGc(referenceFile, contig, start, end)

    val exons =
      geneToExonRegions(gene).distinct.map(exon => exon -> exon.getGc(referenceFile)).toMap
    val introns =
      geneToIntronRegions(gene).distinct.map(exon => exon -> exon.getGc(referenceFile)).toMap

    val exonicGc = BedRecordList.fromList(exons.map(_._1)).combineOverlap.getGc(referenceFile)
    val intronicRegions = BedRecordList.fromList(introns.map(_._1)).combineOverlap
    val intronicGc =
      if (intronicRegions.length > 0)
        Some(intronicRegions.getGc(referenceFile))
      else None

    val transcriptStats = for (transcript <- gene) yield {
      val start = List(transcript.start(), transcript.end()).min
      val end = List(transcript.start(), transcript.end()).max
      val gcCompleteTranscript = FastaUtils.getSequenceGc(referenceFile, contig, start, end)

      val exonRegions = transcriptToExonRegions(transcript)
      val intronRegions = transcriptToIntronRegions(transcript)

      val exonicGc = BedRecordList.fromList(exonRegions).combineOverlap.getGc(referenceFile)
      val intronicRegions = BedRecordList.fromList(intronRegions).combineOverlap
      val intronicGc =
        if (intronicRegions.length > 0)
          Some(intronicRegions.getGc(referenceFile))
        else None

      val exonStats = exonRegions.map(x => RegionStats(x.start, x.end, exons(x))).toArray
      val intronStats = intronRegions.map(x => RegionStats(x.start, x.end, introns(x))).toArray

      TranscriptStats(transcript.name,
                      gcCompleteTranscript,
                      exonicGc,
                      intronicGc,
                      exonStats,
                      intronStats)
    }

    referenceFile.close()
    GeneStats(gene.getName, gcCompleteGene, exonicGc, intronicGc, transcriptStats.toArray)
  }

  def geneToExonRegions(gene: Gene): List[BedRecord] = {
    (for (transcript <- gene) yield {
      transcriptToExonRegions(transcript)
    }).flatten.toList
  }

  def transcriptToExonRegions(transcript: Gene#Transcript): List[BedRecord] = {
    for (exon <- transcript.exons.toList) yield {
      val start = List(exon.start, exon.end).min
      val end = List(exon.start, exon.end).max
      BedRecord(transcript.getGene.getContig, start, end)
    }
  }

  def geneToIntronRegions(gene: Gene): List[BedRecord] = {
    (for (transcript <- gene) yield {
      transcriptToIntronRegions(transcript)
    }).flatten.toList
  }

  def transcriptToIntronRegions(transcript: Gene#Transcript): List[BedRecord] = {
    if (transcript.exons.length > 1) {
      (for (i <- 0 until (transcript.exons.length - 2)) yield {
        val intronStart = transcript.exons(i).end + 1
        val intronEnd = transcript.exons(i + 1).start - 1
        val start = List(intronStart, intronEnd).min
        val end = List(intronStart, intronEnd).max
        BedRecord(transcript.getGene.getContig, start, end)
      }).toList
    } else Nil

  }
}
