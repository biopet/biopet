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

import java.io.{ PrintWriter, File }

import htsjdk.samtools.{ QueryInterval, SAMRecord, SamReaderFactory, ValidationStringency }
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConversions._
import scala.io.Source

object FindRepeatsPacBio extends ToolCommand {
  case class Args(inputBam: File = null,
                  outputFile: Option[File] = None,
                  inputBed: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputBam") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputBam = x)
    } text "Path to input file"
    opt[File]('o', "outputFile") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "Path to input file"
    opt[File]('b', "inputBed") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputBed = x)
    } text "Path to bed file"
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {

    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)
    val bamReader = SamReaderFactory.makeDefault
      .validationStringency(ValidationStringency.SILENT)
      .open(commandArgs.inputBam)
    val bamHeader = bamReader.getFileHeader

    val header = List("chr", "startPos", "stopPos", "Repeat_seq", "repeatLength",
      "original_Repeat_readLength", "Calculated_repeat_readLength",
      "minLength", "maxLength", "inserts", "deletions", "notSpan")

    for (
      bedLine <- Source.fromFile(commandArgs.inputBed).getLines();
      values = bedLine.split("\t"); if values.size >= 3
    ) {
      val interval = new QueryInterval(bamHeader.getSequenceIndex(values(0)), values(1).toInt, values(2).toInt)
      val bamIter = bamReader.query(Array(interval), false)
      val results = for (samRecord <- bamIter) yield procesSamrecord(samRecord, interval)
      val chr = values(0)
      val startPos = values(1)
      val stopPos = values(2)
      val typeRepeat: String = if (values.size >= 15) values(14) else ""
      val repeatLength = typeRepeat.length
      val oriRepeatLength = values(2).toInt - values(1).toInt + 1
      var calcRepeatLength: List[Int] = Nil
      var minLength = -1
      var maxLength = -1
      var inserts: List[String] = Nil
      var deletions: List[String] = Nil
      var notSpan = 0

      for (result <- results) {
        if (result.isEmpty) notSpan += 1
        else {
          inserts ::= result.get.ins.map(_.insert).mkString(",")
          deletions ::= result.get.dels.map(_.length).mkString(",")
          val length = oriRepeatLength - result.get.beginDel - result.get.endDel -
            (0 /: result.get.dels.map(_.length))(_ + _) + (0 /: result.get.ins.map(_.insert.length))(_ + _)
          calcRepeatLength ::= length
          if (length > maxLength) maxLength = length
          if (length < minLength || minLength == -1) minLength = length
        }
      }
      bamIter.close()
      commandArgs.outputFile match {
        case Some(file) => {
          val writer = new PrintWriter(file)
          writer.println(header.mkString("\t"))
          writer.println(List(chr, startPos, stopPos, typeRepeat, repeatLength, oriRepeatLength, calcRepeatLength.mkString(","), minLength,
            maxLength, inserts.mkString("/"), deletions.mkString("/"), notSpan).mkString("\t"))
          writer.close()
        }
        case _ => {
          println(header.mkString("\t"))
          println(List(chr, startPos, stopPos, typeRepeat, repeatLength, oriRepeatLength, calcRepeatLength.mkString(","), minLength,
            maxLength, inserts.mkString("/"), deletions.mkString("/"), notSpan).mkString("\t"))
        }
      }
    }
  }

  case class Del(pos: Int, length: Int)
  case class Ins(pos: Int, insert: String)

  class Result() {
    var beginDel = 0
    var endDel = 0
    var dels: List[Del] = Nil
    var ins: List[Ins] = Nil
    var samRecord: SAMRecord = _

    override def toString = {
      "id: " + samRecord.getReadName + "  beginDel: " + beginDel + "  endDel: " + endDel + "  dels: " + dels + "  ins: " + ins
    }
  }

  def procesSamrecord(samRecord: SAMRecord, interval: QueryInterval): Option[Result] = {
    val readStartPos = List.range(0, samRecord.getReadBases.length)
      .find(x => samRecord.getReferencePositionAtReadPosition(x) >= interval.start)
    var readPos = if (readStartPos.isEmpty) return None else readStartPos.get
    if (samRecord.getAlignmentEnd < interval.end) return None
    if (samRecord.getAlignmentStart > interval.start) return None
    var refPos = samRecord.getReferencePositionAtReadPosition(readPos)

    val result = new Result
    result.samRecord = samRecord
    result.beginDel = interval.start - refPos
    while (refPos < interval.end) {
      val oldRefPos = refPos
      val oldReadPos = readPos
      do {
        readPos += 1
        refPos = samRecord.getReferencePositionAtReadPosition(readPos)
      } while (refPos < oldReadPos)
      val readDiff = readPos - oldReadPos
      val refDiff = refPos - oldRefPos
      if (refPos > interval.end) {
        result.endDel = interval.end - oldRefPos
      } else if (readDiff > refDiff) { //Insertion
        val insert = for (t <- oldReadPos + 1 until readPos) yield samRecord.getReadBases()(t - 1).toChar
        result.ins ::= Ins(oldRefPos, insert.mkString)
      } else if (readDiff < refDiff) { // Deletion
        result.dels ::= Del(oldRefPos, refDiff - readDiff)
      }
    }

    Some(result)
  }
}
