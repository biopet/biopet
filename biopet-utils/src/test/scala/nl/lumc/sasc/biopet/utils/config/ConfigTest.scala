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

import nl.lumc.sasc.biopet.utils.{ConfigUtils, ConfigUtilsTest}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{DataProvider, Test}

/**
  * Test class for [[Config]]
  *
  * Created by pjvan_thof on 1/8/15.
  */
class ConfigTest extends TestNGSuite with Matchers with ConfigUtils.ImplicitConversions {
  @Test def testLoadConfigFile(): Unit = {
    val config = new Config
    config.loadConfigFile(ConfigTest.file)
    config.map shouldBe ConfigTest.map
    config.loadConfigFile(ConfigTest.file)
    config.map shouldBe ConfigTest.map
  }

  @Test def testContains(): Unit = {
    ConfigTest.config.contains("m1") shouldBe true
    ConfigTest.config.contains("notexist") shouldBe false
    ConfigTest.config.contains(new ConfigValueIndex("m1", Nil, "k1")) shouldBe true
    ConfigTest.config.contains(new ConfigValueIndex("notexist", Nil, "k1")) shouldBe true
    ConfigTest.config.contains(new ConfigValueIndex("notexist", Nil, "k1", false)) shouldBe false
  }

  @Test def testApply(): Unit = {
    ConfigTest.config("m1", Nil, "k1").asString shouldBe "v2"
    ConfigTest.config("m1", Nil, "notexist", default = "default").asString shouldBe "default"
  }

  @Test def testMergeConfigs(): Unit = {
    val map1 = Map("1" -> 1)
    val map2 = Map("2" -> 2)
    Config.mergeConfigs(new Config(map1), new Config(map2)).map shouldBe ConfigUtils.mergeMaps(
      map1,
      map2)
  }

  @Test def testToString(): Unit = {
    val map1 = Map("1" -> 1)
    new Config(map1).toString() shouldBe map1.toString()
  }

  @Test def testSkipNested(): Unit = {
    val map = Map("1" -> Map("2" -> Map("4" -> Map("5" -> Map("k1" -> "v1")))))
    Config
      .getValueFromMap(map, new ConfigValueIndex("5", List("1", "2", "4", "5"), "k1"))
      .get
      .asString shouldBe "v1"
    Config
      .getValueFromMap(map, new ConfigValueIndex("5", List("1", "2", "3", "4", "5"), "k1"))
      .get
      .asString shouldBe "v1"
    Config
      .getValueFromMap(
        map,
        new ConfigValueIndex("5", List("1", "2", "3", "dummy", "dummy", "4", "5"), "k1"))
      .get
      .asString shouldBe "v1"
  }

  @DataProvider(name = "testGetValueFromMapProvider")
  def testGetValueFromMapProvider() = {
    Array(
      Array("m1", Nil, "k1", true, "v2"),
      Array("m1", List("bla"), "k1", true, "v2"),
      Array("m1", List("bla", "bla2"), "k1", true, "v2"),
      Array("m2", List("m1"), "k1", true, "v4"),
      Array("m2", Nil, "k1", true, "v3"),
      Array("notexist", Nil, "k1", true, "v1"),
      // With Freevar
      Array("notexist", Nil, "notexist", true, None),
      Array("m1", Nil, "notexist", true, None),
      Array("m2", Nil, "notexist", true, None),
      Array("m1", List("m2"), "notexist", true, None),
      Array("m3", Nil, "k2", true, "v5"),
      Array("m4", Nil, "k2", true, None),
      Array("m5", Nil, "k2", true, None),
      Array("m6", Nil, "k2", true, None),
      Array("m4", List("m3"), "k2", true, "v6"),
      Array("m5", List("m3"), "k2", true, "v5"),
      Array("m6", List("m3"), "k2", true, "v5"),
      Array("m5", List("m3", "m4"), "k2", true, "v7"),
      Array("m6", List("m3", "m4"), "k2", true, "v6"),
      Array("m6", List("m3", "m4", "m5"), "k2", true, "v8"),
      // Without freeVar
      Array("m3", Nil, "k2", false, "v5"),
      Array("m4", Nil, "k2", false, None),
      Array("m5", Nil, "k2", false, None),
      Array("m6", Nil, "k2", false, None),
      Array("m4", List("m3"), "k2", false, "v6"),
      Array("m5", List("m3"), "k2", false, None),
      Array("m6", List("m3"), "k2", false, None),
      Array("m5", List("m3", "m4"), "k2", false, "v7"),
      Array("m6", List("m3", "m4"), "k2", false, None),
      Array("m6", List("m3", "m4", "m5"), "k2", false, "v8")
    )
  }

  @Test(dataProvider = "testGetValueFromMapProvider")
  def testGetValueFromMap(module: String,
                          path: List[String],
                          key: String,
                          freeVar: Boolean,
                          expected: Any): Unit = {
    val map = ConfigTest.map
    val index = new ConfigValueIndex(module, path, key, freeVar)
    val value = Config.getValueFromMap(map, index)
    value match {
      case Some(x) => x.value shouldBe expected
      case None => value shouldBe expected
    }
  }
}

object ConfigTest {
  val map = Map(
    "k1" -> "v1",
    "m1" -> Map(
      "k1" -> "v2",
      "m2" -> Map(
        "k1" -> "v4"
      )
    ),
    "m2" -> Map(
      "k1" -> "v3"
    ),
    "m3" -> Map(
      "k2" -> "v5",
      "m4" -> Map(
        "k2" -> "v6",
        "m5" -> Map(
          "k2" -> "v7",
          "m6" -> Map(
            "k2" -> "v8"
          )
        )
      )
    )
  )

  val file = ConfigUtilsTest.writeTemp(ConfigUtils.mapToJson(map).spaces2, "json")

  val config = new Config
  config.loadConfigFile(file)
}
