package nl.lumc.sasc.biopet.extensions

import java.io.File
import java.nio.file.Paths

import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
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
    val cov = new BedtoolsCoverage(null)
    val tmp = File.createTempFile("test", ".bed")
    tmp.deleteOnExit()
    cov.output = tmp
    val genome = cov.createGenomeFile(file)

    Source.fromFile(genome).getLines().mkString("\n") shouldBe "chr1\t9"
  }

}
