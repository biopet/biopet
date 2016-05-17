package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 17/05/16.
  */
class WriteConfigTest extends TestNGSuite with Matchers {
  @Test
  def testWriteConfig: Unit = {
    val writeConfig = new WriteConfig
    writeConfig.config = Map("test" -> "bla")
    writeConfig.out = File.createTempFile("config.", ".json")
    writeConfig.out.deleteOnExit()
    writeConfig.run

    ConfigUtils.fileToConfigMap(writeConfig.out) shouldBe Map("test" -> "bla")
  }
}
