package nl.lumc.sasc.biopet.extensions

import java.io.File
import java.nio.file.Paths

import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.utils.config.{ Config, Configurable }
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
    class TestCov(override val root: Configurable) extends BedtoolsCoverage(root) {
      jobTempDir = tmp
      override def referenceFai = file

      def genome = BedtoolsCoverage.createGenomeFile(file, file.getParentFile)
    }
    val cov = new TestCov(null)
    val genome = cov.genome
    Source.fromFile(genome).getLines().mkString("\n") shouldBe "chr1\t9"
  }

}
