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

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import nl.lumc.sasc.biopet.utils.SemanticVersion._

/**
  * Created by Sander Bollen on 12-10-16.
  * Here we test [[SemanticVersion]]
  */
class SemanticVersionTest extends TestNGSuite with Matchers {

  val semanticVersion = "1.2.3"
  val semanticVersionWith_v = "v1.2.3"
  val semanticVersionWith_V = "V1.2.3"
  val semanticVersionWithBuild = "1.2.3-alpha0.1"
  val nonSemanticVersion = "v1222.1"

  @Test
  def testIsSemantic(): Unit = {
    isSemanticVersion(semanticVersion) shouldBe true
    isSemanticVersion(semanticVersionWithBuild) shouldBe true
    isSemanticVersion(nonSemanticVersion) shouldBe false
  }

  @Test
  def testMajorVersion(): Unit = {
    getSemanticVersion(semanticVersion).map(_.major) shouldBe Some(1)
    getSemanticVersion(semanticVersionWith_v).map(_.major) shouldBe Some(1)
    getSemanticVersion(semanticVersionWith_V).map(_.major) shouldBe Some(1)
    getSemanticVersion(semanticVersionWithBuild).map(_.major) shouldBe Some(1)
  }

  @Test
  def testMinorVersion(): Unit = {
    getSemanticVersion(semanticVersion).map(_.minor) shouldBe Some(2)
    getSemanticVersion(semanticVersionWith_v).map(_.minor) shouldBe Some(2)
    getSemanticVersion(semanticVersionWith_V).map(_.minor) shouldBe Some(2)
    getSemanticVersion(semanticVersionWithBuild).map(_.minor) shouldBe Some(2)
  }

  @Test
  def testPatchVersion(): Unit = {
    getSemanticVersion(semanticVersion).map(_.patch) shouldBe Some(3)
    getSemanticVersion(semanticVersionWith_v).map(_.patch) shouldBe Some(3)
    getSemanticVersion(semanticVersionWith_V).map(_.patch) shouldBe Some(3)
    getSemanticVersion(semanticVersionWithBuild).map(_.patch) shouldBe Some(3)
  }

  @Test
  def testBuildVersion(): Unit = {
    getSemanticVersion(semanticVersion).flatMap(_.build) shouldBe None
    getSemanticVersion(semanticVersionWith_v).flatMap(_.build) shouldBe None
    getSemanticVersion(semanticVersionWith_V).flatMap(_.build) shouldBe None
    getSemanticVersion(semanticVersionWithBuild).flatMap(_.build) shouldBe Some("alpha0.1")
  }

  @Test
  def testGreaterThen(): Unit = {
    SemanticVersion(1, 1, 1) > SemanticVersion(1, 1, 1) shouldBe false
    SemanticVersion(1, 1, 1) > SemanticVersion(0, 1, 1) shouldBe true
    SemanticVersion(1, 1, 1) > SemanticVersion(1, 0, 1) shouldBe true
    SemanticVersion(1, 1, 1) > SemanticVersion(1, 1, 0) shouldBe true
    SemanticVersion(1, 1, 1) > SemanticVersion(2, 1, 1) shouldBe false
    SemanticVersion(1, 1, 1) > SemanticVersion(1, 2, 1) shouldBe false
    SemanticVersion(1, 1, 1) > SemanticVersion(1, 1, 2) shouldBe false
  }

  @Test
  def testLesserThen(): Unit = {
    SemanticVersion(1, 1, 1) < SemanticVersion(1, 1, 1) shouldBe false
    SemanticVersion(1, 1, 1) < SemanticVersion(0, 1, 1) shouldBe false
    SemanticVersion(1, 1, 1) < SemanticVersion(1, 0, 1) shouldBe false
    SemanticVersion(1, 1, 1) < SemanticVersion(1, 1, 0) shouldBe false
    SemanticVersion(1, 1, 1) < SemanticVersion(2, 1, 1) shouldBe true
    SemanticVersion(1, 1, 1) < SemanticVersion(1, 2, 1) shouldBe true
    SemanticVersion(1, 1, 1) < SemanticVersion(1, 1, 2) shouldBe true
  }

  @Test
  def testGreaterThenOrEqual(): Unit = {
    SemanticVersion(1, 1, 1) >= SemanticVersion(1, 1, 1) shouldBe true
    SemanticVersion(1, 1, 1) >= SemanticVersion(0, 1, 1) shouldBe true
    SemanticVersion(1, 1, 1) >= SemanticVersion(1, 0, 1) shouldBe true
    SemanticVersion(1, 1, 1) >= SemanticVersion(1, 1, 0) shouldBe true
    SemanticVersion(1, 1, 1) >= SemanticVersion(2, 1, 1) shouldBe false
    SemanticVersion(1, 1, 1) >= SemanticVersion(1, 2, 1) shouldBe false
    SemanticVersion(1, 1, 1) >= SemanticVersion(1, 1, 2) shouldBe false
  }

  @Test
  def testLesserThenOrEqual(): Unit = {
    SemanticVersion(1, 1, 1) <= SemanticVersion(1, 1, 1) shouldBe true
    SemanticVersion(1, 1, 1) <= SemanticVersion(0, 1, 1) shouldBe false
    SemanticVersion(1, 1, 1) <= SemanticVersion(1, 0, 1) shouldBe false
    SemanticVersion(1, 1, 1) <= SemanticVersion(1, 1, 0) shouldBe false
    SemanticVersion(1, 1, 1) <= SemanticVersion(2, 1, 1) shouldBe true
    SemanticVersion(1, 1, 1) <= SemanticVersion(1, 2, 1) shouldBe true
    SemanticVersion(1, 1, 1) <= SemanticVersion(1, 1, 2) shouldBe true
  }

}
