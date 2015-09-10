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
import java.util

import htsjdk.variant.variantcontext.{ VariantContext, VariantContextBuilder }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.core.{ ToolCommandFuntion, ToolCommand }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.VcfUtils.scalaListToJavaObjectArrayList
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Biopet extension for tool VcfWithVcf
 */
class VcfWithVcf(val root: Configurable) extends ToolCommandFuntion {
  javaMainClass = getClass.getName

  @Input(doc = "Input vcf file", shortName = "input", required = true)
  var input: File = _

  @Input(doc = "Secondary vcf file", shortName = "secondary", required = true)
  var secondaryVcf: File = _

  @Output(doc = "Output vcf file", shortName = "output", required = true)
  var output: File = _

  @Output(doc = "Output vcf file index", shortName = "output", required = true)
  private var outputIndex: File = _

  var fields: List[(String, String, Option[String])] = List()

  override def defaultCoreMemory = 2.0

  override def beforeGraph() {
    super.beforeGraph()
    if (output.getName.endsWith(".gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    if (output.getName.endsWith(".vcf")) outputIndex = new File(output.getAbsolutePath + ".idx")
    if (fields.isEmpty) throw new IllegalArgumentException("No fields found for VcfWithVcf")
  }

  override def commandLine = super.commandLine +
    required("-I", input) +
    required("-o", output) +
    required("-s", secondaryVcf) +
    repeat("-f", fields.map(x => x._1 + ":" + x._2 + ":" + x._3.getOrElse("none")))
}

/**
 * This is a tool to annotate a vcf file with info value from a other vcf file
 *
 * Created by ahbbollen on 11-2-15.
 */
object VcfWithVcf extends ToolCommand {
  case class Fields(inputField: String, outputField: String, fieldMethod: FieldMethod.Value = FieldMethod.none)

  case class Args(inputFile: File = null,
                  outputFile: File = null,
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
    opt[String]('f', "field") unbounded () valueName "<field> or <input_field:output_field> or <input_field:output_field:method>" action { (x, c) =>
      val values = x.split(":")
      if (values.size > 2) c.copy(fields = Fields(values(0), values(1), FieldMethod.withName(values(2))) :: c.fields)
      else if (values.size > 1) c.copy(fields = Fields(values(0), values(1)) :: c.fields)
      else c.copy(fields = Fields(x, x) :: c.fields)
    } text """| If only <field> is given, the field's identifier in the output VCF will be identical to <field>.
              | By default we will return all values found for a given field.
              | With <method> the values will processed after getting it from the secondary VCF file, posible methods are:
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
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val reader = new VCFFileReader(commandArgs.inputFile)
    val secondaryReader = new VCFFileReader(commandArgs.secondaryVcf)

    val header = reader.getFileHeader
    val secondHeader = secondaryReader.getFileHeader
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setOutputFile(commandArgs.outputFile).
      setReferenceDictionary(header.getSequenceDictionary).
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
      val secondaryRecords = getSecondaryRecords(secondaryReader, record, commandArgs.matchAllele)

      val fieldMap = createFieldMap(commandArgs.fields, secondaryRecords)

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
   * @param secondaryRecords List of VariantContext with secondary records
   * @return Map of fields and their values in secondary records
   */
  def createFieldMap(fields: List[Fields], secondaryRecords: List[VariantContext]): Map[String, List[Any]] = {
    val fieldMap = (for (
      f <- fields if secondaryRecords.exists(_.hasAttribute(f.inputField))
    ) yield {
      f.outputField -> (for (
        secondRecord <- secondaryRecords if secondRecord.hasAttribute(f.inputField)
      ) yield {
        secondRecord.getAttribute(f.inputField) match {
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
      secondaryReader.query(record.getContig, record.getStart, record.getEnd).toList.
        filter(x => record.getAlternateAlleles.exists(x.hasAlternateAllele))
    } else {
      secondaryReader.query(record.getContig, record.getStart, record.getEnd).toList
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
          print(attribute._2.getClass.toString)
          scalaListToJavaObjectArrayList(attribute._2)
        }
      })
    }).make()
  }
}
