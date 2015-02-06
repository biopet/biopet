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
import nl.lumc.sasc.biopet.utils.ConfigUtils

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

  val reportValues = List(1, 10, 20, 30, 40, 50, 60)

  // the base quality for each position on the reads
  var quals: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()
  var nucs: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  // generate the baseHistogram and readHistogram
  var baseHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()
  var readHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  var nucleotideHistoMap: mutable.Map[Char, Long] = mutable.Map()
  var baseQualHistoMap: mutable.Map[Int, Long] = mutable.Map(0 -> 0)
  var readQualHistoMap: mutable.Map[Int, Long] = mutable.Map(0 -> 0)

  case class Args(fastq: File = new File("")) extends AbstractArgs

  class OptParser extends AbstractOptParser {

    head(
      s"""
        |$commandName - Summarize FastQ
      """.stripMargin)

    opt[File]('i', "fastq") required () valueName "<fastq>" action { (x, c) =>
      c.copy(fastq = x)
    } validate {
      x => if (x.exists) success else failure("FASTQ file not found")
    } text "FastQ file to generate stats from"
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

  def detectPhredEncoding(quals: mutable.ArrayBuffer[Long]): Unit = {
    val l_qual = quals.takeWhile(_ == 0).length
    val h_qual = quals.length - 1

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
   */

  def procesRead(record: FastqRecord): Unit = {
    if (baseStats.length < record.length()) {
      baseStats ++= mutable.ArrayBuffer.fill(record.length - baseStats.length)(BaseStat())
    }

    val readQual = record.getBaseQualityString
    val readNucleotides = record.getReadString

    for (t <- 0 until record.length()) {
      if (baseStats(t).qual.length <= readQual(t)) {
        for (_ <- 0 to readQual(t).toInt - baseStats(t).qual.length) baseStats(t).qual.append(0)
      }
      if (baseStats(t).nuc.length <= readNucleotides(t)) {
        for (_ <- 0 to readNucleotides(t).toInt - baseStats(t).nuc.length) baseStats(t).nuc.append(0)
      }
      baseStats(t).qual(readQual(t)) += 1
      baseStats(t).nuc(readNucleotides(t)) += 1
    }

    // implicit conversion to Int using foldLeft(0)
    val avgQual: Int = (readQual.foldLeft(0)(_ + _) / readQual.length)
    if (readQuals.length <= avgQual) {
      for (_ <- 0 to avgQual - readQuals.length) readQuals.append(0)
    }
    readQuals(avgQual) += 1
  }

  def seqStat(fqreader: FastqReader): (Long) = {
    val numReads: Long = fqreader.iterator.size
    for (read <- fqreader.iterator.asScala) {
      procesRead(read)
      //      numReads += 1
    }
    numReads
  }

  def summarize(): Unit = {
    // for every position to the max length of any read
    for (pos <- 0 until baseStats.length) {
      // list all qualities at this particular position `pos`
      // fix the length of `quals`
      if (quals.length <= baseStats(pos).qual.length) {
        for (_ <- 0 until baseStats(pos).qual.length - quals.length) quals.append(0)
      }
      if (nucs.length <= baseStats(pos).nuc.length) {
        for (_ <- nucs.length until baseStats(pos).nuc.length) nucs.append(0)
      }
      // count into the quals
      baseStats(pos).qual.zipWithIndex foreach { case (value, index) => quals(index) += value }
      // count N into nucs
      baseStats(pos).nuc.zipWithIndex foreach { case (value, index) => nucs(index) += value }
    }
    detectPhredEncoding(quals)

    for (pos <- 0 until nucs.length) {
      // always export the N-nucleotide
      if (nucs(pos) > 0 || pos.toChar == 'N') {
        nucleotideHistoMap += (pos.toChar -> nucs(pos))
      }
    }

    // init baseHistogram with the bounderies of the report values
    for (pos <- 0 until reportValues.max + 1) {
      baseHistogram.append(0)
      readHistogram.append(0)
    }

    for (pos <- 0 until quals.length) {
      var key: Int = pos - phred_correction
      if (key > 0) {
        // count till the max of baseHistogram.length
        for (histokey <- 0 until key) {
          baseHistogram(histokey) += quals(pos)
        }
      }
    }

    for (pos <- 0 until baseHistogram.length) {
      baseQualHistoMap += (pos -> baseHistogram(pos))
    }

    for (pos <- 0 until readQuals.length) {
      var key: Int = pos - phred_correction
      if (key > 0) {
        // count till the max of baseHistogram.length
        for (histokey <- 0 until key) {
          readHistogram(histokey) += readQuals(pos)
        }
      }
    }

    for (pos <- 0 until readHistogram.length) {
      readQualHistoMap += (pos -> readHistogram(pos))
    }

  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    logger.info("Start")

    val reader = new FastqReader(commandArgs.fastq)
    seqStat(reader)
    summarize()

    logger.debug(nucs)
    //    logger.debug(baseStats)
    logger.info("Done")

    val report: Map[String, Any] = Map(
      ("files",
        Map(
          ("fastq", Map(
            ("path", commandArgs.fastq),
            ("checksum_sha1", "")
          )
          )
        )
      ),
      ("stats", Map(
        ("bases", Map(
          ("num_n", nucleotideHistoMap('N')),
          ("num_total", nucleotideHistoMap.values.sum),
          ("num_qual_gte", baseQualHistoMap.toMap),
          ("nucleotides", nucleotideHistoMap.toMap)
        )),
        ("reads", Map(
          ("num_with_n", 0),
          ("num_total", 0),
          ("len_min", 0),
          ("len_max", 0),
          ("num_qual_gte", readQualHistoMap.toMap),
          ("qual_encoding", phred_encoding)
        ))
      ))
    )

    val json_report: Json = ConfigUtils.mapToJson(report)
    println(json_report.spaces2)
  }
}
