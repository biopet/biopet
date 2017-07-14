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
  def testWriteConfig(): Unit = {
    val writeConfig = new WriteConfig
    writeConfig.config = Map("test" -> "bla")
    writeConfig.out = File.createTempFile("config.", ".json")
    writeConfig.out.deleteOnExit()
    writeConfig.run()

    ConfigUtils.fileToConfigMap(writeConfig.out) shouldBe Map("test" -> "bla")
  }
}
