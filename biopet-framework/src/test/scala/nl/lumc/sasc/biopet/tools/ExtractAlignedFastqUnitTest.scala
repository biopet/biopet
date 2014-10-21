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
    val ivs = List("chr5:1000-1100")
    val obs = makeFeatureFromString(ivs).next()
    val exp = new BasicFeature("chr5", 1000, 1100)
    obs.getChr should === (exp.getChr)
    obs.getStart should === (exp.getStart)
    obs.getEnd should === (exp.getEnd)
  }

}

