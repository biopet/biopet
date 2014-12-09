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

  def transformPhredEncoding(qualMap: Map[Int, Int]): (Map[Int, Int]) = {
    qualMap map { case (k, v) => (k - phred_correction, v) }
  }

  case class BaseStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nuc: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer())

  val baseStats: mutable.ArrayBuffer[BaseStat] = mutable.ArrayBuffer()
  val readQuals: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  /**
   * Compute the quality metric per read
   * Results are stored in baseStats and readQuals
   *
   *
   *
   */

  def procesRead(record: FastqRecord): Unit = {
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

    // implicit conversion to Int using foldLeft(0)
    val avgQual: Int = (qual.foldLeft(0)(_ + _) / qual.length)
    if (readQuals.length <= avgQual) {
      for (_ <- 0 to avgQual - readQuals.length) readQuals.append(0)
    }
    readQuals(avgQual) += 1
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    logger.info("Start")
    val reader = new FastqReader(commandArgs.fastq)
    for (read <- reader.iterator.asScala) {
      procesRead(read)
    }

    val reportValues = List(1, 10, 20, 30, 40, 50, 60)

    var quals: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

    var baseHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

    for (pos <- 0 until baseStats.length) {
      // list all qualities at this particular position `pos`
      if (quals.length <= baseStats(pos).qual.length) {
        for (_ <- 0 until baseStats(pos).qual.length - quals.length) quals.append(0)
      }
      baseStats(pos).qual.zipWithIndex foreach { case (value, index) => quals(index) += value }
    }

    println(quals)
    println(quals.takeWhile(_ == 0).length)
    println(quals.length - 1)

    logger.info("Done")

    sys.exit()

    //    val baseHistMap = reportValues map (t => t -> baseHistogram.filterKeys(k => k >= t).values.sum) toMap
    //    val readHistMap = reportValues map (t => t -> readHistogram.filterKeys(k => k >= t).values.sum) toMap
    //
    //    val stats = ("bases" := (
    //      ("num_n" := readstats.n_bases) ->:
    //      ("num_total" := readstats.bases_total) ->:
    //      ("num_qual_gte" := (
    //        ("1" := baseHistMap(1)) ->:
    //        ("10" := baseHistMap(10)) ->:
    //        ("20" := baseHistMap(20)) ->:
    //        ("30" := baseHistMap(30)) ->:
    //        ("40" := baseHistMap(40)) ->:
    //        ("50" := baseHistMap(50)) ->:
    //        ("60" := baseHistMap(60)) ->:
    //        jEmptyObject)) ->: jEmptyObject)) ->:
    //        ("reads" := (
    //          ("num_with_n" := readstats.has_N) ->:
    //          ("num_total" := readstats.total_reads) ->:
    //          ("len_min" := readstats.length_min) ->:
    //          ("len_max" := readstats.length_max) ->:
    //          ("num_mean_qual_gte" := (
    //            ("1" := readHistMap(1)) ->:
    //            ("10" := readHistMap(10)) ->:
    //            ("20" := readHistMap(20)) ->:
    //            ("30" := readHistMap(30)) ->:
    //            ("40" := readHistMap(40)) ->:
    //            ("50" := readHistMap(50)) ->:
    //            ("60" := readHistMap(60)) ->:
    //            jEmptyObject)) ->: jEmptyObject)) ->:
    //            ("qual_encoding" := phred_encoding) ->:
    //            jEmptyObject
    //
    //    val json = ("stats" := stats) ->:
    //      ("files" := (
    //        "fastq" := (
    //          ("checksum_sha1" := "") ->:
    //          ("path" := commandArgs.fastq.getCanonicalPath.toString) ->:
    //          jEmptyObject)) ->:
    //          jEmptyObject) ->:
    //          jEmptyObject
    //
    //    println(json.spaces2)
  }
}
