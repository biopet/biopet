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

import htsjdk.samtools.reference.{ FastaSequenceFile, ReferenceSequenceFileFactory }
import htsjdk.variant.variantcontext.writer.{ AsyncVariantContextWriter, VariantContextWriterBuilder }
import htsjdk.variant.variantcontext.{ Allele, GenotypeBuilder, VariantContextBuilder }
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils.{ FastaUtils, ToolCommand }

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by pjvanthof on 15/03/16.
 */
object GensToVcf extends ToolCommand {

  case class Args(inputGenotypes: File = null,
                  inputInfo: Option[File] = None,
                  outputVcf: File = null,
                  sampleFile: File = null,
                  referenceFasta: File = null,
                  contig: String = null,
                  sortInput: Boolean = false) extends AbstractArgs

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
    opt[File]('R', "referenceFasta") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(referenceFasta = x)
    } text "reference fasta file"
    opt[String]('c', "contig") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(contig = x)
    } text "contig of impute file"
    opt[Unit]("sortInput") maxOccurs 1 action { (x, c) =>
      c.copy(sortInput = true)
    } text "In memory sorting"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()).getOrElse(throw new IllegalArgumentException)

    val samples = Source.fromFile(cmdArgs.sampleFile).getLines().toArray.drop(2).map(_.split("\t").take(2).mkString("_"))

    val infoIt = cmdArgs.inputInfo.map(Source.fromFile(_).getLines())
    val infoHeaderKeys = infoIt.map(_.next().split(" ").filterNot(x => x == "rs_id" || x == "position"))
    val infoHeaderMap = infoHeaderKeys.map(_.zipWithIndex.toMap)

    val metaLines = new util.HashSet[VCFHeaderLine]()
    for (keys <- infoHeaderKeys; key <- keys)
      metaLines.add(new VCFInfoHeaderLine(s"GENS_$key", 1, VCFHeaderLineType.String, ""))

    metaLines.add(new VCFFormatHeaderLine("GT", 1, VCFHeaderLineType.String, ""))
    metaLines.add(new VCFFormatHeaderLine("GP", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Float, ""))

    require(FastaUtils.getCachedDict(cmdArgs.referenceFasta).getSequence(cmdArgs.contig) != null,
      s"contig '${cmdArgs.contig}' not found on reference")

    val header = new VCFHeader(metaLines, samples.toList)
    header.setSequenceDictionary(FastaUtils.getCachedDict(cmdArgs.referenceFasta))
    val writer = new AsyncVariantContextWriter(new VariantContextWriterBuilder()
      .setOutputFile(cmdArgs.outputVcf)
      .setReferenceDictionary(header.getSequenceDictionary)
      .build)
    writer.writeHeader(header)

    val genotypeIt = Source.fromFile(cmdArgs.inputGenotypes).getLines()

    lazy val fastaFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(cmdArgs.referenceFasta, true, true)

    case class Line(genotype: String, info: Option[String])
    val lineIt: Iterator[Line] = {
      val it = infoIt match {
        case Some(x) => genotypeIt.zip(x).map(x => Line(x._1, Some(x._2)))
        case _       => genotypeIt.map(x => Line(x, None))
      }

      if (cmdArgs.sortInput) {
        logger.info("Start Sorting input files")
        val list = it.toList
        val pos = list.map { line =>
          val values = line.genotype.split(" ")
          val p = values(2).toInt
          val alt = values(4)
          if (alt == "-") p - 1
          else p
        }
        list.zip(pos).sortBy(_._2).map(_._1).toIterator
      } else it
    }

    logger.info("Start processing genotypes")
    var count = 0L
    for (line <- lineIt) {
      val genotypeValues = line.genotype.split(" ")
      val infoValues = line.info.map(_.split(" "))

      val (start, end, ref, alt) = {
        val start = genotypeValues(2).toInt
        if (genotypeValues(4) == "-") {
          val seq = fastaFile.getSubsequenceAt(cmdArgs.contig, start - 1, start + genotypeValues(4).length - 1)
          (start - 1, start + genotypeValues(4).length - 1,
            Allele.create(new String(seq.getBases), true), Allele.create(new String(Array(seq.getBases.head))))
        } else {
          val ref = Allele.create(genotypeValues(3), true)
          (start, ref.length - 1 + start, Allele.create(genotypeValues(3), true), Allele.create(genotypeValues(4)))
        }
      }
      val genotypes = samples.toList.zipWithIndex.map {
        case (sampleName, index) =>
          val gps = Array(
            genotypeValues(5 + (index * 3)),
            genotypeValues(5 + (index * 3) + 1),
            genotypeValues(5 + (index * 3) + 2)
          ).map(_.toDouble)
          val alleles = gps.indexOf(gps.max) match {
            case 0 => List(ref, ref)
            case 1 => List(ref, alt)
            case 2 => List(alt, alt)
          }
          new GenotypeBuilder()
            .name(sampleName)
            .alleles(alleles)
            .attribute("GP", gps)
            .make()
      }

      val infoMap = infoHeaderKeys
        .map(_.map(x => ("GENS_" + x) -> infoValues.get(infoHeaderMap.get(x)).replaceAll(";", ",")).toMap)
        .getOrElse(Map())

      val builder = (new VariantContextBuilder)
        .chr(cmdArgs.contig)
        .alleles(List(ref, alt))
        .attributes(infoMap)
        .start(start)
        .stop(end)
        .genotypes(genotypes)
      val id = genotypeValues(1)
      if (id.startsWith(cmdArgs.contig + ":")) writer.add(builder.make())
      else writer.add(builder.id(id).make())
      count += 1
      if (count % 10000 == 0) logger.info(s"$count lines processed")
    }

    logger.info(s"$count lines processed")

    writer.close()

    logger.info("Done")
  }
}
