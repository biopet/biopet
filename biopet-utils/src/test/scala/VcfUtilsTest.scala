import htsjdk.variant.variantcontext.{ Allele, Genotype, GenotypeBuilder }
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.JavaConversions._

import nl.lumc.sasc.biopet.utils.VcfUtils

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
