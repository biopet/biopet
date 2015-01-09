package nl.lumc.sasc.biopet.core.config

import nl.lumc.sasc.biopet.utils.ConfigUtils._
import nl.lumc.sasc.biopet.utils.{ ConfigUtilsTest, ConfigUtils }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 1/8/15.
 */
class ConfigTest extends TestNGSuite with MockitoSugar with Matchers with ConfigUtils.ImplicitConversions {
  @Test def testLoadConfigFile: Unit = {
    val config = new Config
    config.loadConfigFile(ConfigTest.file)
    config.map shouldBe ConfigTest.map
    config.loadConfigFile(ConfigTest.file)
    config.map shouldBe ConfigTest.map
  }

  @Test def testContains: Unit = {
    ConfigTest.config.contains("m1") shouldBe true
    ConfigTest.config.contains("notexist") shouldBe false
    ConfigTest.config.contains(new ConfigValueIndex("m1", Nil, "k1")) shouldBe true
    ConfigTest.config.contains(new ConfigValueIndex("notexist", Nil, "k1")) shouldBe true
    ConfigTest.config.contains(new ConfigValueIndex("notexist", Nil, "k1", false)) shouldBe false
  }

  @Test def testApply: Unit = {
    ConfigTest.config("m1", Nil, "k1").asString shouldBe "v2"
    ConfigTest.config("m1", Nil, "notexist", default = "default").asString shouldBe "default"
    intercept[IllegalStateException] {
      ConfigTest.config("m1", Nil, "notexist")
    }
  }

  @Test def testMergeConfigs: Unit = {
    val map1 = Map("1" -> 1)
    val map2 = Map("2" -> 2)
    Config.mergeConfigs(new Config(map1), new Config(map2)).map shouldBe ConfigUtils.mergeMaps(map1, map2)
  }

  @Test def testToString: Unit = {
    val map1 = Map("1" -> 1)
    new Config(map1).toString() shouldBe map1.toString()
  }

  @Test def testSkipNested: Unit = {
    val map = Map("1" -> Map("2" -> Map("4" -> Map("5" -> Map("k1" -> "v1")))))
    Config.getValueFromMap(map, new ConfigValueIndex("5", List("1", "2", "4", "5"), "k1")).get.asString shouldBe "v1"
    Config.getValueFromMap(map, new ConfigValueIndex("5", List("1", "2", "3", "4", "5"), "k1")).get.asString shouldBe "v1"
    Config.getValueFromMap(map, new ConfigValueIndex("5", List("1", "2", "3", "dummy", "dummy", "4", "5"), "k1")).get.asString shouldBe "v1"
  }

  @Test def testGetValueFromMap: Unit = {
    val map = ConfigTest.map
    Config.getValueFromMap(map, new ConfigValueIndex("m1", Nil, "k1")).get.asString shouldBe "v2"
    Config.getValueFromMap(map, new ConfigValueIndex("m1", List("bla"), "k1")).get.asString shouldBe "v2"
    Config.getValueFromMap(map, new ConfigValueIndex("m1", List("bla", "bla2"), "k1")).get.asString shouldBe "v2"
    Config.getValueFromMap(map, new ConfigValueIndex("m2", List("m1"), "k1")).get.asString shouldBe "v4"
    Config.getValueFromMap(map, new ConfigValueIndex("m2", Nil, "k1")).get.asString shouldBe "v3"
    Config.getValueFromMap(map, new ConfigValueIndex("notexist", Nil, "k1")).get.asString shouldBe "v1"

    Config.getValueFromMap(map, new ConfigValueIndex("notexist", Nil, "notexist")) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m1", Nil, "notexist")) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m2", Nil, "notexist")) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m1", List("m2"), "notexist")) shouldBe None

    // With free var
    Config.getValueFromMap(map, new ConfigValueIndex("m3", Nil, "k2")).get.asString shouldBe "v5"
    Config.getValueFromMap(map, new ConfigValueIndex("m4", Nil, "k2")) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m5", Nil, "k2")) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m6", Nil, "k2")) shouldBe None

    Config.getValueFromMap(map, new ConfigValueIndex("m4", List("m3"), "k2")).get.asString shouldBe "v6"
    Config.getValueFromMap(map, new ConfigValueIndex("m5", List("m3"), "k2")).get.asString shouldBe "v5"
    Config.getValueFromMap(map, new ConfigValueIndex("m6", List("m3"), "k2")).get.asString shouldBe "v5"

    Config.getValueFromMap(map, new ConfigValueIndex("m5", List("m3", "m4"), "k2")).get.asString shouldBe "v7"
    Config.getValueFromMap(map, new ConfigValueIndex("m6", List("m3", "m4"), "k2")).get.asString shouldBe "v6"

    Config.getValueFromMap(map, new ConfigValueIndex("m6", List("m3", "m4", "m5"), "k2")).get.asString shouldBe "v8"

    // Without free var
    Config.getValueFromMap(map, new ConfigValueIndex("m3", Nil, "k2", false)).get.asString shouldBe "v5"
    Config.getValueFromMap(map, new ConfigValueIndex("m4", Nil, "k2", false)) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m5", Nil, "k2", false)) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m6", Nil, "k2", false)) shouldBe None

    Config.getValueFromMap(map, new ConfigValueIndex("m4", List("m3"), "k2", false)).get.asString shouldBe "v6"
    Config.getValueFromMap(map, new ConfigValueIndex("m5", List("m3"), "k2", false)) shouldBe None
    Config.getValueFromMap(map, new ConfigValueIndex("m6", List("m3"), "k2", false)) shouldBe None

    Config.getValueFromMap(map, new ConfigValueIndex("m5", List("m3", "m4"), "k2", false)).get.asString shouldBe "v7"
    Config.getValueFromMap(map, new ConfigValueIndex("m6", List("m3", "m4"), "k2", false)) shouldBe None

    Config.getValueFromMap(map, new ConfigValueIndex("m6", List("m3", "m4", "m5"), "k2", false)).get.asString shouldBe "v8"
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

  val file = ConfigUtilsTest.writeTemp(ConfigUtils.mapToJson(map).spaces2)

  val config = new Config
  config.loadConfigFile(file)
}