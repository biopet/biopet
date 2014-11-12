package nl.lumc.sasc.biopet.util

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

  def fillAllele(bases: String, newSize: Int, fillWith: Char = 'N'): String = {
    bases + (Array.fill[Char](newSize - bases.size)(fillWith)).mkString
  }
}
