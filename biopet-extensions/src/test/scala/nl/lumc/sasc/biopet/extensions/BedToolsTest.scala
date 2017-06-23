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

import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by Sander Bollen on 12-5-16.
  */
class BedToolsTest extends TestNGSuite with Matchers {

  @Test
  def testBedtoolsCoverageCreateGenomeFile() = {
    val file = new File(Paths.get(this.getClass.getResource("/ref.fa.fai").toURI).toString)
    val tmp = File.createTempFile("test", ".bed")
    tmp.deleteOnExit()
    class TestCov(override val parent: Configurable) extends BedtoolsCoverage(parent) {
      jobTempDir = tmp
      override def referenceFai = file

      def genome = BedtoolsCoverage.createGenomeFile(file, file.getParentFile)
    }
    val cov = new TestCov(null)
    val genome = cov.genome
    Source.fromFile(genome).getLines().mkString("\n") shouldBe "chr1\t9"
  }

}
