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

import htsjdk.samtools._
import htsjdk.variant.variantcontext.writer.{AsyncVariantContextWriter, VariantContextWriterBuilder}
import htsjdk.variant.variantcontext._
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils._

import scala.collection.JavaConversions._
import scala.collection.mutable

object CountAlleles extends ToolCommand {
  case class Args(inputFile: File = null,
                  outputFile: File = null,
                  bamFiles: List[File] = Nil,
                  minMapQual: Int = 1,
                  referenceFasta: File = null,
                  outputReadgroups: Boolean = false)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "inputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "VCF file"
    opt[File]('o', "outputFile") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "output VCF file name"
    opt[File]('b', "bam") unbounded () minOccurs 1 action { (x, c) =>
      c.copy(bamFiles = x :: c.bamFiles)
    } text "bam file, from which the variants (VCF files) were called"
    opt[Int]('m', "min_mapping_quality") maxOccurs 1 action { (x, c) =>
      c.copy(minMapQual = x)
    } text "minimum mapping quality score for a read to be taken into account"
    opt[File]('R', "referenceFasta") required () maxOccurs 1 action { (x, c) =>
      c.copy(referenceFasta = x)
    } text "reference fasta"
    opt[Unit]("outputReadgroups") maxOccurs 1 action { (_, c) =>
      c.copy(outputReadgroups = true)
    } text "Output each readgroup separated"
  }

  private class CountReport(var notFound: Int = 0,
                            var aCounts: mutable.Map[String, Int] = mutable.Map(),
                            var duplicateReads: Int = 0,
                            var lowMapQualReads: Int = 0)

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val dict = FastaUtils.getCachedDict(cmdArgs.referenceFasta)

    val sampleBamFiles = BamUtils.sampleBamMap(cmdArgs.bamFiles)
    val sampleReadergroup = BamUtils.sampleReadGroups(cmdArgs.bamFiles)
    val samReaderFactory = SamReaderFactory.makeDefault
    val bamReaders: Map[String, SamReader] = sampleBamFiles.map(x => x._1 -> samReaderFactory.open(x._2))

    val reader = new VCFFileReader(cmdArgs.inputFile, false)
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setReferenceDictionary(dict)
        .setOutputFile(cmdArgs.outputFile)
        .build)

    val headerLines: Set[VCFHeaderLine] = Set(new VCFFormatHeaderLine("GT",
      VCFHeaderLineCount.R,
      VCFHeaderLineType.String,
      "Genotype of position"),
    new VCFFormatHeaderLine("AD",
      VCFHeaderLineCount.R,
      VCFHeaderLineType.Integer,
      "Allele depth, ref and alt on order of vcf file"),
    new VCFFormatHeaderLine("DP",
      1,
      VCFHeaderLineType.Integer,
      "Depth of position"))
    val sampleNames = if (cmdArgs.outputReadgroups)
      sampleReadergroup.flatMap(x => x._1 :: x._2.map(rg => rg.getSample + "-" + rg.getReadGroupId)).toList
    else sampleBamFiles.keys.toList

    val header = new VCFHeader(headerLines, sampleNames)
    header.setSequenceDictionary(dict)
    writer.writeHeader(header)

    for (vcfRecord <- reader) {
      val countReports = bamReaders.map(x => x._1 -> countAlleles(vcfRecord, x._2))

      val builder = new VariantContextBuilder()
        .chr(vcfRecord.getContig)
        .start(vcfRecord.getStart)
        .alleles(vcfRecord.getAlleles)
      val genotypes = for ((sampleName, counts) <- countReports) yield {
        val sampleGenotype = counts.values.reduce(_ + _).toGenotype(sampleName, vcfRecord)
        if (cmdArgs.outputReadgroups) sampleGenotype :: counts.map(x => x._2.toGenotype(x._1.getSample + "-" + x._1.getReadGroupId, vcfRecord)).toList
        else List(sampleGenotype)
      }
      builder.genotypes(genotypes.flatten)
      writer.add(builder.make)
    }
    bamReaders.foreach(_._2.close())
    reader.close()
    writer.close()
  }

  protected case class AlleleCounts(count: Map[Allele, Int], dp: Int) {
    def +(other: AlleleCounts): AlleleCounts = {
      val alleles = this.count.keySet ++ other.count.keySet
      val map = alleles.map(a => a -> (this.count.getOrElse(a, 0) + other.count.getOrElse(a, 0)))
      AlleleCounts(map.toMap, other.dp + this.dp)
    }

    def toGenotype(sampleName: String, vcfRecord: VariantContext): Genotype = {
      new GenotypeBuilder(sampleName)
        .attribute("DP", dp)
        .attribute("AD", vcfRecord.getAlleles.map(count.getOrElse(_, 0)).toArray)
        .make()
    }
  }

  def countAlleles(vcfRecord: VariantContext, samReader: SamReader): Map[SAMReadGroupRecord, AlleleCounts] = {
    samReader
      .query(vcfRecord.getContig, vcfRecord.getStart, vcfRecord.getEnd, false)
      .toList
      .groupBy(_.getReadGroup)
      .map { case (readGroup, rs) =>
        val count = rs.flatMap(checkAlleles(_, vcfRecord)).groupBy(x => x).map(x => vcfRecord.getAllele(x._1) -> x._2.size)
        readGroup -> AlleleCounts(count, rs.size)
      }
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
