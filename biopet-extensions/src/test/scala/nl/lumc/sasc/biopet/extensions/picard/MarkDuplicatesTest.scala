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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Test class for [[MarkDuplicates]]
 *
 * Created by pjvan_thof on 2/19/15.
 */
class MarkDuplicatesTest extends TestNGSuite with Matchers {

  @Test
  def summaryData(): Unit = {
    val file = new File(Paths.get(getClass.getResource("picard.dedup.metrics").toURI).toString)
    val job = new MarkDuplicates(null)
    job.outputMetrics = file

    job.summaryStats
  }
}