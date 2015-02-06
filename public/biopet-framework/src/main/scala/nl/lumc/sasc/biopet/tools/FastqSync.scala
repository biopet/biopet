/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 *
 * This tool is a port of a Python implementation written by Martijn Vermaat[1]
 *
 * [1] https://github.com/martijnvermaat/bio-playground/blob/master/sync-paired-end-reads/sync_paired_end_reads.py
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import scala.io.Source
import scala.util.matching.Regex

import scala.annotation.tailrec
import scala.collection.JavaConverters._

import argonaut._, Argonaut._
import scalaz._, Scalaz._
import htsjdk.samtools.fastq.{ AsyncFastqWriter, BasicFastqWriter, FastqReader, FastqRecord }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * FastqSync function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class FastqSync(val root: Configurable) extends BiopetJavaCommandLineFunction {

  javaMainClass = getClass.getName

  @Input(doc = "Original FASTQ file (read 1 or 2)", shortName = "r", required = true)
  var refFastq: File = _

  @Input(doc = "Input read 1 FASTQ file", shortName = "i", required = true)
  var inputFastq1: File = _

  @Input(doc = "Input read 2 FASTQ file", shortName = "j", required = true)
  var inputFastq2: File = _

  @Output(doc = "Output read 1 FASTQ file", shortName = "o", required = true)
  var outputFastq1: File = _

  @Output(doc = "Output read 2 FASTQ file", shortName = "p", required = true)
  var outputFastq2: File = _

  @Output(doc = "Sync statistics", required = true)
  var outputStats: File = _

  // executed command line
  override def commandLine =
    super.commandLine +
      required("-r", refFastq) +
      required("-i", inputFastq1) +
      required("-j", inputFastq2) +
      required("-o", outputFastq1) +
      required("-p", outputFastq2) + " > " +
      required(outputStats)

  // summary statistics
  def summary: Json = {

    val regex = new Regex("""Filtered (\d*) reads from first read file.
                            |Filtered (\d*) reads from second read file.
                            |Synced read files contain (\d*) reads.""".stripMargin,
      "R1", "R2", "RL")

    val (countFilteredR1, countFilteredR2, countRLeft) =
      if (outputStats.exists) {
        val text = Source
          .fromFile(outputStats)
          .getLines()
          .mkString("\n")
        regex.findFirstMatchIn(text) match {
          case None         => (0, 0, 0)
          case Some(rmatch) => (rmatch.group("R1").toInt, rmatch.group("R2").toInt, rmatch.group("RL").toInt)
        }
      } else (0, 0, 0)

    ("num_reads_discarded_R1" := countFilteredR1) ->:
      ("num_reads_discarded_R2" := countFilteredR2) ->:
      ("num_reads_kept" := countRLeft) ->:
      jEmptyObject
  }
}

object FastqSync extends ToolCommand {

  /**
   * Implicit class to allow for lazy retrieval of FastqRecord ID
   * without any read pair mark
   *
   * @param fq FastqRecord
   */
  private implicit class FastqPair(fq: FastqRecord) {
    lazy val fragId = fq.getReadHeader.split("[_/][12]\\s??|\\s")(0)
  }

  /**
   * Counts from syncing FastqRecords
   *
   * @param numDiscard1 Number of reads discarded from the initial read 1
   * @param numDiscard2 Number of reads discarded from the initial read 2
   * @param numKept Number of items in result
   */
  case class SyncCounts(numDiscard1: Int, numDiscard2: Int, numKept: Int)

  /**
   * Filters out FastqRecord that are not present in the input iterators, using
   * a reference sequence object
   *
   * @param pre FastqReader over reference FASTQ file
   * @param seqA FastqReader over read 1
   * @param seqB FastqReader over read 2
   * @return
   */
  def syncFastq(pre: FastqReader, seqA: FastqReader, seqB: FastqReader): (Stream[(FastqRecord, FastqRecord)], SyncCounts) = {
    // counters for discarded A and B seqections + total kept
    // NOTE: we are reasigning values to these variables in the recursion below
    var (numDiscA, numDiscB, numKept) = (0, 0, 0)

    /**
     * Syncs read pairs recursively
     *
     * @param pre Reference sequence, assumed to be a superset of both seqA and seqB
     * @param seqA Sequence over read 1
     * @param seqB Sequence over read 2
     * @param acc Stream containing pairs which are present in read 1 and read 2
     * @return
     */
    @tailrec def syncIter(pre: Stream[FastqRecord],
                          seqA: Stream[FastqRecord], seqB: Stream[FastqRecord],
                          acc: Stream[(FastqRecord, FastqRecord)]): Stream[(FastqRecord, FastqRecord)] =
      (pre.headOption, seqA.headOption, seqB.headOption) match {
        // recursion base case: both iterators have been exhausted
        case (_, None, None) => acc
        // illegal state: reference sequence exhausted but not seqA or seqB
        case (None, Some(_), _) | (None, _, Some(_)) =>
          throw new NoSuchElementException("Reference record stream shorter than expected")
        // keep recursion going if A still has items (we want to count how many)
        case (_, _, None) =>
          numDiscA += 1
          syncIter(pre.tail, seqA.tail, Stream(), acc)
        // like above but for B
        case (_, None, _) =>
          numDiscB += 1
          syncIter(pre.tail, Stream(), seqB.tail, acc)
        // where the magic happens!
        case (Some(r), Some(a), Some(b)) =>
          // value of A iterator in the next recursion
          val nextA =
            // hold A if its head is not equal to reference
            if (a.fragId != r.fragId) {
              if (b.fragId == r.fragId) numDiscB += 1
              seqA
              // otherwise, go to next item
            } else seqA.tail
          // like A above
          val nextB =
            if (b.fragId != r.fragId) {
              if (a.fragId == r.fragId) numDiscA += 1
              seqB
            } else seqB.tail
          // value of accumulator in the next recursion
          val nextAcc =
            // keep accumulator unchanged if any of the two post streams
            // have different elements compared to the reference stream
            if (a.fragId != r.fragId || b.fragId != r.fragId) acc
            // otherwise, grow accumulator
            else {
              numKept += 1
              acc ++ Stream((a, b))
            }
          syncIter(pre.tail, nextA, nextB, nextAcc)
      }

    (syncIter(pre.iterator.asScala.toStream, seqA.iterator.asScala.toStream, seqB.iterator.asScala.toStream,
      Stream.empty[(FastqRecord, FastqRecord)]),
      SyncCounts(numDiscA, numDiscB, numKept))
  }

