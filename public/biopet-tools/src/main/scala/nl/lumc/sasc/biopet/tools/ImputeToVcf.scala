package nl.lumc.sasc.biopet.tools

import java.io.File
import java.util

import htsjdk.tribble.index.IndexCreator
import htsjdk.variant.variantcontext.writer.{Options, VariantContextWriterBuilder, AsyncVariantContextWriter}
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.io.Source

/**
  * Created by pjvanthof on 15/03/16.
  */
object ImputeToVcf extends ToolCommand {

  case class Args(inputGenotypes: File = null,
                  inputInfo: Option[File] = None,
                  outputVcf: File = null,
                  sampleFile: File = null,
                  referenceFasta: Option[File] = None) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('g', "inputGenotypes") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputGenotypes = x)
    } text "Input genotypes"
    opt[File]('i', "inputInfo") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputInfo = Some(x))
    } text "Input info fields"
    opt[File]('o', "outputVcf") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputVcf = x)
    } text "Output vcf file"
    opt[File]('s', "samplesFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(sampleFile = x)
    } text "Samples file"
    opt[File]('R', "referenceFasta") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = Some(x))
    } text "reference fasta file"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()).getOrElse(throw new IllegalArgumentException)

    val samples = new util.HashSet[String]()
    Source.fromFile(cmdArgs.sampleFile).getLines().toArray.drop(2).map(_.split("\t").head).foreach(samples.add(_))

    val metaLines = new util.HashSet[VCFHeaderLine]()
    metaLines.add(new VCFFormatHeaderLine("PL", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Float, ""))

    //TODO: Add reference dict
    val header = new VCFHeader(metaLines, samples)
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder()
      .setOutputFile(cmdArgs.outputVcf)
      .unsetOption(Options.INDEX_ON_THE_FLY)
      //.setReferenceDictionary(header.getSequenceDictionary)
      .build)
    writer.writeHeader(header)



    writer.close()
  }
}
