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
import htsjdk.variant.variantcontext.writer.{
  AsyncVariantContextWriter,
  VariantContextWriterBuilder
}
import htsjdk.variant.variantcontext._
import htsjdk.variant.vcf._
import nl.lumc.sasc.biopet.utils._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

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
    opt[File]('b', "bam") unbounded () action { (x, c) =>
      c.copy(bamFiles = x :: c.bamFiles)
    } text "bam file, from which the variants (VCF files) were called"
    opt[File]("bamList") unbounded () action { (x, c) =>
      c.copy(bamFiles = Source.fromFile(x).getLines().map(new File(_)).toList ::: c.bamFiles)
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

    logger.info("Start")

    val dict = FastaUtils.getCachedDict(cmdArgs.referenceFasta)

    logger.info(cmdArgs.bamFiles.size + " bamfiles found")

    val bamReaders = BamUtils.sampleBamReaderMap(cmdArgs.bamFiles)
    val sampleReadergroup = BamUtils.sampleReadGroups(bamReaders)

    logger.info(s"Samples found: ${sampleReadergroup.size}")
    logger.info(s"Readgroups found: ${sampleReadergroup.map(_._2.size).sum}")

    val reader = new VCFFileReader(cmdArgs.inputFile, false)
    val writer = new AsyncVariantContextWriter(
      new VariantContextWriterBuilder()
        .setReferenceDictionary(dict)
        .setOutputFile(cmdArgs.outputFile)
        .build)

    val headerLines: Set[VCFHeaderLine] = Set(
      new VCFFormatHeaderLine("GT",
                              VCFHeaderLineCount.R,
                              VCFHeaderLineType.String,
                              "Genotype of position"),
      new VCFFormatHeaderLine("AD",
                              VCFHeaderLineCount.R,
                              VCFHeaderLineType.Integer,
                              "Allele depth, ref and alt on order of vcf file"),
      new VCFFormatHeaderLine("DP", 1, VCFHeaderLineType.Integer, "Depth of position")
    )
    val sampleNames =
      (if (cmdArgs.outputReadgroups)
         sampleReadergroup
           .flatMap(x => x._1 :: x._2.map(rg => rg.getSample + "-" + rg.getReadGroupId))
           .toList
       else sampleReadergroup.keys.toList).sorted

    val header = new VCFHeader(headerLines, sampleNames)
    header.setSequenceDictionary(dict)
    writer.writeHeader(header)

    val it = for (vcfRecord <- reader.iterator().buffered) yield {
      val countReports = bamReaders.map(x =>
        x._1 -> Future(countAlleles(vcfRecord, x._2._1, sampleReadergroup(x._1))))

      val builder = new VariantContextBuilder()
        .chr(vcfRecord.getContig)
        .start(vcfRecord.getStart)
        .alleles(vcfRecord.getAlleles)
        .computeEndFromAlleles(vcfRecord.getAlleles, vcfRecord.getStart)
      val genotypes = for ((sampleName, countsFuture) <- countReports) yield {
        val counts = Await.result(countsFuture, Duration.Inf)
        val sampleGenotype =
          counts.values.fold(AlleleCounts())(_ + _).toGenotype(sampleName, vcfRecord)
        if (cmdArgs.outputReadgroups)
          sampleGenotype :: counts
            .map(x => x._2.toGenotype(x._1.getSample + "-" + x._1.getReadGroupId, vcfRecord))
            .toList
        else List(sampleGenotype)
      }
      builder.genotypes(genotypes.flatten).make
    }
    var c = 0L
    it.buffered.foreach { record =>
      c += 1
      if (c % 1000 == 0) logger.info(s"$c variants done")
      writer.add(record)
    }
    logger.info(s"$c variants done")
    bamReaders.foreach(_._2._1.close())
    reader.close()
    writer.close()

    logger.info("Done")
  }

  protected case class AlleleCounts(count: Map[Allele, Int] = Map(), dp: Int = 0) {
    def +(other: AlleleCounts): AlleleCounts = {
      val alleles = this.count.keySet ++ other.count.keySet
      val map = alleles.map(a => a -> (this.count.getOrElse(a, 0) + other.count.getOrElse(a, 0)))
      AlleleCounts(map.toMap, other.dp + this.dp)
    }

    def toGenotype(sampleName: String, vcfRecord: VariantContext): Genotype = {
      val alleles =
        if (count.forall(_._2 == 0)) List(Allele.NO_CALL, Allele.NO_CALL)
        else {
          val f = count.toList.map(x => (x._2.toDouble / dp) -> x._1)
          val maxF = f.map(_._1).max
          val firstAllele = f.find(_._1 == maxF).get._2
          if (maxF > 0.8) List(firstAllele, firstAllele)
          else {
            val leftOver = f.filter(_._2 != firstAllele)
            val maxF2 = (0.0 :: leftOver.map(_._1)).max
            val secondAllele = leftOver.find(_._1 == maxF2).map(_._2).getOrElse(Allele.NO_CALL)
            List(firstAllele, secondAllele)
          }
        }
      new GenotypeBuilder(sampleName)
        .alleles(alleles.sortBy(a => vcfRecord.getAlleleIndex(a)))
        .DP(dp)
        .AD(vcfRecord.getAlleles.map(count.getOrElse(_, 0)).toArray)
        .make()
    }
  }

  def countAlleles(vcfRecord: VariantContext,
                   samReader: SamReader,
                   readGroups: List[SAMReadGroupRecord]): Map[SAMReadGroupRecord, AlleleCounts] = {
    val map = samReader
      .query(vcfRecord.getContig, vcfRecord.getStart, vcfRecord.getEnd, false)
      .toList
      .groupBy(_.getReadGroup)
      .map {
        case (readGroup, rs) =>
          val count = rs
            .flatMap(CheckAllelesVcfInBam.checkAlleles(_, vcfRecord))
            .groupBy(x => x)
            .map(x => vcfRecord.getAllele(x._1) -> x._2.size)
          readGroup -> AlleleCounts(count, rs.size)
      }
    readGroups.map(rg => rg -> map.getOrElse(rg, AlleleCounts())).toMap
  }
}
