package nl.lumc.sasc.biopet.tools

import java.io.File

import scala.collection.JavaConversions._
import htsjdk.variant.variantcontext.{ VariantContextBuilder, VariantContext }
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.core.ToolCommand

/**
 * Created by ahbbollen on 11-2-15.
 */
object VCFWithVCF extends ToolCommand {
  case class Args(inputFile: File = null, outputFile: File = null, secondaryVCF: File = null,
                  fields: List[String] = Nil) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(inputFile = x)
    }
    opt[File]('O', "outputFile") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('S', "SecondaryVCF") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(secondaryVCF = x)
    }
    opt[String]('f', "field") unbounded () action { (x, c) =>
      c.copy(fields = x :: c.fields)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val reader = new VCFFileReader(commandArgs.inputFile)
    val secondaryReader = new VCFFileReader(commandArgs.secondaryVCF)

    val header = reader.getFileHeader

    for (x <- commandArgs.fields) {
      val tmpheaderline = new VCFInfoHeaderLine(x, VCFHeaderLineCount.UNBOUNDED,
        VCFHeaderLineType.String, "A custom annotation")
      header.addMetaDataLine(tmpheaderline)
    }

    val writerBuilder = new VariantContextWriterBuilder()
    writerBuilder.
      setOutputFile(commandArgs.outputFile).
      setOutputFileType(VariantContextWriterBuilder.OutputType.BLOCK_COMPRESSED_VCF)
    val writer = writerBuilder.build()
    writer.writeHeader(header)

    for (record: VariantContext <- reader.iterator()) {
      var attr = record.getAttributes.toMap
      secondaryReader.query(record.getChr, record.getStart, record.getEnd).
        foreach(x => attr = makeAttributesFromRecord(x, attr, commandArgs.fields))
      val nmap = flattenAttributes(attr, commandArgs.fields)
      val nrecord = new VariantContextBuilder(record).attributes(nmap).make()
      writer.add(nrecord)
    }

    writer.close()
    reader.close()
    secondaryReader.close()
  }

  /**
   * Makes a new attribute map with added fields from record
   * @param record the record fields have to be taken from
   * @param attr attribute map
   * @param fields field to get
   * @return new attribute map
   */
  def makeAttributesFromRecord(record: VariantContext, attr: Map[String, AnyRef], fields: List[String]): Map[String, AnyRef] = {
    for (f <- fields) {
      val value = record.getAttribute(f)
      if (attr.contains(f)) {
        attr ++ Map(f -> attr.get(f).toList.add(value))
      } else {
        attr ++ Map(f -> List(value))
      }
    }
    attr
  }

  /**
   * HTSJDK does not have a list type for fields, so we must make a string
   * @param attr attribute map
   * @param fields fields to flatten
   * @return modified map
   */
  def flattenAttributes(attr: Map[String, AnyRef], fields: List[String]): Map[String, AnyRef] = {
    fields.foreach(x => attr ++ Map(x -> attr.get(x).mkString(",")))
    attr
  }

}
