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

import java.io.{ File, PrintWriter }

import htsjdk.samtools.{ SAMRecord, SamReaderFactory }
import nl.lumc.sasc.biopet.utils.{ ToolCommand, ConfigUtils }

import scala.collection.JavaConversions._
import scala.collection.mutable

object BiopetFlagstat extends ToolCommand {

  case class Args(inputFile: File = null,
                  outputFile: Option[File] = None,
                  summaryFile: Option[File] = None,
                  region: Option[String] = None) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "input bam file"
    opt[File]('o', "outputFile") valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "output file"
    opt[File]('s', "summaryFile") valueName "<file>" action { (x, c) =>
      c.copy(summaryFile = Some(x))
    } text "summary output file"
    opt[String]('r', "region") valueName "<chr:start-stop>" action { (x, c) =>
      c.copy(region = Some(x))
    } text "Region to calculate the statistics on"
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val inputSam = SamReaderFactory.makeDefault.open(commandArgs.inputFile)
    val iterSam = if (commandArgs.region.isEmpty) inputSam.iterator else {
      val regionRegex = """(.*):(.*)-(.*)""".r
      commandArgs.region.get match {
        case regionRegex(chr, start, stop) => inputSam.query(chr, start.toInt, stop.toInt, false)
        case _                             => sys.error("Region wrong format")
      }
    }

    val flagstatCollector = new FlagstatCollector
    flagstatCollector.loadDefaultFunctions()
    val m = 10
    val max = 60
    for (t <- 0 to (max / m))
      flagstatCollector.addFunction("MAPQ>" + (t * m), record => record.getMappingQuality > (t * m))
    flagstatCollector.addFunction("First normal, second read inverted (paired end orientation)", record => {
      if (record.getReadPairedFlag &&
        record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag != record.getMateNegativeStrandFlag &&
        ((record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))) true
      else false
    })
    flagstatCollector.addFunction("First normal, second read normal", record => {
      if (record.getReadPairedFlag &&
        record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag == record.getMateNegativeStrandFlag &&
        ((record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))) true
      else false
    })
    flagstatCollector.addFunction("First inverted, second read inverted", record => {
      if (record.getReadPairedFlag &&
        record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag == record.getMateNegativeStrandFlag &&
        ((record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))) true
      else false
    })
    flagstatCollector.addFunction("First inverted, second read normal", record => {
      if (record.getReadPairedFlag &&
        record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag != record.getMateNegativeStrandFlag &&
        ((record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
          (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart))) true
      else false
    })
    flagstatCollector.addFunction("Mate in same strand", record => record.getReadPairedFlag && record.getReadNegativeStrandFlag && record.getMateNegativeStrandFlag &&
      record.getReferenceIndex == record.getMateReferenceIndex)
    flagstatCollector.addFunction("Mate on other chr", record => record.getReadPairedFlag && record.getReferenceIndex != record.getMateReferenceIndex)

    logger.info("Start reading file: " + commandArgs.inputFile)
    for (record <- iterSam) {
      if (flagstatCollector.readsCount % 1e6 == 0 && flagstatCollector.readsCount > 0)
        logger.info("Reads prosessed: " + flagstatCollector.readsCount)
      flagstatCollector.loadRecord(record)
    }

    commandArgs.summaryFile.foreach {
      case file =>
        val writer = new PrintWriter(file)
        writer.println(flagstatCollector.summary)
        writer.close()
    }

    commandArgs.outputFile match {
      case Some(file) => {
        val writer = new PrintWriter(file)
        writer.println(flagstatCollector.report)
        writer.close()
      }
      case _ => println(flagstatCollector.report)
    }
  }

  class FlagstatCollector {
    private var functionCount = 0
    var readsCount = 0
    private val names: mutable.Map[Int, String] = mutable.Map()
    private var functions: Array[SAMRecord => Boolean] = Array()
    private var totalCounts: Array[Long] = Array()
    private var crossCounts = Array.ofDim[Long](1, 1)

