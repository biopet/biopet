package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source


/**
  * Created by pjvanthof on 09/05/16.
  */
class BamToChromSizesTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testChromSizes: Unit = {
    val bamFile = new File(resourcePath("/empty.bam"))
    val bamToChromSizes = new BamToChromSizes(null)
    bamToChromSizes.bamFile = bamFile
    bamToChromSizes.chromSizesFile = File.createTempFile("chrom.", ".sizes")
    bamToChromSizes.chromSizesFile.deleteOnExit()
    bamToChromSizes.run()
    Source.fromFile(bamToChromSizes.chromSizesFile).getLines().toList shouldBe List("chrQ\t10000", "chrR\t10000")
  }
}
