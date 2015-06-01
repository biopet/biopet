package nl.lumc.sasc.biopet.tools

import java.io.File

import scala.collection.JavaConversions._
import htsjdk.variant.variantcontext.{ VariantContextBuilder, VariantContext }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.core.ToolCommand

import scala.collection.immutable

/**
 * Created by ahbbollen on 11-2-15.
 */
object VcfWithVcf extends ToolCommand {
  case class Fields(inputField: String, outputField: String, t: String = "Array")

  case class Args(inputFile: File = null,
                  outputFile: File = null,
                  secondaryVcf: File = null,
                  fields: List[Fields] = Nil,
                  matchAllele: Boolean = true) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[File]('O', "outputFile") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('S', "SecondaryVcf") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(secondaryVcf = x)
    }
    opt[String]('f', "field") unbounded () valueName ("<input_field:output_field>") action { (x, c) =>
      val values = x.split(":")
      if (values.size > 2) c.copy(fields = Fields(values(0), values(1), values(2)) :: c.fields)
      else if (values.size > 1) c.copy(fields = Fields(values(0), values(1)) :: c.fields)
      else c.copy(fields = Fields(x, x) :: c.fields)
    }
    opt[Boolean]("match") valueName ("<Boolean>") maxOccurs (1) action { (x, c) =>
      c.copy(matchAllele = x)
    } text ("Match alternative alleles; default true")
  }

  def main(args: Array[String]): Unit = {
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
        throw new IllegalArgumentException("Field '" + x.outputField + "' already exist in input vcf")
      if (!secondHeader.hasInfoLine(x.inputField))
        throw new IllegalArgumentException("Field '" + x.inputField + "' does not exist in secondary vcf")

      val oldHeaderLine = secondHeader.getInfoHeaderLine(x.inputField)

      val newHeaderLine = new VCFInfoHeaderLine(x.outputField, VCFHeaderLineCount.UNBOUNDED,
        oldHeaderLine.getType, oldHeaderLine.getDescription)
      header.addMetaDataLine(newHeaderLine)
    }

    writer.writeHeader(header)

    var idx = 0

    for (record <- reader) {
      val secondaryRecords = if (commandArgs.matchAllele) {
        secondaryReader.query(record.getChr, record.getStart, record.getEnd).toList.
          filter(x => record.getAlternateAlleles.exists(x.hasAlternateAllele(_)))
      } else {
        secondaryReader.query(record.getChr, record.getStart, record.getEnd).toList
      }

      val fieldMap = (for (
        f <- commandArgs.fields;
        if secondaryRecords.exists(_.hasAttribute(f.inputField))
      ) yield {
        f.outputField -> (for (
          secondRecord <- secondaryRecords;
          if (secondRecord.hasAttribute(f.inputField))
        ) yield {
            secondRecord.getAttribute(f.inputField) match {
            case l: List[_] => l
            case x          => List(x)
          }
        }).fold(Nil)(_ ::: _)
      }).toMap

      writer.add(fieldMap.foldLeft(new VariantContextBuilder(record))((builder, attribute) => {
        builder.attribute(attribute._1, commandArgs.fields.filter(_.outputField == attribute._1).head.t match {
          case "max" => {
              header.getInfoHeaderLine(attribute._1).getType match {
                case VCFHeaderLineType.Integer => Array(attribute._2.map(_.toString.toInt).max)
                case VCFHeaderLineType.Float => Array(attribute._2.map(_.toString.toFloat).max)
                case _ => throw new IllegalArgumentException("Type not fit for max function")
              }
          }
          case _ => attribute._2.toArray
        })
      }).make())

      idx += 1
      if (idx % 100000 == 0) {
        logger.info(s"""Processed $idx records""")
      }
    }
    logger.info(s"""Processed $idx records""")

    logger.debug("Closing readers")
    writer.close()
    reader.close()
    secondaryReader.close()
    logger.info("Done. Goodbye")
  }

}
