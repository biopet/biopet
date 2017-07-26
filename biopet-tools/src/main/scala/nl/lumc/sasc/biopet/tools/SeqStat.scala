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

import java.io.{File, PrintWriter}

import htsjdk.samtools.fastq.{FastqReader, FastqRecord}
import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ConfigUtils, ToolCommand}

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.language.postfixOps

/**
  * Created by wyleung on 01/12/14.
  * Modified by pjvanthof and warindrarto on 27/06/2015
  */
object SeqStat extends ToolCommand {

  import FqEncoding._

  var phredEncoding: FqEncoding.Value = Sanger

  val reportValues = List(1, 10, 20, 30, 40, 50, 60)

  // the base quality for each position on the reads
  var quals: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()
  var nucs: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  // generate the baseHistogram and readHistogram
  var baseQualHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()
  var readQualHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  var nucleotideHistoMap: mutable.Map[Char, Long] = mutable.Map()
  private var baseQualHistoMap: mutable.Map[Int, Long] = mutable.Map(0 -> 0)
  private var readQualGTEHistoMap: mutable.Map[Int, Long] = mutable.Map(0 -> 0)

  case class Args(fastq: File = null, outputJson: Option[File] = None)

  class OptParser extends AbstractOptParser[Args](commandName) {

    head(s"""
         |$commandName - Summarize FastQ
      """.stripMargin)

    opt[File]('i', "fastq") required () unbounded () valueName "<fastq>" action { (x, c) =>
      c.copy(fastq = x)
    } validate { x =>
      if (x.exists) success else failure("FASTQ file not found")
    } text "FastQ file to generate stats from"
    opt[File]('o', "output") unbounded () valueName "<json>" action { (x, c) =>
      c.copy(outputJson = Some(x))
    } text "File to write output to, if not supplied output go to stdout"
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

  /**
    *
    * @param quals Computed quality histogram [flat]
    */
  def detectPhredEncoding(quals: mutable.ArrayBuffer[Long]): Unit = {
    // substract 1 on high value, because we start from index 0
    val qualLowBoundery = quals.takeWhile(_ == 0).length
    val qualHighBoundery = quals.length - 1

    (qualLowBoundery < 59, qualHighBoundery > 74) match {
      case (false, true) => phredEncoding = Solexa
      // TODO: check this later on
      // complex case, we cannot tell wheter this is a sanger or solexa
      // but since the qual_high_boundery exceeds any Sanger/Illumina1.8 quals, we can `assume` this is solexa
      // New @ 2016/01/26: Illumina X ten samples can contain Phred=Q42 (qual_high_boundery==75/K)
      case (true, true) => phredEncoding = Solexa
      // this is definite a sanger sequence, the lower end is sanger only
      case (true, false) => phredEncoding = Sanger
      case (_, _) => phredEncoding = Unknown
    }
  }

  // Setting up the internal storage for the statistics gathered for each read
  // 'nuc' are the nucleotides 'ACTGN', the max ASCII value for this is T, pre-init the ArrayBuffer to this value
  // as we don't expect the have other 'higher' numbered Nucleotides for now.
  case class BaseStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nucs: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill('T'.toInt + 1)(0))

