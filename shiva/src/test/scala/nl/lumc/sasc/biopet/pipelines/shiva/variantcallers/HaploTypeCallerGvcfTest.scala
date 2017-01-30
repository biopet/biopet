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
package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by Sander Bollen on 13-7-16.
 */
class HaploTypeCallerGvcfTest extends TestNGSuite with Matchers {

  @Test
  def testGvcfFiles = {
    val samples = List("sample01", "sample02", "sample03")
    val hc = new HaplotypeCallerGvcf(null)
    hc.inputBams = createInputMap(samples)
    hc.biopetScript()

    hc.getGvcfs.size shouldBe 3
    hc.getGvcfs.keys.toList.sorted shouldEqual samples.sorted
  }

  def createInputMap(samples: List[String]): Map[String, File] = {
    samples.map({ x =>
      val file = File.createTempFile(x, ".bam")
      file.deleteOnExit()
      x -> file
    }).toMap
  }

}
