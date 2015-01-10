package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.tribble.AbstractFeatureReader.getFeatureReader
import htsjdk.tribble.bed.BEDCodec
import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.{ VariantContextWriterBuilder, AsyncVariantContextWriter }
import htsjdk.variant.vcf.{ VCFHeaderLineType, VCFHeaderLineCount, VCFInfoHeaderLine, VCFFileReader }
import nl.lumc.sasc.biopet.core.ToolCommand
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source

/**
 * Created by pjvan_thof on 1/10/15.
 */
class AnnotateVcfWithBed {
  // TODO: Queue wrapper
}

object AnnotateVcfWithBed extends ToolCommand {

  case class Args(inputFile: File = null,
                  bedFile: File = null,
                  outputFile: File = null,
                  fieldName: String = null,
                  fieldDescription: String = "",
                  fieldType: String = "String") extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () unbounded () valueName ("<vcf file>") action { (x, c) =>
      c.copy(inputFile = x)
    } text ("out is a required file property")
    opt[File]('B', "bedFile") required () unbounded () valueName ("<bed file>") action { (x, c) =>
      c.copy(bedFile = x)
    } text ("out is a required file property")
    opt[File]('o', "output") required () unbounded () valueName ("<vcf file>") action { (x, c) =>
      c.copy(outputFile = x)
    } text ("out is a required file property")
    opt[String]('f', "fieldName") required () unbounded () valueName ("<name of field in vcf file>") action { (x, c) =>
      c.copy(fieldName = x)
    } text ("Name of info field in new vcf file")
    opt[String]('d', "fieldDescription") unbounded () valueName ("<name of field in vcf file>") action { (x, c) =>
      c.copy(fieldDescription = x)
    } text ("Description of field in new vcf file")
    opt[String]('t', "fieldType") unbounded () valueName ("<name of field in vcf file>") action { (x, c) =>
      c.copy(fieldType = x)
    } text ("Description of field in new vcf file")
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val bedRecords: mutable.Map[String, List[(Int, Int, String)]] = mutable.Map()
    // Read bed file
    /*
    // function bedRecord.getName will not compile, not clear why
    for (bedRecord <- asScalaIteratorConverter(getFeatureReader(commandArgs.bedFile.toPath.toString, new BEDCodec(), false).iterator()).asScala) {
      logger.debug(bedRecord)
      bedRecords(bedRecord.getChr) = (bedRecord.getStart, bedRecord.getEnd, bedRecord.getName) :: bedRecords.getOrElse(bedRecord.getChr, Nil)
    }
    */
    for (line <- Source.fromFile(commandArgs.bedFile).getLines()) {
      val values = line.split("\t")
      if (values.size >= 4)
        bedRecords(values(0)) = (values(1).toInt, values(2).toInt, values(3)) :: bedRecords.getOrElse(values(0), Nil)
    }

    // Sort records when needed
    for ((chr, record) <- bedRecords) {
      bedRecords(chr) = record.sortBy(x => (x._1, x._2))
    }

    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val header = reader.getFileHeader

    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().setOutputFile(commandArgs.outputFile).build)
    val fieldType = commandArgs.fieldType match {
      case "Integer"   => VCFHeaderLineType.Integer
      case "Flag"      => VCFHeaderLineType.Flag
      case "Character" => VCFHeaderLineType.Character
      case "Float"     => VCFHeaderLineType.Float
      case _           => VCFHeaderLineType.String
    }
    header.addMetaDataLine(new VCFInfoHeaderLine(commandArgs.fieldName,
      VCFHeaderLineCount.UNBOUNDED, fieldType, commandArgs.fieldDescription))
    writer.writeHeader(header)

    for (record <- reader) {
      val overlaps = bedRecords.getOrElse(record.getChr, Nil).filter(x => {
        record.getStart <= x._2 && record.getEnd >= x._1
      })
      if (overlaps.isEmpty) {
        writer.add(record)
      } else {
        val builder = new VariantContextBuilder(record)
        builder.attribute(commandArgs.fieldName, overlaps.map(_._3).mkString(","))
        writer.add(builder.make)
      }
    }
    reader.close
    writer.close
  }
}