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

import java.io.{PrintWriter, InputStream, File}
import java.util

import htsjdk.samtools.util.Interval
import htsjdk.samtools.{QueryInterval, SAMRecord, SamReader, SamReaderFactory}
import htsjdk.tribble.AbstractFeatureReader._
import htsjdk.tribble.{AbstractFeatureReader, TabixFeatureReader}
import htsjdk.tribble.bed.{BEDFeature, SimpleBEDFeature, BEDCodec, FullBEDFeature}
import htsjdk.variant.variantcontext.writer.{AsyncVariantContextWriter, VariantContextWriterBuilder}
import htsjdk.variant.variantcontext.{VariantContext, VariantContextBuilder}
import htsjdk.variant.vcf.{VCFFileReader, VCFHeaderLineCount, VCFHeaderLineType, VCFInfoHeaderLine}
import nl.lumc.sasc.biopet.core.ToolCommand

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source
import scala.math._

object RegionAfCount extends ToolCommand {
  case class Args(bedFile: File = null,
                  outputFile: File = null,
                  vcfFiles: List[File] = Nil) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('b', "bedFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(bedFile = x)
    }
    opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[File]('V', "vcfFile") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(vcfFiles = x :: c.vcfFiles)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val regions = (for (line <- Source.fromFile(cmdArgs.bedFile).getLines()) yield {
      val values = line.split("\t")
      require(values.length >= 3, "to less columns in bed file")
      val name = if (values.length >= 4) values(3)
      else values(0) + ":" + values(1) + "-" + values(2)
      new Interval(values(0), values(1).toInt, values(2).toInt, true, name)
    }).toList

    val counts = (for (region <- regions) yield region.getName -> {
      (for (vcfFile <- cmdArgs.vcfFiles) yield vcfFile -> {
        val reader = new VCFFileReader(vcfFile, true)
        val it = reader.query(region.getContig, region.getStart, region.getEnd)
        val sum = (for (v <- it) yield {
          val bla = v.getAttribute("AF", 0) match {
            case a:util.ArrayList[_] => a.map(_.toString.toDouble).toArray
            case s => Array(s.toString.toDouble)
          }
          bla.sum
        }).sum
        reader.close()
        sum
      }).toMap
    }).toMap

    val writer = new PrintWriter(cmdArgs.outputFile)
    writer.println("\t" + cmdArgs.vcfFiles.map(_.getName).mkString("\t"))
    for (c <- counts) {
      writer.print(c._1 + "\t")
      writer.println(cmdArgs.vcfFiles.map(c._2(_)).mkString("\t"))
    }
    writer.close()
  }
}