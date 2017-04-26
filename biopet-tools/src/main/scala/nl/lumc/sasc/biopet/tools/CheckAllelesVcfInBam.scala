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

import htsjdk.samtools.{QueryInterval, SAMRecord, SamReader, SamReaderFactory}
import htsjdk.variant.variantcontext.{VariantContext, VariantContextBuilder}
import htsjdk.variant.variantcontext.writer.{
  AsyncVariantContextWriter,
  VariantContextWriterBuilder
}
import htsjdk.variant.vcf.{VCFFileReader, VCFHeaderLineCount, VCFHeaderLineType, VCFInfoHeaderLine}
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.collection.JavaConversions._
import scala.collection.mutable

//class CheckAllelesVcfInBam(val root: Configurable) extends BiopetJavaCommandLineFunction {
//  javaMainClass = getClass.getName
//
//  @Input(doc = "Input vcf file", shortName = "V", required = true)
//  var inputFile: File = _
//
//  @Input(doc = "Input bam file", shortName = "bam", required = true)
//  var bamFiles: List[File] = Nil
//
//  @Output(doc = "Output vcf file", shortName = "o", required = true)
//  var output: File = _
//
//  override def commandLine = super.commandLine + required("-V", inputFile) + repeat("-bam", bamFiles) + required(output)
//}