  case class ReadStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nucs: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill('T'.toInt + 1)(0),
                      var withN: Long = 0L,
                      lengths: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer())

  val baseStats: mutable.ArrayBuffer[BaseStat] = mutable.ArrayBuffer()
  val readStats: ReadStat = ReadStat()

  var readLengthHistogram: mutable.Map[String, Long] = mutable.Map.empty

  /**
    * Compute the quality metric per read
    * Results are stored in baseStats and readStats
    *
    * @param record FastqRecord
    */
  def processRead(record: FastqRecord): Unit = {

    // Adjust/expand the length of baseStat case classes to the size of current
    // read if the current list is not long enough to store the data
    if (baseStats.length < record.length) {
      baseStats ++= mutable.ArrayBuffer.fill(record.length - baseStats.length)(BaseStat())
    }

    if (readStats.lengths.length <= record.length)
      readStats.lengths ++= mutable.ArrayBuffer.fill(record.length - readStats.lengths.length + 1)(
        0)

    val readQuality = record.getBaseQualityString
    val readNucleotides = record.getReadString

    readStats.lengths(record.length) += 1

    for (t <- 0 until record.length()) {
      if (baseStats(t).qual.length <= readQuality(t)) {
        baseStats(t).qual ++= mutable.ArrayBuffer.fill(
          readQuality(t).toInt - baseStats(t).qual.length + 1)(0)
      }
      baseStats(t).qual(readQuality(t)) += 1
      baseStats(t).nucs(readNucleotides(t)) += 1
      readStats.nucs(readNucleotides(t)) += 1
    }

    // implicit conversion to Int using foldLeft(0)
    val avgQual: Int = readQuality.sum / readQuality.length
    if (readStats.qual.length <= avgQual) {
      readStats.qual ++= mutable.ArrayBuffer.fill(avgQual - readStats.qual.length + 1)(0)
    }
    readStats.qual(avgQual) += 1
    if (readNucleotides.contains("N")) readStats.withN += 1L
  }

  /**
    * seqStat, the compute entrypoint where all statistics collection starts
    *
    * @param fqreader FastqReader
    * @return numReads - number of reads counted
    */
  def seqStat(fqreader: FastqReader): Long = {
    var numReads: Long = 0
    for (read <- fqreader.iterator.asScala) {
      processRead(read)
      numReads += 1
    }

    if (numReads % 1000000 == 0) {
      logger.info(s"Processed $numReads reads")
    }

    numReads
  }

  def summarize(): Unit = {
    // for every position to the max length of any read
    for (pos <- baseStats.indices) {
      // list all qualities at this particular position `pos`
      // fix the length of `quals`
      if (quals.length <= baseStats(pos).qual.length) {
        quals ++= mutable.ArrayBuffer.fill(baseStats(pos).qual.length - quals.length)(0)
      }
      if (nucs.length <= baseStats(pos).nucs.length) {
        nucs ++= mutable.ArrayBuffer.fill(baseStats(pos).nucs.length - nucs.length)(0)
      }
      // count into the quals
      baseStats(pos).qual.zipWithIndex foreach { case (value, index) => quals(index) += value }
      // count N into nucs
      baseStats(pos).nucs.zipWithIndex foreach { case (value, index) => nucs(index) += value }
    }
    detectPhredEncoding(quals)
    logger.debug(
      "Detected '" + phredEncoding.toString.toLowerCase + "' encoding in fastq file ...")

    nucleotideHistoMap = nucs.toList
      .foldLeft(mutable.Map[Char, Long]())(
        (output, nucleotideCount) => output + (output.size.toChar -> nucleotideCount)
      )
      // ensure bases: `ACTGN` is always reported even having a zero count.
      // Other chars might be counted also, these are also reported
      .retain((nucleotide, count) => count > 0 || "ACTGN".contains(nucleotide.toString))

    baseQualHistogram = quals.slice(phredEncoding.id, quals.size)
    baseQualHistogram ++= mutable.ArrayBuffer.fill(reportValues.max + 1 - baseQualHistogram.size)(
      0L)

    readQualHistogram = readStats.qual.slice(phredEncoding.id, readStats.qual.size)
    readQualHistogram ++= mutable.ArrayBuffer.fill(reportValues.max + 1 - readQualHistogram.size)(
      0L)

    readQualGTEHistoMap = readQualHistogram.indices
      .foldLeft(mutable.Map[Int, Long]())(
        (output, index) => {
          output + (output.keys.size -> readQualHistogram.slice(index, readQualHistogram.size).sum)
        }
      )

  }

  def reportMap(fastqPath: File): Map[String, Any] = {
    Map(
      ("files",
       Map(
         ("fastq", Map(("path", fastqPath.getAbsolutePath)))
       )),
      ("stats",
       Map(
         ("bases",
          Map(
            ("num_total", nucleotideHistoMap.values.sum),
            ("num_qual", baseQualHistogram.toList),
            ("nucleotides", nucleotideHistoMap.toMap)
          )),
         ("reads",
          Map(
            ("num_with_n", readStats.withN),
            ("num_total", readStats.qual.sum),
            ("len_min", readStats.lengths.takeWhile(_ == 0).length),
            ("len_max", readStats.lengths.length - 1),
            ("num_avg_qual_gte", readQualGTEHistoMap.toMap),
            ("qual_encoding", phredEncoding.toString.toLowerCase),
            ("len_histogram", readStats.lengths.toList)
          ))
       ))
    )
  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    logger.info("Start seqstat")
    seqStat(new FastqReader(commandArgs.fastq))
    summarize()
    logger.info("Seqstat done")

    val report = reportMap(commandArgs.fastq)

    commandArgs.outputJson match {
      case Some(file) =>
        val writer = new PrintWriter(file)
        writer.println(ConfigUtils.mapToJson(report))
        writer.close()
      case _ => println(ConfigUtils.mapToJson(report))
    }
  }
}

object FqEncoding extends Enumeration {
  type FqEncoding = Value
  val Sanger = Value(33, "Sanger")
  val Solexa = Value(64, "Solexa")
  val Unknown = Value(0, "Unknown")
}
