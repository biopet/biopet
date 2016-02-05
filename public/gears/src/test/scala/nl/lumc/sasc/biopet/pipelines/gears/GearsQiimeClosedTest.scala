package nl.lumc.sasc.biopet.pipelines.gears

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvan_thof on 2/5/16.
  */
class GearsQiimeClosedTest extends  TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testQiimeBiomToKrona: Unit = {
    val qiimeBiomFile = new File(resourcePath("/otu_table.biom"))
    val outputFile = File.createTempFile("krona.", ".xml")
    outputFile.deleteOnExit()
    GearsQiimeClosed.qiimeBiomToKrona(qiimeBiomFile, outputFile)
  }
}
