package nl.lumc.sasc.biopet.utils

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Try

/**
 * Created by pjvan_thof on 4/14/15.
 */
class PackageTest extends TestNGSuite with Matchers {

  @Test def testConvert: Unit = {
    tryToParseNumber("4") shouldBe Try(4)
    tryToParseNumber("13.37") shouldBe Try(13.37)
    tryToParseNumber("I'm not a number") should not be Try("I'm not a number")

    tryToParseNumber("4", true) shouldBe Try(4)
    tryToParseNumber("13.37", true) shouldBe Try(13.37)
    tryToParseNumber("I'm not a number", true) shouldBe Try("I'm not a number")
  }
}
