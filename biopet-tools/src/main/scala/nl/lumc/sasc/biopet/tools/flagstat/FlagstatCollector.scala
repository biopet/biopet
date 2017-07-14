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
package nl.lumc.sasc.biopet.tools.flagstat

import java.io.{File, PrintWriter}

import htsjdk.samtools.SAMRecord
import nl.lumc.sasc.biopet.utils.ConfigUtils

import scala.collection.mutable

/**
  * Created by pjvan_thof on 21-7-16.
  */
class FlagstatCollector {
  protected[FlagstatCollector] var functionCount = 0
  var readsCount = 0
  protected[FlagstatCollector] val names: mutable.Map[Int, String] = mutable.Map()
  protected[FlagstatCollector] var functions: Array[SAMRecord => Boolean] = Array()
  protected[FlagstatCollector] var totalCounts: Array[Long] = Array()
  protected[FlagstatCollector] var crossCounts: Array[Array[Long]] = Array.ofDim[Long](1, 1)

  def writeAsTsv(file: File): Unit = {
    val writer = new PrintWriter(file)
    names.foreach(x => writer.println(x._2 + "\t" + totalCounts(x._1)))
    writer.close()
  }

  def loadDefaultFunctions() {
    addFunction("All", _ => true)
    addFunction("Mapped", record => !record.getReadUnmappedFlag)
    addFunction("Duplicates", record => record.getDuplicateReadFlag)
    addFunction("FirstOfPair",
                record => if (record.getReadPairedFlag) record.getFirstOfPairFlag else false)
    addFunction("SecondOfPair",
                record => if (record.getReadPairedFlag) record.getSecondOfPairFlag else false)

    addFunction("ReadNegativeStrand", record => record.getReadNegativeStrandFlag)

    addFunction("NotPrimaryAlignment", record => record.getNotPrimaryAlignmentFlag)

    addFunction("ReadPaired", record => record.getReadPairedFlag)
    addFunction("ProperPair",
                record => if (record.getReadPairedFlag) record.getProperPairFlag else false)

    addFunction(
      "MateNegativeStrand",
      record => if (record.getReadPairedFlag) record.getMateNegativeStrandFlag else false)
    addFunction("MateUnmapped",
                record => if (record.getReadPairedFlag) record.getMateUnmappedFlag else false)

    addFunction("ReadFailsVendorQualityCheck", record => record.getReadFailsVendorQualityCheckFlag)
    addFunction("SupplementaryAlignment", record => record.getSupplementaryAlignmentFlag)
    addFunction("SecondaryOrSupplementary", record => record.isSecondaryOrSupplementary)
  }

  /**
    * The method will aditional checks based on  mapping quality of the sam records.
    *
    * @param m steps of quality
    * @param max maximum quality
    */
  def loadQualityFunctions(m: Int = 10, max: Int = 60): Unit = {
    for (t <- 0 to (max / m))
      this.addFunction("MAPQ>" + (t * m), record => record.getMappingQuality > (t * m))
  }

  /**
    * This method will add functions to check orientation, for this a combination of flags and read positions are used.
    */
  def loadOrientationFunctions(): Unit = {
    this.addFunction(
      "First normal, second read inverted (paired end orientation)",
      record => {
        if (record.getReadPairedFlag &&
            record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag != record.getMateNegativeStrandFlag &&
            ((record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart)))
          true
        else false
      }
    )
    this.addFunction(
      "First normal, second read normal",
      record => {
        if (record.getReadPairedFlag &&
            record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag == record.getMateNegativeStrandFlag &&
            ((record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart)))
          true
        else false
      }
    )
    this.addFunction(
      "First inverted, second read inverted",
      record => {
        if (record.getReadPairedFlag &&
            record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag == record.getMateNegativeStrandFlag &&
            ((record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart)))
          true
        else false
      }
    )
    this.addFunction(
      "First inverted, second read normal",
      record => {
        if (record.getReadPairedFlag &&
            record.getReferenceIndex == record.getMateReferenceIndex && record.getReadNegativeStrandFlag != record.getMateNegativeStrandFlag &&
            ((record.getFirstOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getFirstOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && record.getReadNegativeStrandFlag && record.getAlignmentStart < record.getMateAlignmentStart) ||
            (record.getSecondOfPairFlag && !record.getReadNegativeStrandFlag && record.getAlignmentStart > record.getMateAlignmentStart)))
          true
        else false
      }
    )
    this.addFunction(
      "Mate in same strand",
      record =>
        record.getReadPairedFlag && record.getReadNegativeStrandFlag && record.getMateNegativeStrandFlag &&
          record.getReferenceIndex == record.getMateReferenceIndex
    )
    this.addFunction(
      "Mate on other chr",
      record =>
        record.getReadPairedFlag && record.getReferenceIndex != record.getMateReferenceIndex)

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
      val percentage = (totalCounts(t).toFloat / readsCount) * 100
      buffer.append(
        "#" + (t + 1) + "\t" + totalCounts(t) + "\t" + f"$percentage%.4f" + "%\t" + names(t) + "\n")
    }
    buffer.append("\n")

    buffer.append(crossReport() + "\n")
    buffer.append(crossReport(fraction = true) + "\n")

    buffer.toString()
  }

  def writeReportToFile(outputFile: File): Unit = {
    val writer = new PrintWriter(outputFile)
    writer.println(report)
    writer.close()
  }

  def summary: String = {
    val map = (for (t <- 0 until names.size) yield {
      names(t) -> totalCounts(t)
    }).toMap ++ Map(
      "Singletons" -> crossCounts(names.find(_._2 == "Mapped").map(_._1).getOrElse(-1))(
        names.find(_._2 == "MateUnmapped").map(_._1).getOrElse(-1)))

    ConfigUtils.mapToJson(map).spaces4
  }

  def writeSummaryTofile(outputFile: File): Unit = {
    val writer = new PrintWriter(outputFile)
    writer.println(summary)
    writer.close()
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
          val percentage = (reads.toFloat / totalCounts(t).toFloat) * 100
          buffer.append(f"$percentage%.4f" + "%")
        } else buffer.append(reads)
        if (t2 == names.size - 1) buffer.append("\n")
        else buffer.append("\t")
      }
    }
    buffer.toString()
  }

  def toSummaryMap: Map[String, Any] = {
    val sortedKeys = names.keys.toArray.sorted
    sortedKeys.map(x => names(x) -> totalCounts(x)).toMap ++
      Map("cross_counts" -> crossCounts)
  }

  def +=(other: FlagstatCollector): FlagstatCollector = {
    require(this.names == other.names)
    //require(this.functions == other.functions)

    this.readsCount += other.readsCount

    this.totalCounts.zipWithIndex.foreach {
      case (_, i) => this.totalCounts(i) += other.totalCounts(i)
    }
    this.crossCounts.zipWithIndex.foreach {
      case (v1, i1) =>
        v1.zipWithIndex.foreach {
          case (_, i2) =>
            this.crossCounts(i1)(i2) += other.crossCounts(i1)(i2)
        }
    }

    this
  }

  override def equals(other: Any): Boolean = {
    other match {
      case f: FlagstatCollector => f.totalCounts.toList == this.totalCounts.toList
      case _ => false
    }
  }
}
