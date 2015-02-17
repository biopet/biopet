/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wai Yi Leung <w.y.leung@lumc.nl>
 *
 */
package nl.lumc.sasc.biopet.tools

import java.io.File
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.immutable.Map
import scala.io.Source
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

  @Input(doc = "Input FASTQ", shortName = "input", required = true)
  var input: File = null

  @Output(doc = "Output JSON", shortName = "output", required = true)
  var output: File = null

  override val defaultVmem = "4G"
  memoryLimit = Option(3.0)

  override def commandLine = super.commandLine + required("-i", input) + " > " + required(output)

  def summary: Json = {
    val json = Parse.parseOption(Source.fromFile(output).mkString)
    if (json.isEmpty) jNull
    else json.get.fieldOrEmptyObject("stats")
  }
}

object FqEncoding extends Enumeration {
  type FqEncoding = Value
  val Sanger = Value(33, "Sanger")
  val Solexa = Value(64, "Solexa")
  val Unknown = Value(0, "Unknown")
}

object Seqstat extends ToolCommand {
  def apply(root: Configurable, input: File, output: File): Seqstat = {
    val seqstat = new Seqstat(root)
    seqstat.input = input
    seqstat.output = output
    seqstat
  }

  def apply(root: Configurable, fastqfile: File, outDir: String): Seqstat = {
    val seqstat = new Seqstat(root)
    val ext = fastqfile.getName.substring(fastqfile.getName.lastIndexOf("."))
    seqstat.input = fastqfile
    seqstat.output = new File(outDir + fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    seqstat
  }

  def mergeSummaries(jsons: List[Json]): Json = {
    def addJson(json: Json, total: mutable.Map[String, Long]) {
      for (key <- json.objectFieldsOrEmpty) {
        if (json.field(key).get.isObject) addJson(json.field(key).get, total)
        else if (json.field(key).get.isNumber) {
          val number = json.field(key).get.numberOrZero.toLong
          if (total.contains(key)) {
            if (key == "len_min") {
              if (total(key) > number) total(key) = number
            } else if (key == "len_max") {
              if (total(key) < number) total(key) = number
            } else total(key) += number
          } else total += (key -> number)
        }
      }
    }

    var basesTotal: mutable.Map[String, Long] = mutable.Map()
    var readsTotal: mutable.Map[String, Long] = mutable.Map()
    var encoding: Set[Json] = Set()
    for (json <- jsons) {
      encoding += json.fieldOrEmptyString("qual_encoding")

      val bases = json.fieldOrEmptyObject("bases")
      addJson(bases, basesTotal)

      val reads = json.fieldOrEmptyObject("reads")
      addJson(reads, readsTotal)
    }
    ("bases" := (
      ("num_n" := basesTotal("num_n")) ->:
      ("num_total" := basesTotal("num_total")) ->:
      ("num_qual_gte" := (
        ("1" := basesTotal("1")) ->:
        ("10" := basesTotal("10")) ->:
        ("20" := basesTotal("20")) ->:
        ("30" := basesTotal("30")) ->:
        ("40" := basesTotal("40")) ->:
        ("50" := basesTotal("50")) ->:
        ("60" := basesTotal("60")) ->:
        jEmptyObject)) ->: jEmptyObject)) ->:
        ("reads" := (
          ("num_with_n" := readsTotal("num_with_n")) ->:
          ("num_total" := readsTotal("num_total")) ->:
          ("len_min" := readsTotal("len_min")) ->:
          ("len_max" := readsTotal("len_max")) ->:
          ("num_mean_qual_gte" := (
            ("1" := readsTotal("1")) ->:
            ("10" := readsTotal("10")) ->:
            ("20" := readsTotal("20")) ->:
            ("30" := readsTotal("30")) ->:
            ("40" := readsTotal("40")) ->:
            ("50" := readsTotal("50")) ->:
            ("60" := readsTotal("60")) ->:
            jEmptyObject)) ->: jEmptyObject)) ->:
            ("qual_encoding" := encoding.head) ->:
            jEmptyObject
  }

  import FqEncoding._

  var phredEncoding: FqEncoding.Value = Sanger

  val reportValues = List(1, 10, 20, 30, 40, 50, 60)

  // the base quality for each position on the reads
  var quals: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()
  var nucs: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  // generate the baseHistogram and readHistogram
  var baseHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()
  var readHistogram: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer()

  var nucleotideHistoMap: mutable.Map[Char, Long] = mutable.Map()
  private var baseQualHistoMap: mutable.Map[Int, Long] = mutable.Map(0 -> 0)
  private var readQualHistoMap: mutable.Map[Int, Long] = mutable.Map(0 -> 0)

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

  /**
   *
   * @param quals Computed quality histogram [flat]
   */
  def detectPhredEncoding(quals: mutable.ArrayBuffer[Long]): Unit = {
    // substract 1 on high value, because we start from index 0
    val l_qual = quals.takeWhile(_ == 0).length
    val h_qual = quals.length - 1

    (l_qual < 59, h_qual > 74) match {
      case (false, true) => {
        phredEncoding = Solexa
      }
      case (true, true) => {
        // TODO: check this later on
        // complex case, we cannot tell wheter this is a sanger or solexa
        // but since the h_qual exceeds any Sanger/Illumina1.8 quals, we can `assume` this is solexa
        phredEncoding = Solexa
      }
      case (true, false) => {
        // this is definite a sanger sequence, the lower end is sanger only
        phredEncoding = Sanger
      }
      case (_, _) => {
        phredEncoding = Unknown
      }
    }
  }

  // Setting up the internal storage for the statistics gathered for each read
  // 'nuc' are the nucleotides 'ACTGN', the max ASCII value for this is T, pre-init the ArrayBuffer to this value
  // as we don't expect the have other 'higher' numbered Nucleotides for now.
  case class BaseStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nuc: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill('T'.toInt + 1)(0))

