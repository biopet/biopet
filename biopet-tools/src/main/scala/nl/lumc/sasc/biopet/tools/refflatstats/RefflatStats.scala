package nl.lumc.sasc.biopet.tools.refflatstats

import java.io.{File, PrintWriter}

import htsjdk.samtools.reference.IndexedFastaSequenceFile
import nl.lumc.sasc.biopet.utils.intervals.BedRecord
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
  /**
    * Args for commandline program
    * @param refflatFile input fastq file (can be zipper)
    * @param referenceFasta output fastq file (can be zipper)
    * @param output Seq to prefix the reads with
    */
  case class Args(refflatFile: File = null, referenceFasta: File = null, output: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('a', "annotation_refflat") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(refflatFile = x)
    }
    opt[File]('R', "reference_fasta") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    }
    opt[File]('o', "output") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(output = x)
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
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    //Sets picard logging level
    htsjdk.samtools.util.Log
      .setGlobalLogLevel(htsjdk.samtools.util.Log.LogLevel.valueOf(logger.getLevel.toString))

    val geneReader = GeneAnnotationReader.loadRefFlat(
      cmdArgs.refflatFile,
      FastaUtils.getCachedDict(cmdArgs.referenceFasta))

    val geneStats = Await.result(Future.sequence(geneReader.getAll.map(generateGeneStats(_, cmdArgs.referenceFasta))), Duration.Inf)

    val writer = new PrintWriter(cmdArgs.output)
    geneStats.foreach(writer.println)
    writer.close()

    logger.info("Done")

  }

  def generateGeneStats(gene: Gene, fastaFile: File) = Future {
    val referenceFile = new IndexedFastaSequenceFile(fastaFile)
    val contig = gene.getContig
    val start = List(gene.getStart, gene.getEnd).min
    val end = List(gene.getStart, gene.getEnd).max
    val gcCompleteGene = FastaUtils.getSequenceGc(referenceFile, contig, start, end)

    val exons = (for (transcript <- gene) yield {
      for (exon <- transcript.exons) yield {
        val start = List(exon.start, exon.end).min
        val end = List(exon.start, exon.end).max
        BedRecord(contig, start, end)
      }
    }).flatten.toList.distinct.map(exon => exon -> exon.getGc(referenceFile)).toMap

    referenceFile.close()
    s"${gene.getName} - $gcCompleteGene"
  }
}
