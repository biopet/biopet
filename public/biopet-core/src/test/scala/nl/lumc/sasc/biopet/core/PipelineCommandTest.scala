package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.utils.config.Config
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 17/11/15.
  */
class PipelineCommandTest extends TestNGSuite with Matchers {
  @Test
  def testPipelineCommand: Unit = {
    val pipeline = new PipelineCommand {
      override val globalConfig = new Config(Map())
      def getConfig = globalConfig
    }

    pipeline.pipelineName shouldBe this.getClass.getSimpleName.toLowerCase
    pipeline.pipeline shouldBe s"/${this.getClass.getName.stripSuffix("$").replaceAll("\\.", "/")}.class"

    // Config should be emty if the main method is not yet touched
    pipeline.getConfig.map shouldBe empty

    //TODO: Main method testing
  }
}
