package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.variantcontext.writer.{
  AsyncVariantContextWriter,
  VariantContextWriterBuilder
}
import htsjdk.variant.vcf.VCFFileReader
import nl.lumc.sasc.biopet.utils.{FastaUtils, ToolCommand}

import scala.collection.JavaConversions._

/**
  * Created by pjvan_thof on 30-5-17.
  */
object ReplaceContigsVcfFile extends ToolCommand {
  case class Args(input: File = null,
                  output: File = null,
                  referenceFile: File = null,
                  contigs: Map[String, String] = Map())
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    } text "Input vcf file"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    } text "Output vcf file"
    opt[File]('R', "referenceFile") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(referenceFile = x)
    } text "Reference fasta file"
    opt[Map[String, String]]("contig") unbounded () action { (x, c) =>
      c.copy(contigs = c.contigs ++ x)
    }
    opt[File]("contigMappingFile") unbounded () action { (x, c) =>
      c.copy(contigs = c.contigs ++ FastaUtils.readContigMapReverse(x))
    } text "File how to map contig names, first column is the new name, second column is semicolon separated list of alternative names"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

    if (!cmdArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + cmdArgs.input)

    logger.info("Start")

    val dict = FastaUtils.getDictFromFasta(cmdArgs.referenceFile)

    val reader = new VCFFileReader(cmdArgs.input, false)
    val header = reader.getFileHeader
    header.setSequenceDictionary(dict)
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setOutputFile(cmdArgs.output)
        .setReferenceDictionary(dict)
        .build)
    writer.writeHeader(header)

    for (record <- reader) {
      val builder = new VariantContextBuilder(record)

      val newRecord =
        builder.chr(cmdArgs.contigs.getOrElse(record.getContig, record.getContig)).make()
      writer.write(newRecord)
    }

    reader.close()
    writer.close()
    logger.info("Done")
  }

}
