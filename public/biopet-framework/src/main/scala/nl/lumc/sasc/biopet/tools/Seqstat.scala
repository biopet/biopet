/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 *
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.postfixOps

import htsjdk.samtools.fastq.{ FastqReader, FastqRecord }
import scalaz._, Scalaz._
import argonaut._, Argonaut._

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Seqstat function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class Seqstat(val root: Configurable) extends BiopetJavaCommandLineFunction {
  javaMainClass = getClass.getName
}

object Seqstat extends ToolCommand {

  var phred_encoding: String = "sanger"
  var phred_correction: Int = 33

  case class ReadStats(length: Int, n_bases: Int, has_N: Int, avg_qual: Map[Int, Int], qualstat: Map[Int, Int],
                       total_reads: Int, length_max: Int, length_min: Int,
                       bases_total: Long)

  /**
   * Count N and quals per read
   * - numBases
   *
   * @param record FastqRecord
   */
  def analyseFastQRecord(record: FastqRecord): ReadStats = {
    var (qualstat, avg_qual, numBases, n_bases, readlength) = (Map(0 -> 0), Map(0 -> 0), 0, 0, 0)

    qualstat = histogramReadQual(record.getBaseQualityString)
    avg_qual = Map(qualstat.view.map { case (k, v) => k * v }.toList.foldLeft(0)(_ + _) / qualstat.values.sum -> 1)

    n_bases = record.getReadString.count(_ == 'N')
    readlength = record.getReadString.length

    val has_N = if (n_bases > 0) 1 else 0

    ReadStats(readlength, n_bases, has_N, avg_qual, qualstat, 1, readlength, readlength, readlength)
  }

  /**
   * Reads a FastQ file and counts:
   * - N bases/read
   * - Reads with N / file
   * - n Reads
   * - n Bases
   * - histogram of basequalities
   * - NotImplemented: histogram of avg. read qualities
   *
   * @param fqf FastqReader of the source FastQ file
   */
  def startStatistics(fqf: FastqReader): (ReadStats, Map[Int, Int], Map[Int, Int]) = {

    var baseQuals: Map[Int, Int] = Map(0 -> 0)
    var readQuals: Map[Int, Int] = Map(0 -> 0)

    val result: ReadStats = fqf.iterator
      .asScala.toList
      .map(record => analyseFastQRecord(record)).reduceLeft(statReducer)

    //    val it = fqf.iterator.asScala
    //    while (it.hasNext) {
    //      val record = it.next()
    //
    //      numRecords += 1
    //      numBases += record.getReadString.length
    //
    //      val recordNbases: Int = record.getReadString.count(_ == 'N')
    //      if (recordNbases > 0) {
    //        numRecordsWithN += 1
    //        numBasesWithN += recordNbases
    //      }
    //
    //      // compute the histogram of qualities for this read
    //      val qualStat = histogramReadQual(record.getBaseQualityString)
    //      // merge the matrixes
    //      baseQuals = baseQuals |+| qualStat
    //
    //      // compute the per read avg quality
    //      val AvgReadQual = qualStat.view.map { case (k, v) => k * v }.toList.foldLeft(0)(_ + _) / qualStat.values.sum
    //      readQuals = readQuals |+| Map(AvgReadQual -> 1)
    //
    //      readlength_max = if (record.length() > readlength_max) record.length() else readlength_max;
    //      readlength_min = if (record.length() < readlength_min) record.length() else readlength_min;
    //
    //    }

    // detect and set the Phred encoding
    detectPhredEncoding(result.qualstat)
    // transform the keys
    baseQuals = transformPhredEncoding(result.qualstat)
    readQuals = transformPhredEncoding(result.avg_qual)

    (result, baseQuals, readQuals)
  }

  /**
   * Creates histogram from a (quality) string
   * - qualString, string containing the qualities
   *
   * @param qualString String of the source FastQ file
   */
  def histogramReadQual(qualstring: String): (Map[Int, Int]) = {
    // http://langref.org/scala/maps/algorithms/histogram
    qualstring.foldLeft(Map[Int, Int]()) {
      (m, c) => m.updated(c.toInt, m.getOrElse(c.toInt, 0) + 1)
    }
  }

