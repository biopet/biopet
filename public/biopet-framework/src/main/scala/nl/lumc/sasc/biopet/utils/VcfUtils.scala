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
package nl.lumc.sasc.biopet.utils

import java.util

import htsjdk.variant.variantcontext.VariantContext

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
   * HACK!!
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
      case x: Int    => out.add(Int.box(x))
      case x: Float  => out.add(Float.box(x))
      case x: String => out.add(x)
      case x: Object => out.add(x)
      case x         => out.add(x.toString)
    }
    out
  }

  def identicalVariantContext(var1: VariantContext, var2: VariantContext): Boolean = {
    if (var1.getContig != var2.getContig) {
      false
    }
    if (var1.getStart != var2.getStart) {
      false
    }
    if (var1.getEnd != var2.getEnd) {
      false
    }
    if (!var1.getAttributes.forall(x => var2.hasAttribute(x._1)) || !var2.getAttributes.forall(x => var1.hasAttribute(x._1))) {
      false
    }
    if (!var1.getAttributes.forall(x => var2.getAttribute(x._1) == var1.getAttribute(x._1)) ||
      !var2.getAttributes.forall(x => var1.getAttribute(x._1) == var2.getAttribute(x._1))) {
      false
    }

    true
  }
}
