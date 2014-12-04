/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 *
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.parallel.mutable.ParMap
import scala.collection.mutable.{ Map => MutMap }
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

  case class ReadStats(length: Int, n_bases: Int, has_N: Int,
                       avg_qual: Map[Int, Int], qualstat: Map[Char, Int],
                       total_reads: Int, length_max: Int, length_min: Int,
                       bases_total: Long)

  /**
   * Count N and quals per read
   * - numBases
   *
   * @param record FastqRecord
   */
  def analyseFastQRecord(record: FastqRecord): ReadStats = {
    var (avg_qual, numBases, n_bases, readlength) = (Map(0 -> 0), 0, 0, 0)

    val qualstat = histogramReadQual(record.getBaseQualityString).toMap
    avg_qual = Map(qualstat.view.map { case (k, v) => k.toInt * v }.toList.foldLeft(0)(_ + _) / qualstat.values.sum -> 1)

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

    // fixed the memory problem by removing .toList after .asScala
    // currently this implementation is as fast as the while-loop version (1st or 2nd commit)
    val result: ReadStats = fqf.iterator
      .asScala
      .map(record => analyseFastQRecord(record)).reduceLeft(statReducer)

    // the qualities are in the 'char' format, convert to int first
    result.qualstat.foreach { case (key, value) => baseQuals += (key.toInt -> value) }
    //    baseQuals = result.qualstat
    readQuals = result.avg_qual

    // detect and set the Phred encoding
    detectPhredEncoding(baseQuals)
    // transform the keys
    baseQuals = transformPhredEncoding(baseQuals)
    readQuals = transformPhredEncoding(readQuals)

    (result, baseQuals, readQuals)
  }

  /**
   * Creates histogram from a (quality) string
   * - qualString, string containing the qualities
   *
   * @param qualstring String of the source FastQ file
   */
  def histogramReadQual(qualstring: String): Map[Char, Int] = {
    // http://langref.org/scala/maps/algorithms/histogram
    //    qualstring.foldLeft(Map[Int, Int]()) {
    //      (m, c) => m.updated(c.toInt, m.getOrElse(c.toInt, 0) + 1)
    //    }
    qualstring.groupBy(identity).mapValues(_.size)
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
      res.length_max max nxt.length_max,
      res.length_min min nxt.length_min,
      res.bases_total + nxt.bases_total)
  }

  def transformPhredEncoding(qualMap: Map[Int, Int]): (Map[Int, Int]) = {
    qualMap map { case (k, v) => (k - phred_correction, v) }
  }

  case class BaseStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nuc: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer())

  val baseStats: mutable.ArrayBuffer[BaseStat] = mutable.ArrayBuffer()
  val readQuals: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  def prosesRead(record: FastqRecord): Unit = {
    if (baseStats.length < record.length()) {
      baseStats ++= mutable.ArrayBuffer.fill(record.length - baseStats.length)(BaseStat())
    }

    val qual = record.getBaseQualityString
    val nuc = record.getReadString
    for (t <- 0 until record.length()) {
      if (baseStats(t).qual.length <= qual(t)) {
        for (_ <- 0 to qual(t).toInt - baseStats(t).qual.length) baseStats(t).qual.append(0)
      }
      if (baseStats(t).nuc.length <= nuc(t)) {
        for (_ <- 0 to nuc(t).toInt - baseStats(t).nuc.length) baseStats(t).nuc.append(0)
      }
      val qualLength = baseStats(t).qual.length
      val nucLength = baseStats(t).nuc.length
      baseStats(t).qual(qual(t)) += 1
      baseStats(t).nuc(nuc(t)) += 1
    }
    val avgQual: Char = (qual.foldLeft(0)(_ + _) / qual.length).toChar
    if (readQuals.length <= avgQual) {
      for (_ <- 0 to avgQual.toInt - readQuals.length) readQuals.append(0)
    }
    readQuals(avgQual) += 1
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    logger.info("Start")
    val reader = new FastqReader(commandArgs.fastq)
    for (read <- reader.iterator.asScala) {
      prosesRead(read)
    }

    logger.info("Done")

    sys.exit()

    val (readstats, baseHistogram, readHistogram) = startStatistics(
      new FastqReader(commandArgs.fastq))

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
        jEmptyObject)) ->: jEmptyObject)) ->:
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
            jEmptyObject)) ->: jEmptyObject)) ->:
            ("qual_encoding" := phred_encoding) ->:
            jEmptyObject

    val json = ("stats" := stats) ->:
      ("files" := (
        "fastq" := (
          ("checksum_sha1" := "") ->:
          ("path" := commandArgs.fastq.getCanonicalPath.toString) ->:
          jEmptyObject)) ->:
          jEmptyObject) ->:
          jEmptyObject

    println(json.spaces2)
  }
}
