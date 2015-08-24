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

import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf.{ VCFFileReader, VCFHeaderLineCount, VCFHeaderLineType, VCFInfoHeaderLine }
import nl.lumc.sasc.biopet.core.ToolCommand

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source

class AnnotateVcfWithBed {
  // TODO: Queue wrapper
}

/**
 * This a tools to annotate a vcf file with values from a bed file
 *
 * Created by pjvan_thof on 1/10/15.
 */
object AnnotateVcfWithBed extends ToolCommand {

  /**
   * Args for the commandline tool
   * @param inputFile input vcf file
   * @param bedFile bed file to annotate to vcf file
   * @param outputFile output vcf file
   * @param fieldName Info field that should be used
   * @param fieldDescription Description at field if needed
   * @param fieldType Type of filed, can be: "Integer", "Flag", "Character", "Float"
   */
  case class Args(inputFile: File = null,
                  bedFile: File = null,
                  outputFile: File = null,
                  fieldName: String = null,
                  fieldDescription: String = "",
                  fieldType: String = "String") extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () valueName "<vcf file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "Input is a required file property"
    opt[File]('B', "bedFile") required () unbounded () valueName "<bed file>" action { (x, c) =>
      c.copy(bedFile = x)
    } text "Bedfile is a required file property"
    opt[File]('o', "output") required () unbounded () valueName "<vcf file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "out is a required file property"
    opt[String]('f', "fieldName") required () unbounded () valueName "<name of field in vcf file>" action { (x, c) =>
      c.copy(fieldName = x)
    } text "Name of info field in new vcf file"
    opt[String]('d', "fieldDescription") unbounded () valueName "<name of field in vcf file>" action { (x, c) =>
      c.copy(fieldDescription = x)
    } text "Description of field in new vcf file"
    opt[String]('t', "fieldType") unbounded () valueName "<name of field in vcf file>" action { (x, c) =>
      c.copy(fieldType = x)
    } text "Description of field in new vcf file"
  }

  /**
   * Program will Annotate a vcf file with the overlapping regions of a bed file,
   * 4e column of the bed file we in a info tag in the vcf file
   */
  def main(args: Array[String]): Unit = {

    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val bedRecords: mutable.Map[String, List[(Int, Int, String)]] = mutable.Map()
    // Read bed file
    /*
    // function bedRecord.getName will not compile, not clear why
    for (bedRecord <- asScalaIteratorConverter(getFeatureReader(commandArgs.bedFile.toPath.toString, new BEDCodec(), false).iterator()).asScala) {
      logger.debug(bedRecord)
      bedRecords(bedRecord.getChr) = (bedRecord.getStart, bedRecord.getEnd, bedRecord.getName) :: bedRecords.getOrElse(bedRecord.getChr, Nil)
    }
    */

    val fieldType = cmdArgs.fieldType match {
      case "Integer"   => VCFHeaderLineType.Integer
      case "Flag"      => VCFHeaderLineType.Flag
      case "Character" => VCFHeaderLineType.Character
      case "Float"     => VCFHeaderLineType.Float
      case _           => VCFHeaderLineType.String
    }

    logger.info("Reading bed file")

    for (line <- Source.fromFile(cmdArgs.bedFile).getLines()) {
      val values = line.split("\t")
      if (values.size >= 4)
        bedRecords(values(0)) = (values(1).toInt, values(2).toInt, values(3)) :: bedRecords.getOrElse(values(0), Nil)
      else values.size >= 3 && fieldType == VCFHeaderLineType.Flag
      bedRecords(values(0)) = (values(1).toInt, values(2).toInt, "") :: bedRecords.getOrElse(values(0), Nil)
    }

    logger.info("Sorting bed records")

    // Sort records when needed
    for ((chr, record) <- bedRecords) {
      bedRecords(chr) = record.sortBy(x => (x._1, x._2))
    }

    logger.info("Starting output file")

    val reader = new VCFFileReader(cmdArgs.inputFile, false)
    val header = reader.getFileHeader

    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setOutputFile(cmdArgs.outputFile).
      setReferenceDictionary(header.getSequenceDictionary).
      build)

    header.addMetaDataLine(new VCFInfoHeaderLine(cmdArgs.fieldName,
      VCFHeaderLineCount.UNBOUNDED, fieldType, cmdArgs.fieldDescription))
    writer.writeHeader(header)

    logger.info("Start reading vcf records")

    for (record <- reader) {
      val overlaps = bedRecords.getOrElse(record.getContig, Nil).filter(x => {
        record.getStart <= x._2 && record.getEnd >= x._1
      })
      if (overlaps.isEmpty) {
        writer.add(record)
      } else {
        val builder = new VariantContextBuilder(record)
        if (fieldType == VCFHeaderLineType.Flag) builder.attribute(cmdArgs.fieldName, true)
        else builder.attribute(cmdArgs.fieldName, overlaps.map(_._3).mkString(","))
        writer.add(builder.make)
      }
    }
    reader.close()
    writer.close()

    logger.info("Done")
  }
}