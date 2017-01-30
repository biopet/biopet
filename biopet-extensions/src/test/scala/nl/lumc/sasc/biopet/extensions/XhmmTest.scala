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
package nl.lumc.sasc.biopet.extensions

import java.io.File
import java.nio.file.Paths

import nl.lumc.sasc.biopet.extensions.xhmm.XhmmDiscover
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmTest extends TestNGSuite with Matchers {

  @Test
  def testXcnvSummaryTest() = {
    val file = new File(Paths.get(getClass.getResource("/test.xcnv").toURI).toString)
    val cnv = new XhmmDiscover(null)
    cnv.outputXcnv = file

    val stats = cnv.summaryStats

    stats.keys.toList.sorted shouldBe List("Sample_01", "Sample_02", "Sample_03").sorted

    stats.getOrElse("Sample_01", Map()) shouldBe Map("DEL" -> 44, "DUP" -> 11)
    stats.getOrElse("Sample_02", Map()) shouldBe Map("DEL" -> 48, "DUP" -> 7)
    stats.getOrElse("Sample_03", Map()) shouldBe Map("DEL" -> 25, "DUP" -> 17)

  }

}
