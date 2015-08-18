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

import java.io.{InputStream, File}

import htsjdk.samtools.util.Interval
import htsjdk.samtools.{QueryInterval, SAMRecord, SamReader, SamReaderFactory}
import htsjdk.tribble.AbstractFeatureReader._
import htsjdk.tribble.TabixFeatureReader
import htsjdk.tribble.bed.{SimpleBEDFeature, BEDCodec, FullBEDFeature}
import htsjdk.variant.variantcontext.writer.{AsyncVariantContextWriter, VariantContextWriterBuilder}
import htsjdk.variant.variantcontext.{VariantContext, VariantContextBuilder}
import htsjdk.variant.vcf.{VCFFileReader, VCFHeaderLineCount, VCFHeaderLineType, VCFInfoHeaderLine}
import nl.lumc.sasc.biopet.core.ToolCommand

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable

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
    opt[File]('v', "vcfFile") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(vcfFiles = x :: c.vcfFiles)
    }
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    //val bedReader = new TabixFeatureReader[BEDCodec, File](cmdArgs.bedFile.getAbsolutePath, new BEDCodec)

    val bedIt = asScalaIteratorConverter(getFeatureReader(cmdArgs.bedFile.toPath.toString, new BEDCodec(), false).iterator).asScala
    for ( bedRecord <- bedIt) {
      bedRecord.getName()
    }
  }
}