object CheckAllelesVcfInBam extends ToolCommand {
  case class Args(inputFile: File = null,
                  outputFile: File = null,
                  samples: List[String] = Nil,
                  bamFiles: List[File] = Nil,
                  minMapQual: Int = 1)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "VCF file"
    opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "output VCF file name"
    opt[String]('s', "sample") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(samples = x :: c.samples)
    } text "sample name"
    opt[File]('b', "bam") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(bamFiles = x :: c.bamFiles)
    } text "bam file, from which the variants (VCF files) were called"
    opt[Int]('m', "min_mapping_quality") maxOccurs 1 action { (x, c) =>
      c.copy(minMapQual = c.minMapQual)
    } text "minimum mapping quality score for a read to be taken into account"
  }

  private class CountReport(var notFound: Int = 0,
                            var aCounts: mutable.Map[String, Int] = mutable.Map(),
                            var duplicateReads: Int = 0,
                            var lowMapQualReads: Int = 0)

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (commandArgs.bamFiles.size != commandArgs.samples.size)
      logger.warn(
        "Number of samples is different from number of bam files: additional samples or bam files will not be used")
    val samReaderFactory = SamReaderFactory.makeDefault
    val bamReaders: Map[String, SamReader] = Map(
      commandArgs.samples zip commandArgs.bamFiles.map(x => samReaderFactory.open(x)): _*)
    val bamHeaders = bamReaders.map(x => (x._1, x._2.getFileHeader))

    val reader = new VCFFileReader(commandArgs.inputFile, false)
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setOutputFile(commandArgs.outputFile)
        .setReferenceDictionary(reader.getFileHeader.getSequenceDictionary)
        .build)

    val header = reader.getFileHeader
    for ((sample, _) <- bamReaders) {
      header.addMetaDataLine(
        new VCFInfoHeaderLine("BAM-AD-" + sample,
                              VCFHeaderLineCount.UNBOUNDED,
                              VCFHeaderLineType.Integer,
                              "Allele depth, ref and alt on order of vcf file"))
      header.addMetaDataLine(
        new VCFInfoHeaderLine("BAM-DP-" + sample,
                              1,
                              VCFHeaderLineType.Integer,
                              "Total reads on this location"))
    }

    writer.writeHeader(header)

    for (vcfRecord <- reader) {
      val countReports: Map[String, CountReport] = bamReaders.map(x => (x._1, new CountReport))
      val refAllele = vcfRecord.getReference.getBaseString
      for ((sample, bamReader) <- bamReaders) {
        val queryInterval = new QueryInterval(
          bamHeaders(sample).getSequenceIndex(vcfRecord.getContig),
          vcfRecord.getStart,
          vcfRecord.getStart + refAllele.length - 1)
        val bamIter = bamReader.query(Array(queryInterval), false)

        def filterRead(samRecord: SAMRecord): Boolean = {
          if (samRecord.getDuplicateReadFlag) {
            countReports(sample).duplicateReads += 1
            return true
          }
          if (samRecord.getSupplementaryAlignmentFlag) return true
          if (samRecord.getNotPrimaryAlignmentFlag) return true
          if (samRecord.getMappingQuality < commandArgs.minMapQual) {
            countReports(sample).lowMapQualReads += 1
            return true
          }
          false
        }

        val counts = for (samRecord <- bamIter if !filterRead(samRecord)) {
          checkAlleles(samRecord, vcfRecord) match {
            case Some(a) =>
              if (countReports(sample).aCounts.contains(a)) countReports(sample).aCounts(a) += 1
              else countReports(sample).aCounts += (a -> 1)
            case _ => countReports(sample).notFound += 1
          }
        }
        bamIter.close()
      }

      val builder = new VariantContextBuilder(vcfRecord)
      for ((k, v) <- countReports) {
        val s = for (allele <- vcfRecord.getAlleles) yield {
          val s = allele.getBaseString
          if (v.aCounts.contains(s)) v.aCounts(s)
          else 0
        }
        builder.attribute("BAM-AD-" + k, s.mkString(","))
        builder.attribute("BAM-DP-" + k, (0 /: s)(_ + _) + v.notFound)
      }
      writer.add(builder.make)
    }
    for ((_, r) <- bamReaders) r.close()
    reader.close()
    writer.close()
  }

  def checkAlleles(samRecord: SAMRecord, vcfRecord: VariantContext): Option[String] = {
    val readStartPos = List
      .range(0, samRecord.getReadBases.length)
      .find(x => samRecord.getReferencePositionAtReadPosition(x + 1) == vcfRecord.getStart) getOrElse {
      return None
    }
    val readBases = samRecord.getReadBases
    val alleles = vcfRecord.getAlleles.map(x => x.getBaseString)
    val refAllele = alleles.head
    var maxSize = 1
    for (allele <- alleles if allele.length > maxSize) maxSize = allele.length
    val readC = for (t <- readStartPos until readStartPos + maxSize if t < readBases.length)
      yield readBases(t).toChar
    val allelesInRead = mutable.Set(alleles.filter(readC.mkString.startsWith): _*)

    // Removal of insertions that are not really in the cigarstring
    for (allele <- allelesInRead if allele.length > refAllele.length) {
      val refPos = for (t <- refAllele.length until allele.length)
        yield samRecord.getReferencePositionAtReadPosition(readStartPos + t + 1)
      if (refPos.exists(_ > 0)) allelesInRead -= allele
    }

    // Removal of alleles that are not really in the cigarstring
    for (allele <- allelesInRead) {
      val readPosAfterAllele =
        samRecord.getReferencePositionAtReadPosition(readStartPos + allele.length + 1)
      val vcfPosAfterAllele = vcfRecord.getStart + refAllele.length
      if (readPosAfterAllele != vcfPosAfterAllele &&
          (refAllele.length != allele.length || (refAllele.length == allele.length && readPosAfterAllele < 0)))
        allelesInRead -= allele
    }

    for (allele <- allelesInRead if allele.length >= refAllele.length) {
      if (allelesInRead.exists(_.length > allele.length)) allelesInRead -= allele
    }
    if (allelesInRead.contains(refAllele) && allelesInRead.exists(_.length < refAllele.length))
      allelesInRead -= refAllele
    if (allelesInRead.isEmpty) None
    else if (allelesInRead.size == 1) Some(allelesInRead.head)
    else {
      logger.warn("vcfRecord: " + vcfRecord)
      logger.warn("samRecord: " + samRecord.getSAMString)
      logger.warn("Found multiple options: " + allelesInRead.toString)
      logger.warn("ReadStartPos: " + readStartPos + "  Read Length: " + samRecord.getReadLength)
      logger.warn("Read skipped, please report this")
      None
    }
  }
}