    def loadDefaultFunctions() {
      addFunction("All", record => true)
      addFunction("Mapped", record => !record.getReadUnmappedFlag)
      addFunction("Duplicates", record => record.getDuplicateReadFlag)
      addFunction("FirstOfPair", record => if (record.getReadPairedFlag) record.getFirstOfPairFlag else false)
      addFunction("SecondOfPair", record => if (record.getReadPairedFlag) record.getSecondOfPairFlag else false)

      addFunction("ReadNegativeStrand", record => record.getReadNegativeStrandFlag)

      addFunction("NotPrimaryAlignment", record => record.getNotPrimaryAlignmentFlag)

      addFunction("ReadPaired", record => record.getReadPairedFlag)
      addFunction("ProperPair", record => if (record.getReadPairedFlag) record.getProperPairFlag else false)

      addFunction("MateNegativeStrand", record => if (record.getReadPairedFlag) record.getMateNegativeStrandFlag else false)
      addFunction("MateUnmapped", record => if (record.getReadPairedFlag) record.getMateUnmappedFlag else false)

      addFunction("ReadFailsVendorQualityCheck", record => record.getReadFailsVendorQualityCheckFlag)
      addFunction("SupplementaryAlignment", record => record.getSupplementaryAlignmentFlag)
      addFunction("SecondaryOrSupplementary", record => record.isSecondaryOrSupplementary)
    }

    def loadRecord(record: SAMRecord) {
      readsCount += 1
      val values: Array[Boolean] = new Array(names.size)
      for (t <- 0 until names.size) {
        values(t) = functions(t)(record)
        if (values(t)) {
          totalCounts(t) += 1
        }
      }
      for (t <- 0 until names.size) {
        for (t2 <- 0 until names.size) {
          if (values(t) && values(t2)) {
            crossCounts(t)(t2) += 1
          }
        }
      }
    }

    def addFunction(name: String, function: SAMRecord => Boolean) {
      functionCount += 1
      crossCounts = Array.ofDim[Long](functionCount, functionCount)
      totalCounts = new Array[Long](functionCount)
      val temp = new Array[SAMRecord => Boolean](functionCount)
      for (t <- 0 until (temp.length - 1)) temp(t) = functions(t)
      functions = temp

      val index = functionCount - 1
      names += (index -> name)
      functions(index) = function
      totalCounts(index) = 0
    }

    def report: String = {
      val buffer = new StringBuilder
      buffer.append("Number\tTotal Flags\tFraction\tName\n")
      for (t <- 0 until names.size) {
        val precentage = (totalCounts(t).toFloat / readsCount) * 100
        buffer.append("#" + (t + 1) + "\t" + totalCounts(t) + "\t" + f"$precentage%.4f" + "%\t" + names(t) + "\n")
      }
      buffer.append("\n")

      buffer.append(crossReport() + "\n")
      buffer.append(crossReport(fraction = true) + "\n")

      buffer.toString()
    }

    def summary: String = {
      val map = (for (t <- 0 until names.size) yield {
        names(t) -> totalCounts(t)
      }).toMap ++ Map("Singletons" -> crossCounts(names.find(_._2 == "Mapped").map(_._1).getOrElse(-1))(names.find(_._2 == "MateUnmapped").map(_._1).getOrElse(-1))
      )

      ConfigUtils.mapToJson(map).spaces4
    }

    def crossReport(fraction: Boolean = false): String = {
      val buffer = new StringBuilder

      for (t <- 0 until names.size) // Header for table
        buffer.append("\t#" + (t + 1))
      buffer.append("\n")

      for (t <- 0 until names.size) {
        buffer.append("#" + (t + 1) + "\t")
        for (t2 <- 0 until names.size) {
          val reads = crossCounts(t)(t2)
          if (fraction) {
            val precentage = (reads.toFloat / totalCounts(t).toFloat) * 100
            buffer.append(f"$precentage%.4f" + "%")
          } else buffer.append(reads)
          if (t2 == names.size - 1) buffer.append("\n")
          else buffer.append("\t")
        }
      }
      buffer.toString()
    }
  } // End of class

}
