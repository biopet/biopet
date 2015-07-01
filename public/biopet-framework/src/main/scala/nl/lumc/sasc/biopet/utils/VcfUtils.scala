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
    bases + (Array.fill[Char](newSize - bases.size)(fillWith)).mkString
  }
}
