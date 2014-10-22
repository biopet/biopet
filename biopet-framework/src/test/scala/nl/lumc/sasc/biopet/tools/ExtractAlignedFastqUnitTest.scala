/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.tools

import htsjdk.tribble._
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class ExtractAlignedFastqUnitTest extends TestNGSuite with Matchers {

  import ExtractAlignedFastq._

  @Test def testIntervalStartEnd() = {
    val obs = makeFeatureFromString(List("chr5:1000-1100")).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStartEndComma() = {
    val obs = makeFeatureFromString(List("chr5:1,000-1,100")).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStartEndDot() = {
    val obs = makeFeatureFromString(List("chr5:1.000-1.100")).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalStart() = {
    val obs = makeFeatureFromString(List("chr5:1000")).next()
    val exp = new BasicFeature("chr5", 1000, 1000)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

  @Test def testIntervalError() = {
    intercept[IllegalArgumentException] {
      makeFeatureFromString(List("chr5:1000-")).next()
    }
  }
}

