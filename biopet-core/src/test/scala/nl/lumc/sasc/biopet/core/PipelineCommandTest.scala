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
package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.utils.config.Config
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.language.reflectiveCalls

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
