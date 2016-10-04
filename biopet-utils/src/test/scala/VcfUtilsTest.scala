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

}
