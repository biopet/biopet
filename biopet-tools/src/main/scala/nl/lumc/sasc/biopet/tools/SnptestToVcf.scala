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

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, Options, VariantContextWriterBuilder }
import htsjdk.variant.variantcontext.{ Allele, VariantContextBuilder }
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils.{ FastaUtils, ToolCommand }

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by pjvanthof on 15/03/16.
 */
object SnptestToVcf extends ToolCommand {

  case class Args(inputInfo: File = null,
                  outputVcf: File = null,
                  referenceFasta: File = null,
                  contig: String = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "inputInfo") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputInfo = x)
    } text "Input info fields"
    opt[File]('o', "outputVcf") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputVcf = x)
    } text "Output vcf file"
    opt[File]('R', "referenceFasta") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    } text "reference fasta file"
    opt[String]('c', "contig") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(contig = x)
    } text "contig of impute file"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()).getOrElse(throw new IllegalArgumentException)

    val infoIt = Source.fromFile(cmdArgs.inputInfo).getLines()
    val infoHeader = infoIt.find(!_.startsWith("#"))

    infoHeader match {
      case Some(header) => parseLines(header, infoIt, cmdArgs)
      case _ =>
        writeEmptyVcf(cmdArgs.outputVcf, cmdArgs.referenceFasta)
        logger.info("No header and records found in file")
    }

    logger.info("Done")
  }

  def writeEmptyVcf(outputVcf: File, referenceFasta: File): Unit = {
    val vcfHeader = new VCFHeader()
    vcfHeader.setSequenceDictionary(FastaUtils.getCachedDict(referenceFasta))
    val writer = new VariantContextWriterBuilder()
      .setOutputFile(outputVcf)
      .setReferenceDictionary(vcfHeader.getSequenceDictionary)
      .unsetOption(Options.INDEX_ON_THE_FLY)
      .build
    writer.writeHeader(vcfHeader)
    writer.close()
  }

  def parseLines(header: String, lineIt: Iterator[String], cmdArgs: Args): Unit = {
    val headerKeys = header.split(" ")
    val headerMap = headerKeys.zipWithIndex.toMap
    require(headerKeys.size == headerMap.size, "Duplicates header keys found")
    val metaLines = new util.HashSet[VCFHeaderLine]()
    for (
      key <- headerKeys if key != "rsid" if key != "chromosome" if key != "position" if key != "alleleA" if key != "alleleB" if key != "alleleA"
    ) metaLines.add(new VCFInfoHeaderLine(s"ST_$key", 1, VCFHeaderLineType.String, ""))

    require(FastaUtils.getCachedDict(cmdArgs.referenceFasta).getSequence(cmdArgs.contig) != null,
      s"contig '${cmdArgs.contig}' not found on reference")

    val vcfHeader = new VCFHeader(metaLines)
    vcfHeader.setSequenceDictionary(FastaUtils.getCachedDict(cmdArgs.referenceFasta))
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder()
      .setOutputFile(cmdArgs.outputVcf)
      .setReferenceDictionary(vcfHeader.getSequenceDictionary)
      .unsetOption(Options.INDEX_ON_THE_FLY)
      .build)
    writer.writeHeader(vcfHeader)

    val infoKeys = for (
      key <- headerKeys if key != "rsid" if key != "chromosome" if key != "position" if key != "alleleA" if key != "alleleB" if key != "alleleA"
    ) yield key

    var counter = 0
    for (line <- lineIt if !line.startsWith("#")) {
      val values = line.split(" ")
      require(values.size == headerKeys.size, "Number of values are not the same as number of header keys")
      val alleles = List(Allele.create(values(headerMap("alleleA")), true), Allele.create(values(headerMap("alleleB"))))
      val start = values(headerMap("position")).toLong
      val end = alleles.head.length() + start - 1
      val rsid = values(headerMap("rsid"))
      val builder = (new VariantContextBuilder)
        .chr(cmdArgs.contig)
        .alleles(alleles)
        .start(start)
        .stop(end)
        .noGenotypes()

      val infoBuilder = infoKeys.foldLeft(builder) { case (a, b) => a.attribute("ST_" + b, values(headerMap(b)).replaceAll(";", ",")) }

      writer.add(infoBuilder.id(rsid.replaceAll(";", ",")).make())

      counter += 1
      if (counter % 10000 == 0) logger.info(s"$counter lines processed")
    }

    logger.info(s"$counter lines processed")

    writer.close()

  }
}
