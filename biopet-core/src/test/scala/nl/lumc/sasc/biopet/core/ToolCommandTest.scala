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

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import nl.lumc.sasc.biopet.FullVersion

/**
  * Created by pjvanthof on 16/11/15.
  */
class ToolCommandTest extends TestNGSuite with Matchers {
  @Test
  def testToolCommand(): Unit = {
    val tool = new ToolCommandFunction {
      def parent = null
      def toolObject = ToolCommandTest
    }

    tool.versionCommand shouldBe empty
    tool.versionRegex shouldBe empty
    tool.getVersion shouldBe Some("Biopet " + FullVersion)
    tool.beforeGraph()

    tool.javaMainClass shouldBe ToolCommandTest.getClass.getName.takeWhile(_ != '$')
  }
}

object ToolCommandTest {}