  case class Args(fastq: File = new File("")) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Sync paired-end FASTQ files
      """.stripMargin)

    opt[File]('i', "fastq") required () valueName "<fastq>" action { (x, c) =>
      c.copy(fastq = x)
    } validate {
      x => if (x.exists) success else failure("FASTQ file not found")
    } text "FASTQ file to generate stats from"
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

  def detectPhredEncoding(qualMap: Map[Int, Int]): Unit = {
    val l_qual = qualMap.keys.min
    val h_qual = qualMap.keys.max

    if (h_qual > 74) {
      phred_correction = 64
      phred_encoding = "solexa"
    } else if (l_qual < 59) {
      phred_correction = 33
      phred_encoding = "sanger"
    }
  }

  /**
   * http://stackoverflow.com/questions/17408880/reduce-fold-or-scan-left-right
   *
   */
  def statReducer(res: ReadStats,
                  nxt: ReadStats) = {
    // reduce the following:
    // ReadStats(length: Int, n_bases: Int, has_N: Int, avg_qual: Map[Int, Int], qualstat: Map[Int, Int],
    //            total_reads: Int, length_max: Int, length_min: Int)
    ReadStats(
      res.length + nxt.length,
      res.n_bases + nxt.n_bases,
      res.has_N + nxt.has_N,
      res.avg_qual |+| nxt.avg_qual,
      res.qualstat |+| nxt.qualstat,
      res.total_reads + nxt.total_reads,
      List(res.length_max, nxt.length_max).max,
      List(res.length_min, nxt.length_min).min,
      res.bases_total + nxt.bases_total
    )
  }

  def transformPhredEncoding(qualMap: Map[Int, Int]): (Map[Int, Int]) = {
    qualMap map { case (k, v) => (k - phred_correction, v) }
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)
    val (readstats, baseHistogram, readHistogram) = startStatistics(
      new FastqReader(commandArgs.fastq)
    )

    val reportValues = List(1, 10, 20, 30, 40, 50, 60)

    val baseHistMap = reportValues map (t => t -> baseHistogram.filterKeys(k => k >= t).values.sum) toMap
    val readHistMap = reportValues map (t => t -> readHistogram.filterKeys(k => k >= t).values.sum) toMap

    val stats = ("bases" := (
      ("num_n" := readstats.n_bases) ->:
      ("num_total" := readstats.bases_total) ->:
      ("num_qual_gte" := (
        ("1" := baseHistMap(1)) ->:
        ("10" := baseHistMap(10)) ->:
        ("20" := baseHistMap(20)) ->:
        ("30" := baseHistMap(30)) ->:
        ("40" := baseHistMap(40)) ->:
        ("50" := baseHistMap(50)) ->:
        ("60" := baseHistMap(60)) ->:
        jEmptyObject
      )) ->: jEmptyObject)) ->:
        ("reads" := (
          ("num_with_n" := readstats.has_N) ->:
          ("num_total" := readstats.total_reads) ->:
          ("len_min" := readstats.length_min) ->:
          ("len_max" := readstats.length_max) ->:
          ("num_mean_qual_gte" := (
            ("1" := readHistMap(1)) ->:
            ("10" := readHistMap(10)) ->:
            ("20" := readHistMap(20)) ->:
            ("30" := readHistMap(30)) ->:
            ("40" := readHistMap(40)) ->:
            ("50" := readHistMap(50)) ->:
            ("60" := readHistMap(60)) ->:
            jEmptyObject
          )) ->: jEmptyObject)) ->:
            ("qual_encoding" := phred_encoding) ->:
            jEmptyObject

    val json = ("stats" := stats) ->:
      ("files" := (
        "fastq" := (
          ("checksum_sha1" := "") ->:
          ("path" := commandArgs.fastq.getCanonicalPath.toString) ->:
          jEmptyObject
        )
      ) ->:
          jEmptyObject
      ) ->:
          jEmptyObject

    println(json.spaces2)
  }
}