  def writeSyncedFastq(sync: Stream[(FastqRecord, FastqRecord)],
                       counts: SyncCounts,
                       outputFastq1: AsyncFastqWriter,
                       outputFastq2: AsyncFastqWriter): Unit = {
    sync.foreach {
      case (rec1, rec2) =>
        outputFastq1.write(rec1)
        outputFastq2.write(rec2)
    }
    println("Filtered %d reads from first read file.".format(counts.numDiscard1))
    println("Filtered %d reads from second read file.".format(counts.numDiscard2))
    println("Synced read files contain %d reads.".format(counts.numKept))
  }

  /** Function to merge this tool's summary with summaries from other objects */
  // TODO: refactor this into the object? At least make it work on the summary object
  def mergeSummaries(jsons: List[Json]): Json = {

    val (read1FilteredCount, read2FilteredCount, readsLeftCount) = jsons
      // extract the values we require from each JSON object into tuples
      .map {
        case json =>
          (json.field("num_reads_discarded_R1").get.numberOrZero.toInt,
            json.field("num_reads_discarded_R2").get.numberOrZero.toInt,
            json.field("num_reads_kept").get.numberOrZero.toInt)
      }
      // reduce the tuples
      .reduceLeft {
        (x: (Int, Int, Int), y: (Int, Int, Int)) =>
          (x._1 + y._1, x._2 + y._2, x._3 + y._3)
      }

    ("num_reads_discarded_R1" := read1FilteredCount) ->:
      ("num_reads_discarded_R2" := read2FilteredCount) ->:
      ("num_reads_kept" := readsLeftCount) ->:
      jEmptyObject
  }

  case class Args(refFastq: File = new File(""),
                  inputFastq1: File = new File(""),
                  inputFastq2: File = new File(""),
                  outputFastq1: File = new File(""),
                  outputFastq2: File = new File("")) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    // TODO: make output format independent from input format?
    head(
      s"""
        |$commandName - Sync paired-end FASTQ files.
        |
        |This tool works with gzipped or non-gzipped FASTQ files. The output
        |file will be gzipped when the input is also gzipped.
      """.stripMargin)

    opt[File]('r', "ref") required () valueName "<fastq>" action { (x, c) =>
      c.copy(refFastq = x)
    } validate {
      x => if (x.exists) success else failure("Reference FASTQ file not found")
    } text "Reference FASTQ file"

    opt[File]('i', "in1") required () valueName "<fastq>" action { (x, c) =>
      c.copy(inputFastq1 = x)
    } validate {
      x => if (x.exists) success else failure("Input FASTQ file 1 not found")
    } text "Input FASTQ file 1"

    opt[File]('j', "in2") required () valueName "<fastq[.gz]>" action { (x, c) =>
      c.copy(inputFastq2 = x)
    } validate {
      x => if (x.exists) success else failure("Input FASTQ file 2 not found")
    } text "Input FASTQ file 2"

    opt[File]('o', "out1") required () valueName "<fastq[.gz]>" action { (x, c) =>
      c.copy(outputFastq1 = x)
    } text "Output FASTQ file 1"

    opt[File]('p', "out2") required () valueName "<fastq>" action { (x, c) =>
      c.copy(outputFastq2 = x)
    } text "Output FASTQ file 2"
  }

  /**
   * Parses the command line argument
   *
   * @param args Array of arguments
   * @return
   */
  def parseArgs(args: Array[String]): Args = new OptParser()
    .parse(args, Args())
    .getOrElse(sys.exit(1))

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    val (synced, counts) = syncFastq(
      new FastqReader(commandArgs.refFastq),
      new FastqReader(commandArgs.inputFastq1),
      new FastqReader(commandArgs.inputFastq2))

    writeSyncedFastq(synced, counts,
      // using 3000 for queue size to approximate NFS buffer
      new AsyncFastqWriter(new BasicFastqWriter(commandArgs.outputFastq1), 3000),
      new AsyncFastqWriter(new BasicFastqWriter(commandArgs.outputFastq2), 3000)
    )
  }
}
