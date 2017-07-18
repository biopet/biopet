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
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.fastq.{AsyncFastqWriter, BasicFastqWriter, FastqReader, FastqRecord}
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object FastqSync extends ToolCommand {

  /** Implicit class to allow for lazy retrieval of FastqRecord ID without any read pair mark */
  private implicit class FastqPair(fq: FastqRecord) {
    lazy val fragId: String = fq.getReadHeader.split(" ").head match {
      case x if x.endsWith(idSufixes._1) => x.stripSuffix(idSufixes._1)
      case x if x.endsWith(idSufixes._2) => x.stripSuffix(idSufixes._2)
      case x => x
    }
  }

  /**
    * Filters out FastqRecord that are not present in the input iterators, using
    * a reference sequence object
    *
    * @param pre FastqReader over reference FASTQ file
    * @param seqA FastqReader over read 1
    * @param seqB FastqReader over read 2
    * @return
    */
  def syncFastq(pre: FastqReader,
                seqA: FastqReader,
                seqB: FastqReader,
                seqOutA: AsyncFastqWriter,
                seqOutB: AsyncFastqWriter): (Long, Long, Long) = {
    // counters for discarded A and B seqections + total kept
    // NOTE: we are reasigning values to these variables in the recursion below
    var (numDiscA, numDiscB, numKept) = (0, 0, 0)

    /**
      * Syncs read pairs recursively
      *
      * @param pre Reference sequence, assumed to be a superset of both seqA and seqB
      * @param seqA Sequence over read 1
      * @param seqB Sequence over read 2
      * @return
      */
    @tailrec
    def syncIter(pre: Stream[FastqRecord],
                 seqA: Stream[FastqRecord],
                 seqB: Stream[FastqRecord]): Unit =
      (pre.headOption, seqA.headOption, seqB.headOption) match {
        // where the magic happens!
        case (Some(r), Some(a), Some(b)) =>
          val (nextA, nextB) = (a.fragId == r.fragId, b.fragId == r.fragId) match {
            // all IDs are equal to ref
            case (true, true) =>
              numKept += 1
              seqOutA.write(a)
              seqOutB.write(b)
              (seqA.tail, seqB.tail)
            // B not equal to ref and A is equal, then we discard A and progress
            case (true, false) =>
              numDiscA += 1
              (seqA.tail, seqB)
            // A not equal to ref and B is equal, then we discard B and progress
            case (false, true) =>
              numDiscB += 1
              (seqA, seqB.tail)
            case (false, false) =>
              (seqA, seqB)
          }
          syncIter(pre.tail, nextA, nextB)
        // recursion base case: both iterators have been exhausted
        case (_, None, None) => ;
        // illegal state: reference sequence exhausted but not seqA or seqB
        case (None, Some(_), _) | (None, _, Some(_)) =>
          throw new NoSuchElementException("Reference record stream shorter than expected")
        // keep recursion going if A still has items (we want to count how many)
        case (_, _, None) =>
          numDiscA += 1
          syncIter(pre.tail, seqA.tail, seqB)
        // like above but for B
        case (_, None, _) =>
          numDiscB += 1
          syncIter(pre.tail, seqA, seqB.tail)
      }

    syncIter(pre.iterator.asScala.toStream,
             seqA.iterator.asScala.toStream,
             seqB.iterator.asScala.toStream)

    (numDiscA, numDiscB, numKept)
  }

  case class Args(refFastq1: File = null,
                  refFastq2: File = null,
                  inputFastq1: File = null,
                  inputFastq2: File = null,
                  outputFastq1: File = null,
                  outputFastq2: File = null)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(s"""
        |$commandName - Sync paired-end FASTQ files.
        |
        |This tool works with gzipped or non-gzipped FASTQ files. The output
        |file will be gzipped when the input is also gzipped.
      """.stripMargin)

    opt[File]('r', "ref1") unbounded () required () valueName "<fastq>" action { (x, c) =>
      c.copy(refFastq1 = x)
    } validate { x =>
      if (x.exists) success else failure("Reference FASTQ file not found")
    } text "Reference R1 FASTQ file"

    opt[File]("ref2") unbounded () required () valueName "<fastq>" action { (x, c) =>
      c.copy(refFastq2 = x)
    } validate { x =>
      if (x.exists) success else failure("Reference FASTQ file not found")
    } text "Reference R2 FASTQ file"

    opt[File]('i', "in1") unbounded () required () valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x)
    } validate { x =>
      if (x.exists) success else failure("Input FASTQ file 1 not found")
    } text "Input FASTQ file 1"

    opt[File]('j', "in2") unbounded () required () valueName "<fastq[.gz]>" action { (x, c) =>
      c.copy(inputFastq2 = x)
    } validate { x =>
      if (x.exists) success else failure("Input FASTQ file 2 not found")
    } text "Input FASTQ file 2"

    opt[File]('o', "out1") unbounded () required () valueName "<fastq[.gz]>" action { (x, c) =>
      c.copy(outputFastq1 = x)
    } text "Output FASTQ file 1"

    opt[File]('p', "out2") unbounded () required () valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq2 = x)
    } text "Output FASTQ file 2"
  }

  /**
    * Parses the command line argument
    *
    * @param args Array of arguments
    * @return
    */
  def parseArgs(args: Array[String]): Args =
    new OptParser()
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    idSufixes = findR1R2Subfixes(commandArgs.refFastq1, commandArgs.refFastq2)

    val refReader = new FastqReader(commandArgs.refFastq1)
    val AReader = new FastqReader(commandArgs.inputFastq1)
    val BReader = new FastqReader(commandArgs.inputFastq2)
    val AWriter = new AsyncFastqWriter(new BasicFastqWriter(commandArgs.outputFastq1), 3000)
    val BWriter = new AsyncFastqWriter(new BasicFastqWriter(commandArgs.outputFastq2), 3000)

    try {
      val (numDiscA, numDiscB, numKept) = syncFastq(refReader, AReader, BReader, AWriter, BWriter)
      println(s"Filtered $numDiscA reads from first read file.")
      println(s"Filtered $numDiscB reads from second read file.")
      println(s"Synced files contain $numKept reads.")
    } finally {
      refReader.close()
      AReader.close()
      BReader.close()
      AWriter.close()
      BWriter.close()
    }
  }

  /**
    * This method will look up the unique suffix for R1 and R2
    *
    * @param fastqR1 input R1 file
    * @param fastqR2 Input R2 file
    * @return subfix for (R1, R2)
    */
  def findR1R2Subfixes(fastqR1: File, fastqR2: File): (String, String) = {
    val refReader1 = new FastqReader(fastqR1)
    val refReader2 = new FastqReader(fastqR2)
    val r1Name = refReader1.next().getReadHeader.split(" ").head
    val r2Name = refReader2.next().getReadHeader.split(" ").head
    refReader1.close()
    refReader2.close()

    val genericName = new String(r1Name.zip(r2Name).takeWhile(x => x._1 == x._2).map(_._1).toArray)

    (r1Name.stripPrefix(genericName), r2Name.stripPrefix(genericName))
  }

  /** Regex for capturing read ID ~ taking into account its read pair mark (if present) */
  private var idSufixes: (String, String) = _

}
