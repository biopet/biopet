/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 *
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import scala.annotation.tailrec
import scala.collection.JavaConverters._

import htsjdk.samtools.fastq.{ BasicFastqWriter, FastqReader, FastqRecord }
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
  def startStatistics(fqf: FastqReader): (Int, Int, Int, Int, Map[Int, Int], Map[Int, Int], Int, Int) = {
    var (numRecords, numRecordsWithN, numBases, numBasesWithN) = (0, 0, 0, 0)

    var baseQuals: Map[Int, Int] = Map(0 -> 0)
    var readQuals: Map[Int, Int] = Map(0 -> 0)

    var readlength_min: Int = Int.MaxValue
    var readlength_max: Int = Int.MinValue

    val it = fqf.iterator.asScala

    while (it.hasNext) {
      val record = it.next()

      numRecords += 1
      numBases += record.getReadString.length

      val recordNbases: Int = record.getReadString.count(_ == 'N')
      if (recordNbases > 0) {
        numRecordsWithN += 1
        numBasesWithN += recordNbases
      }

      // compute the histogram of qualities for this read
      val qualStat = histogramReadQual(record.getBaseQualityString)
      // merge the matrixes
      baseQuals = baseQuals |+| qualStat

      // compute the per read avg quality
      val AvgReadQual = qualStat.view.map { case (k, v) => k * v }.toList.foldLeft(0)(_ + _) / qualStat.values.sum
      readQuals = readQuals |+| Map(AvgReadQual -> 1)

      readlength_max = if (record.length() > readlength_max) record.length() else readlength_max;
      readlength_min = if (record.length() < readlength_min) record.length() else readlength_min;

    }

    // set the Phred encoding
    detectPhredEncoding(baseQuals)
    // transform the keys
    baseQuals = transformPhredEncoding(baseQuals)
    readQuals = transformPhredEncoding(readQuals)

    (numRecords, numRecordsWithN, numBases, numBasesWithN, baseQuals, readQuals, readlength_max, readlength_min)
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

  def transformPhredEncoding(qualMap: Map[Int, Int]): (Map[Int, Int]) = {
    qualMap map { case (k, v) => (k - phred_correction, v) }
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)
    val (numRecords, numRecordsWithN,
      numBases, numBasesWithN,
      baseHistogram, readHistogram,
      readlength_max, readlength_min) = startStatistics(
      new FastqReader(commandArgs.fastq)
    )

    //    println("Detected encoding: " + phred_encoding)
    //    println("Apply correction: " + phred_correction)
    //    println("Number of records:" + numRecords)
    //    println("Number of N records:" + numRecordsWithN)
    //    println("Number of bases:" + numBases)
    //    println("Number of N bases:" + numBasesWithN)
    //    println(baseHistogram)
    //    println(readHistogram)

    val reportValues = List(1, 10, 20, 30, 40, 50, 60)

    val baseHistMap = reportValues map (t => t -> baseHistogram.filterKeys(k => k >= t).values.sum) toMap
    val readHistMap = reportValues map (t => t -> readHistogram.filterKeys(k => k >= t).values.sum) toMap

    //    println(baseHistMap)
    //    println(readHistMap)

    val stats = ("bases" := (
      ("num_n" := numBasesWithN) ->:
      ("num_total" := numBases) ->:
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
          ("num_with_n" := numRecordsWithN) ->:
          ("num_total" := numRecords) ->:
          ("len_min" := readlength_min) ->:
          ("len_max" := readlength_max) ->:
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
