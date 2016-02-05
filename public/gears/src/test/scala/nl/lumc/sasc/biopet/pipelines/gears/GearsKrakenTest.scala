package nl.lumc.sasc.biopet.pipelines.gears

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvan_thof on 2/5/16.
  */
class GearsKrakenTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testConvertKrakenJsonToKronaXml: Unit = {
    val krakenJsonFile = new File(resourcePath("/hpv_simu_R1.krkn.json"))
    val outputFile = File.createTempFile("krona.", ".xml")
    outputFile.deleteOnExit()
    GearsKraken.convertKrakenJsonToKronaXml(Map("test" -> krakenJsonFile), outputFile)
  }
}
