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

import java.io.File

import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf.{ VCFFileReader, VCFHeaderLineCount, VCFHeaderLineType, VCFInfoHeaderLine }
import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.{ BedRecord, BedRecordList }

import scala.collection.JavaConversions._

// TODO: Queue wrapper

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
    } text "Input VCF file. Mandatory field"
    opt[File]('B', "bedFile") required () unbounded () valueName "<bed file>" action { (x, c) =>
      c.copy(bedFile = x)
    } text "Input Bed file. Mandatory field"
    opt[File]('o', "output") required () unbounded () valueName "<vcf file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "Output VCF file. Mandatory field"
    opt[String]('f', "fieldName") required () unbounded () valueName "<name of field in vcf file>" action { (x, c) =>
      c.copy(fieldName = x)
    } text "Name of info field in new vcf file"
    opt[String]('d', "fieldDescription") unbounded () valueName "<description of field in vcf file>" action { (x, c) =>
      c.copy(fieldDescription = x)
    } text "Description of field in new vcf file"
    opt[String]('t', "fieldType") unbounded () valueName "<type of field in vcf file>" action { (x, c) =>
      c.copy(fieldType = x)
    } text "Type of field in new vcf file. Can be 'Integer', 'Flag', 'Character', 'Float'"
  }

  /**
   * Program will Annotate a vcf file with the overlapping regions of a bed file,
   * 4e column of the bed file we in a info tag in the vcf file
   */
  def main(args: Array[String]): Unit = {

    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val fieldType = cmdArgs.fieldType match {
      case "Integer"   => VCFHeaderLineType.Integer
      case "Flag"      => VCFHeaderLineType.Flag
      case "Character" => VCFHeaderLineType.Character
      case "Float"     => VCFHeaderLineType.Float
      case _           => VCFHeaderLineType.String
    }

    logger.info("Reading bed file")
    val bedRecords = BedRecordList.fromFile(cmdArgs.bedFile).sorted

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
      val overlaps = bedRecords.overlapWith(BedRecord(record.getContig, record.getStart, record.getEnd))
      if (overlaps.isEmpty) {
        writer.add(record)
      } else {
        val builder = new VariantContextBuilder(record)
        if (fieldType == VCFHeaderLineType.Flag) builder.attribute(cmdArgs.fieldName, true)
        else builder.attribute(cmdArgs.fieldName, overlaps.map(_.name).mkString(","))
        writer.add(builder.make)
      }
    }
    reader.close()
    writer.close()

    logger.info("Done")
  }
}