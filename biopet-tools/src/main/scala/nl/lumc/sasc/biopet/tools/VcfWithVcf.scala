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
import java.util

import htsjdk.variant.variantcontext.{ VariantContext, VariantContextBuilder }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils.{ FastaUtils, ToolCommand }
import nl.lumc.sasc.biopet.utils.VcfUtils.scalaListToJavaObjectArrayList
import nl.lumc.sasc.biopet.utils.BamUtils.SamDictCheck

import scala.collection.JavaConversions._

/**
 * This is a tool to annotate a vcf file with info value from a other vcf file
 *
 * Created by ahbbollen on 11-2-15.
 */
object VcfWithVcf extends ToolCommand {
  case class Fields(inputField: String, outputField: String, fieldMethod: FieldMethod.Value = FieldMethod.none)

  case class Args(inputFile: File = null,
                  outputFile: File = null,
                  referenceFasta: File = null,
                  secondaryVcf: File = null,
                  fields: List[Fields] = Nil,
                  matchAllele: Boolean = true) extends AbstractArgs

  object FieldMethod extends Enumeration {
    val none, max, min, unique = Value
  }

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('s', "secondaryVcf") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(secondaryVcf = x)
    }
    opt[File]('R', "reference") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    }
    opt[String]('f', "field") unbounded () valueName "<field> or <input_field:output_field> or <input_field:output_field:method>" action { (x, c) =>
      val values = x.split(":")
      if (values.size > 2) c.copy(fields = Fields(values(0), values(1), FieldMethod.withName(values(2))) :: c.fields)
      else if (values.size > 1) c.copy(fields = Fields(values(0), values(1)) :: c.fields)
      else c.copy(fields = Fields(x, x) :: c.fields)
    } text """| If only <field> is given, the field's identifier in the output VCF will be identical to <field>.
              | By default we will return all values found for a given field.
              | For INFO fields with type R or A we will take the respective alleles present in the input file.
              | If a <method> is supplied, a method will be applied over the contents of the field.
              | In this case, all values will be considered.
              | The following methods are available:
              |   - max   : takes maximum of found value, only works for numeric (integer/float) fields
              |   - min   : takes minimum of found value, only works for numeric (integer/float) fields
              |   - unique: takes only unique values """.stripMargin
    opt[Boolean]("match") valueName "<Boolean>" maxOccurs 1 action { (x, c) =>
      c.copy(matchAllele = x)
    } text "Match alternative alleles; default true"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Init phase")

    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val reader = new VCFFileReader(commandArgs.inputFile)
    val secondaryReader = new VCFFileReader(commandArgs.secondaryVcf)

    val referenceDict = FastaUtils.getCachedDict(commandArgs.referenceFasta)

    val header = reader.getFileHeader
    val vcfDict = header.getSequenceDictionary match {
      case r if r != null =>
        r.assertSameDictionary(referenceDict, true)
        r
      case _ => referenceDict
    }
    val secondHeader = secondaryReader.getFileHeader

    secondHeader.getSequenceDictionary match {
      case r if r != null => r.assertSameDictionary(referenceDict, true)
      case _              =>
    }

    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setOutputFile(commandArgs.outputFile).
      setReferenceDictionary(vcfDict).
      build)

    for (x <- commandArgs.fields) {
      if (header.hasInfoLine(x.outputField))
        throw new IllegalArgumentException("Field '" + x.outputField + "' already exists in input vcf")
      if (!secondHeader.hasInfoLine(x.inputField))
        throw new IllegalArgumentException("Field '" + x.inputField + "' does not exist in secondary vcf")

      val oldHeaderLine = secondHeader.getInfoHeaderLine(x.inputField)

      val newHeaderLine = new VCFInfoHeaderLine(x.outputField, VCFHeaderLineCount.UNBOUNDED,
        oldHeaderLine.getType, oldHeaderLine.getDescription)
      header.addMetaDataLine(newHeaderLine)
    }
    writer.writeHeader(header)

    logger.info("Start reading records")

    var counter = 0
    for (record <- reader) {
      require(vcfDict.getSequence(record.getContig) != null, s"Contig ${record.getContig} does not exist on reference")
      val secondaryRecords = getSecondaryRecords(secondaryReader, record, commandArgs.matchAllele)

      val fieldMap = createFieldMap(commandArgs.fields, record, secondaryRecords, secondHeader)

      writer.add(createRecord(fieldMap, record, commandArgs.fields, header))

      counter += 1
      if (counter % 100000 == 0) {
        logger.info(s"""Processed $counter records""")
      }
    }
    logger.info(s"""Processed $counter records""")

    logger.debug("Closing readers")
    writer.close()
    reader.close()
    secondaryReader.close()
    logger.info("Done")
  }

  /**
   * Create Map of field -> List of attributes in secondary records
   * @param fields List of Field
   * @param record Original record
   * @param secondaryRecords List of VariantContext with secondary records
   * @param header: header of secondary reader
   * @return Map of fields and their values in secondary records
   */
  def createFieldMap(fields: List[Fields], record: VariantContext, secondaryRecords: List[VariantContext], header: VCFHeader): Map[String, List[Any]] = {
    val fieldMap = (for (
      f <- fields if secondaryRecords.exists(_.hasAttribute(f.inputField))
    ) yield {
      f.outputField -> (for (
        secondRecord <- secondaryRecords if secondRecord.hasAttribute(f.inputField)
      ) yield {
        getSecondaryField(record, secondRecord, f.inputField, header) match {
          case l: List[_]           => l
          case y: util.ArrayList[_] => y.toList
          case x                    => List(x)
        }
      }).fold(Nil)(_ ::: _)
    }).toMap
    fieldMap
  }

  /**
   * Get secondary records matching the query record
   * @param secondaryReader reader for secondary records
   * @param record query record
   * @param matchAllele allele has to match query allele?
   * @return List of VariantContext
   */
  def getSecondaryRecords(secondaryReader: VCFFileReader,
                          record: VariantContext, matchAllele: Boolean): List[VariantContext] = {
    if (matchAllele) {
      secondaryReader.query(record.getContig, record.getStart, record.getEnd).
        filter(x => record.getAlternateAlleles.exists(x.hasAlternateAllele)).toList
    } else {
      secondaryReader.query(record.getContig, record.getStart, record.getEnd).toIterable.toList
    }
  }

  def createRecord(fieldMap: Map[String, List[Any]], record: VariantContext,
                   fields: List[Fields], header: VCFHeader): VariantContext = {
    fieldMap.foldLeft(new VariantContextBuilder(record))((builder, attribute) => {
      builder.attribute(attribute._1, fields.filter(_.outputField == attribute._1).head.fieldMethod match {
        case FieldMethod.max =>
          header.getInfoHeaderLine(attribute._1).getType match {
            case VCFHeaderLineType.Integer => scalaListToJavaObjectArrayList(List(attribute._2.map(_.toString.toInt).max))
            case VCFHeaderLineType.Float   => scalaListToJavaObjectArrayList(List(attribute._2.map(_.toString.toFloat).max))
            case _                         => throw new IllegalArgumentException("Type of field " + attribute._1 + " is not numeric")
          }
        case FieldMethod.min =>
          header.getInfoHeaderLine(attribute._1).getType match {
            case VCFHeaderLineType.Integer => scalaListToJavaObjectArrayList(List(attribute._2.map(_.toString.toInt).min))
            case VCFHeaderLineType.Float   => scalaListToJavaObjectArrayList(List(attribute._2.map(_.toString.toFloat).min))
            case _                         => throw new IllegalArgumentException("Type of field " + attribute._1 + " is not numeric")
          }
        case FieldMethod.unique => scalaListToJavaObjectArrayList(attribute._2.distinct)
        case _ => {
          scalaListToJavaObjectArrayList(attribute._2)
        }
      })
    }).make()
  }

  /**
   * Get the proper representation of a field from a secondary record given an original record
   * @param record original record
   * @param secondaryRecord secondary record
   * @param field field
   * @param header header of secondary record
   * @return
   */
  def getSecondaryField(record: VariantContext, secondaryRecord: VariantContext, field: String, header: VCFHeader): Any = {
    header.getInfoHeaderLine(field).getCountType match {
      case VCFHeaderLineCount.A => numberA(record, secondaryRecord, field)
      case VCFHeaderLineCount.R => numberR(record, secondaryRecord, field)
      case _                    => secondaryRecord.getAttribute(field)
    }
  }

  /**
   * Get the correct values from a field that has number=A
   * @param referenceRecord the reference record
   * @param annotateRecord the to-be-annotated record
   * @param field the field to annotate
   * @return
   */
  def numberA(referenceRecord: VariantContext, annotateRecord: VariantContext, field: String): List[Any] = {
    val refValues = referenceRecord.getAttributeAsList(field).toArray
    annotateRecord.
      getAlternateAlleles.filter(referenceRecord.hasAlternateAllele).
      map(x => referenceRecord.getAlternateAlleles.indexOf(x)).
      flatMap(x => refValues.lift(x)).
      toList
  }

  /**
   * Get the correct values from a field that has number=R
   * @param referenceRecord the reference record
   * @param annotateRecord the to-be-annotated record
   * @param field the field to annotate
   * @return
   */
  def numberR(referenceRecord: VariantContext, annotateRecord: VariantContext, field: String): List[Any] = {
    val refValues = referenceRecord.getAttributeAsList(field).toArray
    annotateRecord.
      getAlleles.
      filter(referenceRecord.hasAllele).
      map(x => referenceRecord.getAlleles.indexOf(x)).
      flatMap(x => refValues.lift(x)).
      toList
  }
}
