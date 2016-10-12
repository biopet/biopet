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
    getSemanticVersion(semanticVersion).map(_.major) shouldBe Some(1)
    getSemanticVersion(semanticVersionWithBuild).map(_.major) shouldBe Some(1)
  }

  @Test
  def testMinorVersion() = {
    getSemanticVersion(semanticVersion).map(_.minor) shouldBe Some(2)
    getSemanticVersion(semanticVersionWithBuild).map(_.minor) shouldBe Some(2)
  }

  @Test
  def testPatchVersion() = {
    getSemanticVersion(semanticVersion).map(_.patch) shouldBe Some(3)
    getSemanticVersion(semanticVersionWithBuild).map(_.patch) shouldBe Some(3)
  }

  @Test
  def testBuildVersion() = {
    getSemanticVersion(semanticVersion).flatMap(_.build) shouldBe None
    getSemanticVersion(semanticVersionWithBuild).flatMap(_.build) shouldBe Some("alpha0.1")
  }
}
