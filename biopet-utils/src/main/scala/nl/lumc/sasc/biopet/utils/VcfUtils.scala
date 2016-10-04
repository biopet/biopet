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
package nl.lumc.sasc.biopet.utils

import java.io.File
import java.util

import htsjdk.variant.variantcontext.{ Genotype, VariantContext }
import htsjdk.variant.vcf.{ VCFFileReader, VCFHeader, VCFFilterHeaderLine }

import scala.collection.JavaConversions._

/** Utility object for general vcf file/records functions. */
object VcfUtils {
  /**
   * Return longest allele of VariantContext.
   *
   * @param vcfRecord record to check
   * @return allele with most nucleotides
   */
  def getLongestAllele(vcfRecord: VariantContext) = {
    val alleles = vcfRecord.getAlleles
    val longestAlleleId = alleles.map(_.getBases.length).zipWithIndex.maxBy(_._1)._2
    alleles(longestAlleleId)
  }

  /**
   * Method will extend a allele till a new length
   * @param bases Allele
   * @param newSize New size of allele
   * @param fillWith Char to fill gap
   * @return
   */
  def fillAllele(bases: String, newSize: Int, fillWith: Char = '-'): String = {
    bases + Array.fill[Char](newSize - bases.length)(fillWith).mkString
  }

  /**
   * Stands for scalaListToJavaObjectArrayList
   * Convert a scala List[Any] to a java ArrayList[Object]. This is necessary for BCF conversions
   * As scala ints and floats cannot be directly cast to java objects (they aren't objects),
   * we need to box them.
   * For items not Int, Float or Object, we assume them to be strings (TODO: sane assumption?)
   * @param array scala List[Any]
   * @return converted java ArrayList[Object]
   */
  def scalaListToJavaObjectArrayList(array: List[Any]): util.ArrayList[Object] = {
    val out = new util.ArrayList[Object]()

    array.foreach {
      case x: Long    => out.add(Long.box(x))
      case x: Int     => out.add(Int.box(x))
      case x: Char    => out.add(Char.box(x))
      case x: Byte    => out.add(Byte.box(x))
      case x: Double  => out.add(Double.box(x))
      case x: Float   => out.add(Float.box(x))
      case x: Boolean => out.add(Boolean.box(x))
      case x: String  => out.add(x)
      case x: Object  => out.add(x)
      case x          => out.add(x.toString)
    }
    out
  }

  //TODO: Add genotype comparing to this function
  def identicalVariantContext(var1: VariantContext, var2: VariantContext): Boolean = {
    var1.getContig == var2.getContig &&
      var1.getStart == var2.getStart &&
      var1.getEnd == var2.getEnd &&
      var1.getAttributes == var2.getAttributes
  }

  /**
   * Return true if header is a block-type GVCF file
   * @param header header of Vcf file
   * @return boolean
   */
  def isBlockGVcf(header: VCFHeader): Boolean = {
    header.getMetaDataLine("GVCFBlock") != null
  }

  /**
   * Get sample IDs from vcf File
   * @param vcf File object pointing to vcf
   * @return list of strings with sample IDs
   */
  def getSampleIds(vcf: File): List[String] = {
    val reader = new VCFFileReader(vcf, false)
    val samples = reader.getFileHeader.getSampleNamesInOrder.toList
    reader.close()
    samples
  }

  /**
   * Check whether record has minimum genome Quality
   * @param record variant context
   * @param sample sample name
   * @param minGQ minimum genome quality value
   * @return
   */
  def hasMinGenomeQuality(record: VariantContext, sample: String, minGQ: Int): Boolean = {
    if (!record.getSampleNamesOrderedByName.contains(sample))
      throw new IllegalArgumentException("Sample does not exist")
    val gt = record.getGenotype(sample)
    hasMinGenomeQuality(gt, minGQ)
  }

  /**
   * Check whether genotype has minimum genome Quality
   * @param gt Genotype
   * @param minGQ minimum genome quality value
   * @return
   */
  def hasMinGenomeQuality(gt: Genotype, minGQ: Int): Boolean = {
    gt.hasGQ && gt.getGQ >= minGQ
  }

  def getVcfIndexFile(vcfFile: File): File = {
    val name = vcfFile.getAbsolutePath
    if (name.endsWith(".vcf")) new File(name + ".idx")
    else if (name.endsWith(".vcf.gz")) new File(name + ".tbi")
    else throw new IllegalArgumentException(s"File given is no vcf file: $vcfFile")
  }

  def vcfFileIsEmpty(file: File): Boolean = {
    val reader = new VCFFileReader(file, false)
    val hasNext = reader.iterator().hasNext
    reader.close()
    !hasNext
  }

  /**
   * Check whether genotype is of the form 0/.
   * @param genotype genotype
   * @return boolean
   */
  def isCompoundNoCall(genotype: Genotype): Boolean = {
    genotype.isCalled && genotype.getAlleles.exists(_.isNoCall) && genotype.getAlleles.exists(_.isReference)
  }
}