  case class ReadStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nuc: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill('T'.toInt + 1)(0),
                      var withN: Long,
                      lengths: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer())

  val baseStats: mutable.ArrayBuffer[BaseStat] = mutable.ArrayBuffer()
  val readStats: ReadStat = new ReadStat(mutable.ArrayBuffer(),
    mutable.ArrayBuffer.fill('T'.toInt + 1)(0),
    0L,
    mutable.ArrayBuffer())

  /**
   * Compute the quality metric per read
   * Results are stored in baseStats and readStats
   *
   * @param record FastqRecord
   */
  def procesRead(record: FastqRecord): Unit = {

    // Adjust/expand the length of baseStat case classes to the size of current
    // read if the current list is not long enough to store the data
    if (baseStats.length < record.length) {
      baseStats ++= mutable.ArrayBuffer.fill(record.length - baseStats.length)(BaseStat())
    }

    if (readStats.lengths.length < record.length) {
      readStats.lengths ++= mutable.ArrayBuffer.fill(record.length - readStats.lengths.length + 1)(0)
    }

    val readQual = record.getBaseQualityString
    val readNucleotides = record.getReadString

    readStats.lengths(record.length) += 1

    for (t <- 0 until record.length()) {
      if (baseStats(t).qual.length <= readQual(t)) {
        baseStats(t).qual ++= mutable.ArrayBuffer.fill(readQual(t).toInt - baseStats(t).qual.length + 1)(0)
      }
      baseStats(t).qual(readQual(t)) += 1
      baseStats(t).nuc(readNucleotides(t)) += 1
      readStats.nuc(readNucleotides(t)) += 1
    }

    // implicit conversion to Int using foldLeft(0)
    val avgQual: Int = (readQual.sum / readQual.length)
    if (readStats.qual.length <= avgQual) {
      readStats.qual ++= mutable.ArrayBuffer.fill(avgQual - readStats.qual.length + 1)(0)
    }
    readStats.qual(avgQual) += 1
    readStats.withN += {
      if (readNucleotides.contains("N")) 1L
      else 0L
    }
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
      procesRead(read)
      numReads += 1
    }
    numReads
  }

  def summarize(): Unit = {
    // for every position to the max length of any read
    for (pos <- 0 until baseStats.length) {
      // list all qualities at this particular position `pos`
      // fix the length of `quals`
      if (quals.length <= baseStats(pos).qual.length) {
        quals ++= mutable.ArrayBuffer.fill(baseStats(pos).qual.length - quals.length)(0)
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
      var key: Int = pos - phredEncoding.id
      if (key > 0) {
        // count till the max of baseHistogram.length
        for (histokey <- 0 until key + 1) {
          baseHistogram(histokey) += quals(pos)
        }
      }
    }

    for (pos <- 0 until baseHistogram.length) {
      baseQualHistoMap += (pos -> baseHistogram(pos))
    }

    for (pos <- 0 until readStats.qual.length) {
      var key: Int = pos - phredEncoding.id
      if (key > 0) {
        // count till the max of baseHistogram.length
        for (histokey <- 0 until key + 1) {
          readHistogram(histokey) += readStats.qual(pos)
        }
      }
    }

    for (pos <- 0 until readHistogram.length) {
      readQualHistoMap += (pos -> readHistogram(pos))
    }

  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    logger.info("Start seqstat")

    val reader = new FastqReader(commandArgs.fastq)
    val numReads = seqStat(reader)
    summarize()

    logger.debug(nucs)
    //    logger.debug(baseStats)
    logger.info("Seqstat done")

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
          ("num_with_n", readStats.withN),
          ("num_total", readStats.qual.sum),
          ("len_min", readStats.lengths.takeWhile(_ == 0).length),
          ("len_max", readStats.lengths.length - 1),
          ("num_qual_gte", readQualHistoMap.toMap),
          ("qual_encoding", phredEncoding.toString.toLowerCase)
        ))
      ))
    )

    val jsonReport: Json = {
      ConfigUtils.mapToJson(report)
    }
    println(jsonReport.spaces2)
  }
}
