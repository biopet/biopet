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

import scala.util.Try

/**
 * Test class for utils package object
 *
 * Created by pjvan_thof on 4/14/15.
 */
class PackageTest extends TestNGSuite with Matchers {

  @Test def testConvert(): Unit = {
    tryToParseNumber("4") shouldBe Try(4)
    tryToParseNumber("13.37") shouldBe Try(13.37)
    tryToParseNumber("I'm not a number") should not be Try("I'm not a number")

    tryToParseNumber("4", fallBack = true) shouldBe Try(4)
    tryToParseNumber("13.37", fallBack = true) shouldBe Try(13.37)
    tryToParseNumber("I'm not a number", fallBack = true) shouldBe Try("I'm not a number")
  }
}
