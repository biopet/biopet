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
import scalaz._
import Scalaz._

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

// TODO: implement reading from gzipped files
object Seqstat extends ToolCommand {

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
  def startStatistics(fqf: FastqReader): (Int, Int, Int, Int, Map[Int, Int]) = {
    var (numRecords, numRecordsWithN, numBases, numBasesWithN) = (0, 0, 0, 0)
    var baseQuals: Map[Int, Int] = Map(0 -> 0)

    val it = fqf.iterator.asScala
    //    for (bla <- fqf.iterator.asScala.par) {
    //
    //    }
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

    }

    (numRecords, numRecordsWithN, numBases, numBasesWithN, baseQuals)
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

  def main(args: Array[String]): Unit = {

    val commandArgs: Args = parseArgs(args)
    val (numRecords, numRecordsWithN, numBases, numBasesWithN, baseHistogram) = startStatistics(
      new FastqReader(commandArgs.fastq)
    )
    println("Number of records:" + numRecords)
    println("Number of N records:" + numRecordsWithN)
    println("Number of bases:" + numBases)
    println("Number of N bases:" + numBasesWithN)
    println(baseHistogram)

    //
    //    val (synced, counts) = syncFastq(
    //      new FastqReader(commandArgs.refFastq),
    //      new FastqReader(commandArgs.inputFastq1),
    //      new FastqReader(commandArgs.inputFastq2))
    //
    //    writeSyncedFastq(synced, counts,
    //      new BasicFastqWriter(commandArgs.outputFastq1),
    //      new BasicFastqWriter(commandArgs.outputFastq2)
    //    )
  }
}
