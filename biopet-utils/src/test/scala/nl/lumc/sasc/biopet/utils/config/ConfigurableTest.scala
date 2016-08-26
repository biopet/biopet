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

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Test class for [[Configurable]]
 *
 * Created by pjvan_thof on 1/8/15.
 */
class ConfigurableTest extends TestNGSuite with Matchers {

  abstract class Cfg extends Configurable {
    def get(key: String,
            default: String = null,
            configNamespace: String = null,
            freeVar: Boolean = true,
            sample: String = null,
            library: String = null) = {
      config(key, default, configNamespace, freeVar = freeVar, sample = sample, library = library)
    }
  }

  class ClassA(val root: Configurable) extends Cfg

  class ClassB(val root: Configurable) extends Cfg {
    lazy val classA = new ClassA(this)
    // Why this needs to be lazy?
  }

  class ClassC(val root: Configurable) extends Cfg {
    def this() = this(null)
    lazy val classB = new ClassB(this)
    // Why this needs to be lazy?
  }

  @Test def testConfigurable(): Unit = {
    val classC = new ClassC {
      override def configNamespace = "classc"
      override val globalConfig = new Config(ConfigurableTest.map)
      override val fixedValues = Map("fixed" -> "fixed")
    }

    classC.configPath shouldBe Nil
    classC.configFullPath shouldBe List("classc")
    classC.classB.configPath shouldBe List("classc")
    classC.classB.configFullPath shouldBe List("classc", "classb")
    classC.classB.classA.configPath shouldBe List("classc", "classb")
    classC.classB.classA.configFullPath shouldBe List("classc", "classb", "classa")

    classC.get("k1").asString shouldBe "c1"
    classC.classB.get("k1").asString shouldBe "c1"
    classC.classB.classA.get("k1").asString shouldBe "c1"

    classC.get("notexist", default = "default").asString shouldBe "default"

    classC.get("k1", freeVar = false).asString shouldBe "c1"
    classC.classB.get("k1", freeVar = false).asString shouldBe "b1"
    classC.classB.classA.get("k1", freeVar = false).asString shouldBe "a1"

    classC.get("bla", sample = "sample1", library = "library1").asString shouldBe "bla"
    classC.get("test", sample = "sample1", library = "library1").asString shouldBe "test"
    classC.get("test", sample = "sample1").asString shouldBe "test"

    // Fixed values
    classC.get("fixed").asString shouldBe "fixed"
    classC.classB.get("fixed").asString shouldBe "fixed"
    classC.classB.classA.get("fixed").asString shouldBe "fixed"
  }
}

object ConfigurableTest {
  val map = Map(
    "fixed" -> "nonfixed",
    "classa" -> Map(
      "k1" -> "a1",
      "fixed" -> "nonfixed"
    ), "classb" -> Map(
      "k1" -> "b1",
      "fixed" -> "nonfixed"
    ), "classc" -> Map(
      "k1" -> "c1",
      "fixed" -> "nonfixed"
    ), "samples" -> Map(
      "sample1" -> Map(
        "fixed" -> "nonfixed",
        "test" -> "test",
        "libraries" -> Map(
          "library1" -> Map(
            "fixed" -> "nonfixed",
            "bla" -> "bla"
          )
        )
      )
    )
  )
}
