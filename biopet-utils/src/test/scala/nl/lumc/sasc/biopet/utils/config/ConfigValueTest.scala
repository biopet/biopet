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
package nl.lumc.sasc.biopet.utils.config

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Test class for [[ConfigValue]]
 *
 * Created by pjvan_thof on 1/8/15.
 */
class ConfigValueTest extends TestNGSuite with Matchers {
  val index = ConfigValueIndex("", Nil, "")
  @Test def testAs(): Unit = {
    ConfigValue(index, index, "bla").asString shouldBe "bla"
    ConfigValue(index, index, 1).asInt shouldBe 1
    ConfigValue(index, index, 1.0).asDouble shouldBe 1.0
    ConfigValue(index, index, List("bla")).asList shouldBe List("bla")
    ConfigValue(index, index, true).asBoolean shouldBe true
    ConfigValue(index, index, Map("1" -> 1)).asMap shouldBe Map("1" -> 1)
    ConfigValue(index, index, List("bla")).asStringList shouldBe List("bla")
    ConfigValue(index, index, List("bla")).asFileList shouldBe List(new File("bla"))
  }

  @Test def testToString(): Unit = {
    ConfigValue(index, index, "bla", default = true).toString.getClass.getSimpleName shouldBe "String"
  }
}
