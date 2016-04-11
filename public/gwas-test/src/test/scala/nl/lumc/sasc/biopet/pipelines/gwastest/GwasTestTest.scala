package nl.lumc.sasc.biopet.pipelines.gwastest

import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvan_thof on 4/11/16.
  */
class GwasTestTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): GwasTest = {
    new GwasTest {
      override def configName = "gwastest"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }



  @Test
  def testEmpty: Unit = {
    val pipeline = initPipeline(Map())
    intercept[IllegalArgumentException] {
      pipeline.script()
    }
  }
}
