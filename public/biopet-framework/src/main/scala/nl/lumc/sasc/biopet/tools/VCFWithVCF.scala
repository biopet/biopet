package nl.lumc.sasc.biopet.tools

import java.io.File

import scala.collection.JavaConversions._
import htsjdk.variant.variantcontext.{ VariantContextBuilder, VariantContext }
import htsjdk.variant.variantcontext.writer.{AsyncVariantContextWriter, VariantContextWriterBuilder}
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.core.ToolCommand

import scala.collection.immutable

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

    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder().
      setOutputFile(commandArgs.outputFile).build())
    writer.writeHeader(header)

    var idx = 0

    for (record: VariantContext <- reader.iterator()) {
      if (idx % 100000 == 0) {
        logger.info(s"""Processed $idx records""")
      }
      var attr = record.getAttributes.toMap
      val field_map = scala.collection.mutable.Map[String, List[String]]()
      for (snd_rec <- secondaryReader.query(record.getChr, record.getStart, record.getEnd)) {
        for (f <- commandArgs.fields) {
          if (field_map.contains(f)) {
            field_map.update(f, snd_rec.getAttributeAsString(f, "unknown") :: field_map.get(f).get)
          }
          else {
            field_map += (f -> List(snd_rec.getAttributeAsString(f, "unknown")))
          }
        }
      }
      writer.add(field_map.filter(_._2.nonEmpty).map(x => (x._1, x._2.mkString(",").stripPrefix("[").stripSuffix("]")))
        .foldLeft(new VariantContextBuilder(record))((builder, attribute)
        => builder.attribute(attribute._1, attribute._2))
        .make())
      idx += 1
    }

    logger.debug("Closing readers")
    writer.close()
    reader.close()
    secondaryReader.close()
    logger.info("Done. Goodbye")
  }

}
