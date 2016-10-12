package nl.lumc.sasc.biopet.utils

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by Sander Bollen on 12-10-16.
  */
class UtilsTest extends TestNGSuite with Matchers {

  val semanticVersion = "1.2.3"
  val semanticVersionWithBuild = "1.2.3-alpha0.1"
  val nonSemanticVersion = "v1222.1"

  @Test
  def testIsSemantic() = {
    isSemanticVersion(semanticVersion) shouldBe true
    isSemanticVersion(semanticVersionWithBuild) shouldBe true
    isSemanticVersion(nonSemanticVersion) shouldBe false
  }

  @Test
  def testMajorVersion() = {
    majorVersion(semanticVersion) shouldBe Some(1)
    majorVersion(semanticVersionWithBuild) shouldBe Some(1)
  }

  @Test
  def testMinorVersion() = {
    minorVersion(semanticVersion) shouldBe Some(2)
    minorVersion(semanticVersionWithBuild) shouldBe Some(2)
  }

  @Test
  def testPatchVersion() = {
    patchVersion(semanticVersion) shouldBe Some(3)
    patchVersion(semanticVersionWithBuild) shouldBe Some(3)
  }

}
