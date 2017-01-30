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

import htsjdk.variant.variantcontext.{ Allele, GenotypeBuilder }
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.JavaConversions._

/**
 * Created by Sander Bollen on 4-10-16.
 */
class VcfUtilsTest extends TestNGSuite with Matchers {

  @Test
  def testCompoundNoCall(): Unit = {
    val noAllele = Allele.NO_CALL
    val refAllele = Allele.create("A", true)
    val compoundNoCall = GenotypeBuilder.create("sample_01", List(noAllele, refAllele))
    VcfUtils.isCompoundNoCall(compoundNoCall) shouldBe true

    val altAllele = Allele.create("G", false)
    val normalGenotype = GenotypeBuilder.create("sample_01", List(refAllele, altAllele))
    VcfUtils.isCompoundNoCall(normalGenotype) shouldBe false

    val completeNoCall = GenotypeBuilder.create("sample_01", List(noAllele, noAllele))
    VcfUtils.isCompoundNoCall(completeNoCall) shouldBe false
  }

  @Test
  def testAlleleOverlap(): Unit = {

    val a1 = Allele.create("G")
    val a2 = Allele.create("A")

    VcfUtils.alleleOverlap(List(a1, a1), List(a1, a1)) shouldBe 2
    VcfUtils.alleleOverlap(List(a2, a2), List(a2, a2)) shouldBe 2
    VcfUtils.alleleOverlap(List(a1, a2), List(a1, a2)) shouldBe 2
    VcfUtils.alleleOverlap(List(a1, a2), List(a2, a1)) shouldBe 2
    VcfUtils.alleleOverlap(List(a2, a1), List(a1, a2)) shouldBe 2
    VcfUtils.alleleOverlap(List(a2, a1), List(a2, a1)) shouldBe 2

    VcfUtils.alleleOverlap(List(a1, a2), List(a1, a1)) shouldBe 1
    VcfUtils.alleleOverlap(List(a2, a1), List(a1, a1)) shouldBe 1
    VcfUtils.alleleOverlap(List(a1, a1), List(a1, a2)) shouldBe 1
    VcfUtils.alleleOverlap(List(a1, a1), List(a2, a1)) shouldBe 1

    VcfUtils.alleleOverlap(List(a1, a1), List(a2, a2)) shouldBe 0
    VcfUtils.alleleOverlap(List(a2, a2), List(a1, a1)) shouldBe 0
  }

}
