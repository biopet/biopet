package nl.lumc.sasc.biopet.tools.refflatstats

import java.io.{File, PrintWriter}

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import nl.lumc.sasc.biopet.utils.intervals.{BedRecord, BedRecordList}
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, FastaUtils, ToolCommand}
import picard.annotation.{Gene, GeneAnnotationReader}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

/**
  * Created by pjvan_thof on 1-5-17.
  */
object RefflatStats extends ToolCommand {

  case class Args(refflatFile: File = null,
                  referenceFasta: File = null,
                  geneOutput: File = null,
                  transcriptOutput: File = null,
                  exonOutput: File = null,
                  intronOutput: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('a', "annotationRefflat") required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(refflatFile = x)
    }
    opt[File]('R', "referenceFasta") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    }
    opt[File]('g', "geneOutput") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(geneOutput = x)
    }
    opt[File]('t', "transcriptOutput") required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
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

    logger.info("Reading refflat file")

    val geneReader = GeneAnnotationReader.loadRefFlat(
      cmdArgs.refflatFile,
      FastaUtils.getCachedDict(cmdArgs.referenceFasta))

    val futures = geneReader.getAll.filter(_.getName == "ENSG00000003249").map(generateGeneStats(_, cmdArgs.referenceFasta)).toList
    val totalGenes = futures.length

    logger.info(s"$totalGenes genes found in refflat file")

    val f = Future.sequence(futures)

    def waitOnFuture(future: Future[List[GeneStats]]): List[GeneStats] = {
      try {
        Await.result(future, Duration(5, "seconds"))
      } catch {
        case _: TimeoutException =>
          logger.info(futures.count(_.isCompleted) + s" / $totalGenes genes done")
          waitOnFuture(future)
      }
    }

    val geneStats = waitOnFuture(f)

    logger.info("Writing output files")

    val geneWriter = new PrintWriter(cmdArgs.geneOutput)
    val transcriptWriter = new PrintWriter(cmdArgs.transcriptOutput)
    val exonWriter = new PrintWriter(cmdArgs.exonOutput)
    val intronWriter = new PrintWriter(cmdArgs.intronOutput)

    geneWriter.println("gene\tcontig\tstart\tend\ttotalGC\texonGc\tintronGc\tlength\texonLength")
    transcriptWriter.println(
      "gene\ttranscript\tcontig\tstart\tend\ttotalGC\texonGc\tintronGc\tlength\texonLenth\tnumberOfExons")
    exonWriter.println("gene\ttranscript\tcontig\tstart\tend\tgc\tlength")
    intronWriter.println("gene\ttranscript\tcontig\tstart\tend\tgc\tlength")

    for (geneStat <- geneStats.sortBy(_.name)) {
      geneWriter.println(
        s"${geneStat.name}\t${geneStat.contig}\t${geneStat.start}\t${geneStat.end}\t${geneStat.totalGc}\t${geneStat.exonGc}\t${geneStat.intronGc
          .getOrElse(".")}\t${geneStat.length}\t${geneStat.exonLength}")
      for (transcriptStat <- geneStat.transcripts.sortBy(_.name)) {
        val exonLength = transcriptStat.exons.map(_.length).sum
        transcriptWriter.println(
          s"${geneStat.name}\t${transcriptStat.name}\t${geneStat.contig}\t" +
            s"${transcriptStat.start}\t${transcriptStat.end}\t" +
            s"${transcriptStat.totalGc}\t${transcriptStat.exonGc}\t${transcriptStat.intronGc
            .getOrElse(".")}\t${transcriptStat.length}\t$exonLength\t${transcriptStat.exons.length}")
        for (stat <- transcriptStat.exons) {
          exonWriter.println(
            s"${geneStat.name}\t${transcriptStat.name}\t${geneStat.contig}\t${stat.start}\t${stat.end}\t${stat.gc}\t${stat.length}")
        }
        for (stat <- transcriptStat.introns) {
          intronWriter.println(
            s"${geneStat.name}\t${transcriptStat.name}\t${geneStat.contig}\t${stat.start}\t${stat.end}\t${stat.gc}\t${stat.length}")
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
      geneToIntronRegions(gene).distinct.map(intron => intron -> intron.getGc(referenceFile)).toMap

    val exonicRegions = BedRecordList.fromList(exons.keys).combineOverlap
    val exonicGc = exonicRegions.getGc(referenceFile)
    val intronicRegions = BedRecordList.fromList(introns.keys).combineOverlap
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
                      transcript.start(),
                      transcript.end(),
                      gcCompleteTranscript,
                      exonicGc,
                      intronicGc,
                      exonStats,
                      intronStats)
    }

    referenceFile.close()
    GeneStats(gene.getName,
              gene.getContig,
              gene.getStart,
              gene.getEnd,
              gcCompleteGene,
              exonicGc,
              intronicGc,
              exonicRegions.length.toInt,
              transcriptStats.toArray)
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
