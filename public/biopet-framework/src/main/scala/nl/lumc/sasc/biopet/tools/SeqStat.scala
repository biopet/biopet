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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.fastq.{ FastqReader, FastqRecord }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.core.{ ToolCommand, ToolCommandFuntion }
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.language.postfixOps

/**
 * Seqstat function class for usage in Biopet pipelines
 *
 * @param root Configuration object for the pipeline
 */
class SeqStat(val root: Configurable) extends ToolCommandFuntion with Summarizable {
  javaMainClass = getClass.getName

  @Input(doc = "Input FASTQ", shortName = "input", required = true)
  var input: File = null

  @Output(doc = "Output JSON", shortName = "output", required = true)
  var output: File = null

  override def defaultCoreMemory = 2.5

  override def commandLine = super.commandLine + required("-i", input) + " > " + required(output)

  def summaryStats: Map[String, Any] = {
    val map = ConfigUtils.fileToConfigMap(output)

    ConfigUtils.any2map(map.getOrElse("stats", Map()))
  }

  def summaryFiles: Map[String, File] = Map()

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Array[_], v2: Array[_])           => v1.zip(v2).map(v => resolveSummaryConflict(v._1, v._2, key))
      case (v1: List[_], v2: List[_])             => v1.zip(v2).map(v => resolveSummaryConflict(v._1, v._2, key))
      case (v1: Int, v2: Int) if key == "len_min" => if (v1 < v2) v1 else v2
      case (v1: Int, v2: Int) if key == "len_max" => if (v1 > v2) v1 else v2
      case (v1: Int, v2: Int)                     => v1 + v2
      case (v1: Long, v2: Long)                   => v1 + v2
      case _                                      => v1
    }
  }
}

object FqEncoding extends Enumeration {
  type FqEncoding = Value
  val Sanger = Value(33, "Sanger")
  val Solexa = Value(64, "Solexa")
  val Unknown = Value(0, "Unknown")
}

object SeqStat extends ToolCommand {
  def apply(root: Configurable, input: File, output: File): SeqStat = {
    val seqstat = new SeqStat(root)
    seqstat.input = input
    seqstat.output = new File(output, input.getName.substring(0, input.getName.lastIndexOf(".")) + ".seqstats.json")
    seqstat
  }

  def apply(root: Configurable, fastqfile: File, outDir: String): SeqStat = {
    val seqstat = new SeqStat(root)
    seqstat.input = fastqfile
    seqstat.output = new File(outDir, fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    seqstat
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
      case (false, true) => phredEncoding = Solexa
      // TODO: check this later on
      // complex case, we cannot tell wheter this is a sanger or solexa
      // but since the h_qual exceeds any Sanger/Illumina1.8 quals, we can `assume` this is solexa
      case (true, true)  => phredEncoding = Solexa
      // this is definite a sanger sequence, the lower end is sanger only
      case (true, false) => phredEncoding = Sanger
      case (_, _)        => phredEncoding = Unknown
    }
  }

  // Setting up the internal storage for the statistics gathered for each read
  // 'nuc' are the nucleotides 'ACTGN', the max ASCII value for this is T, pre-init the ArrayBuffer to this value
  // as we don't expect the have other 'higher' numbered Nucleotides for now.
  case class BaseStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nuc: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill('T'.toInt + 1)(0))

  case class ReadStat(qual: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer(),
                      nuc: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.fill('T'.toInt + 1)(0),
                      var withN: Long = 0L,
                      lengths: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer())

  val baseStats: mutable.ArrayBuffer[BaseStat] = mutable.ArrayBuffer()
  val readStats: ReadStat = new ReadStat()

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

    if (record.length >= readStats.lengths.size) // Extends array when length not yet possible
      (0 to (record.length - readStats.lengths.size)).foreach(_ => readStats.lengths.append(0))

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
    val avgQual: Int = readQual.sum / readQual.length
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
      procesRead(read)
      numReads += 1
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
      if (nucs.length <= baseStats(pos).nuc.length) {
        for (_ <- nucs.length until baseStats(pos).nuc.length) nucs.append(0)
      }
      // count into the quals
      baseStats(pos).qual.zipWithIndex foreach { case (value, index) => quals(index) += value }
      // count N into nucs
      baseStats(pos).nuc.zipWithIndex foreach { case (value, index) => nucs(index) += value }
    }
    detectPhredEncoding(quals)
    logger.debug("Detected '" + phredEncoding.toString.toLowerCase + "' encoding in fastq file ...")

    for (pos <- nucs.indices) {
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

    for (pos <- quals.indices) {
      val key: Int = pos - phredEncoding.id
      if (key >= 0) {
        baseHistogram(key) += quals(pos)
      }
    }

    for (pos <- readStats.qual.indices) {
      val key: Int = pos - phredEncoding.id
      if (key > 0) {
        // count till the max of baseHistogram.length
        for (histokey <- 0 until key + 1) {
          readHistogram(histokey) += readStats.qual(pos)
        }
      }
    }

    for (pos <- readHistogram.indices) {
      readQualHistoMap += (pos -> readHistogram(pos))
    }

  }

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)

    logger.info("Start seqstat")
    seqStat(new FastqReader(commandArgs.fastq))
    summarize()
    logger.info("Seqstat done")

    val report: Map[String, Any] = Map(
      ("files",
        Map(
          ("fastq", Map(
            ("path", commandArgs.fastq))
          )
        )
      ),
      ("stats", Map(
        ("bases", Map(
          ("num_total", nucleotideHistoMap.values.sum),
          ("num_qual", baseHistogram.toList),
          ("nucleotides", nucleotideHistoMap.toMap)
        )),
        ("reads", Map(
          ("num_with_n", readStats.withN),
          ("num_total", readStats.qual.sum),
          ("len_min", readStats.lengths.takeWhile(_ == 0).length),
          ("len_max", readStats.lengths.length - 1),
          ("num_avg_qual_gte", readQualHistoMap.toMap),
          ("qual_encoding", phredEncoding.toString.toLowerCase)
        ))
      ))
    )

    // TODO: have function generate map so that it can be tested

    println(ConfigUtils.mapToJson(report))
  }
}
