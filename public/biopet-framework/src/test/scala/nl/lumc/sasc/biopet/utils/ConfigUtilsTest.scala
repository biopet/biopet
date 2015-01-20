package nl.lumc.sasc.biopet.utils

import java.io.{ PrintWriter, File }

import argonaut.Argonaut._
import argonaut.Json
import nl.lumc.sasc.biopet.core.config.{ ConfigValueIndex, ConfigValue }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 1/5/15.
 */
class ConfigUtilsTest extends TestNGSuite with Matchers {
  import ConfigUtils._
  import ConfigUtilsTest._

  @Test def testGetValueFromPath: Unit = {
    getValueFromPath(map1, List("dummy")) shouldBe Some(Map("dummy" -> 1))
    getValueFromPath(map1, List("dummy", "dummy")) shouldBe Some(1)
    getValueFromPath(map1, List("nested3", "nested2", "nested1")) shouldBe Some(Map("dummy" -> 1))
    getValueFromPath(map1, List("notexist", "dummy")) shouldBe None
    getValueFromPath(map1, List("dummy", "notexist")) shouldBe None
  }

  @Test def testGetMapFromPath: Unit = {
    getMapFromPath(map1, List("dummy")) shouldBe Some(Map("dummy" -> 1))
    getMapFromPath(map1, List("nested3", "nested2", "nested1")) shouldBe Some(Map("dummy" -> 1))
    intercept[IllegalStateException] {
      getMapFromPath(map1, List("dummy", "dummy"))
    }
  }

  // Merge maps
  @Test def testMergeMaps: Unit = {
    val mergedMap = mergeMaps(map1, map2)
    getValueFromPath(mergedMap, List("nested", "1")) shouldBe Some(1)
    getValueFromPath(mergedMap, List("nested", "2")) shouldBe Some(1)
    getValueFromPath(mergedMap, List("nested", "3")) shouldBe Some(2)
  }

  // Json to scala values
  @Test def testFileToJson: Unit = {
    fileToJson(file1) shouldBe json1
    fileToJson(file2) shouldBe json2
    intercept[IllegalStateException] {
      fileToJson(corruptFile)
    }
  }

  @Test def testJsonToMap: Unit = {
    jsonToMap(json1) shouldBe map1
    jsonToMap(json2) shouldBe map2
    intercept[IllegalStateException] {
      jsonToMap(Json.jNumberOrString(1337))
    }
  }

  @Test def testFileToConfigMap: Unit = {
    fileToConfigMap(file1) shouldBe map1
    fileToConfigMap(file2) shouldBe map2
  }

  // Any/scala values to Json objects
  @Test def testAnyToJson: Unit = {
    anyToJson("bla") shouldBe jString("bla")
    anyToJson(1337) shouldBe Json.jNumberOrString(1337)
    anyToJson(13.37) shouldBe Json.jNumberOrString(13.37)
    anyToJson(1337L) shouldBe Json.jNumberOrString(1337L)
    anyToJson(13.37f) shouldBe Json.jNumberOrString(13.37f)
    anyToJson(List("bla")) shouldBe Json.array(anyToJson("bla"))
    anyToJson(anyToJson("bla")) shouldBe anyToJson("bla")
    anyToJson(Map()) shouldBe jEmptyObject
    anyToJson(Map("bla" -> 1337)) shouldBe ("bla" := 1337) ->: jEmptyObject
  }

  @Test def testMapToJson: Unit = {
    mapToJson(Map()) shouldBe jEmptyObject
    mapToJson(Map("bla" -> 1337)) shouldBe ("bla" := 1337) ->: jEmptyObject
    mapToJson(Map("bla" -> Map())) shouldBe ("bla" := jEmptyObject) ->: jEmptyObject
    mapToJson(Map("bla" -> Map("nested" -> 1337))) shouldBe ("bla" := (("nested" := 1337) ->: jEmptyObject)) ->: jEmptyObject
  }

  // Any to scala values
  @Test def testAny2string: Unit = {
    any2string("bla") shouldBe "bla"
    any2string(1337) shouldBe "1337"
    any2string(true) shouldBe "true"
    any2string(13.37) shouldBe "13.37"
  }

  @Test def testAny2int: Unit = {
    any2int(1337) shouldBe 1337
    any2int("1337") shouldBe 1337
    any2int(13.37) shouldBe 13
    intercept[IllegalStateException] {
      any2int(new Object)
    }
  }

  @Test def testAny2long: Unit = {
    any2long(1337L) shouldBe 1337L
    any2long(1337) shouldBe 1337L
    any2long("1337") shouldBe 1337L
    any2long(13.37) shouldBe 13L
    intercept[IllegalStateException] {
      any2long(new Object)
    }
  }

  @Test def testAny2double: Unit = {
    any2double(13.37) shouldBe 13.37d
    any2double("1337") shouldBe 1337d
    any2double(1337) shouldBe 1337d
    any2double(1337L) shouldBe 1337d
    any2double(1337f) shouldBe 1337d
    intercept[IllegalStateException] {
      any2double(new Object)
    }
  }

  @Test def testAny2float: Unit = {
    any2float(1337d) shouldBe 1337f
    any2float("1337") shouldBe 1337f
    any2float(1337) shouldBe 1337f
    any2float(1337L) shouldBe 1337f
    any2float(13.37f) shouldBe 13.37f
    intercept[IllegalStateException] {
      any2float(new Object)
    }
  }

  @Test def testAny2boolean: Unit = {
    any2boolean(true) shouldBe true
    any2boolean("false") shouldBe false
    any2boolean("true") shouldBe true
    any2boolean(0) shouldBe false
    any2boolean(1) shouldBe true
    intercept[IllegalStateException] {
      any2boolean(new Object)
    }
  }

  @Test def testAny2list: Unit = {
    any2list(Nil) shouldBe Nil
    any2list(List(1, 2, 3, 4)) shouldBe List(1, 2, 3, 4)
    any2list(List(1337)) shouldBe List(1337)
    any2list(1337) shouldBe List(1337)
  }

  @Test def testAny2stringList: Unit = {
    any2stringList(Nil) shouldBe Nil
    any2stringList(List("1337")) shouldBe List("1337")
    any2stringList(List(1337)) shouldBe List("1337")
    any2stringList(1337) shouldBe List("1337")
  }

  @Test def testAny2map: Unit = {
    any2map(Map()) shouldBe Map()
    any2map(Map("bla" -> 1337)) shouldBe Map("bla" -> 1337)
    any2map(null) shouldBe null
    intercept[IllegalStateException] {
      any2map(new Object)
    }
  }

  @Test def testImplicits: Unit = {
    val index = ConfigValueIndex("test", Nil, "test")
    new ImplicitConversions {
      configValue2list(ConfigValue(index, index, List(""))) shouldBe List("")
      intercept[IllegalStateException] {
        configValue2list(ConfigValue(index, index, null))
      }

      configValue2stringList(ConfigValue(index, index, List(""))) shouldBe List("")
      intercept[IllegalStateException] {
        configValue2stringList(ConfigValue(index, index, null))
      }

      configValue2stringSet(ConfigValue(index, index, List(""))) shouldBe Set("")
      intercept[IllegalStateException] {
        configValue2stringSet(ConfigValue(index, index, null))
      }

      var int: Int = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        int = ConfigValue(index, index, null)
      }

      var long: Long = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        long = ConfigValue(index, index, null)
      }

      var double: Double = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        double = ConfigValue(index, index, null)
      }

      var float: Float = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        float = ConfigValue(index, index, null)
      }

      var boolean: Boolean = ConfigValue(index, index, true)
      intercept[IllegalStateException] {
        boolean = ConfigValue(index, index, null)
      }

      var intOption: Option[Int] = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        int = ConfigValue(index, index, null)
      }

      var longOption: Option[Long] = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        long = ConfigValue(index, index, null)
      }

      var doubleOption: Option[Double] = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        double = ConfigValue(index, index, null)
      }

      var floatOption: Option[Float] = ConfigValue(index, index, 1)
      intercept[IllegalStateException] {
        float = ConfigValue(index, index, null)
      }

      var booleanOption: Option[Boolean] = ConfigValue(index, index, true)
      intercept[IllegalStateException] {
        boolean = ConfigValue(index, index, null)
      }

      var string: String = ConfigValue(index, index, "test")
      string = ConfigValue(index, index, null)

      var file: File = ConfigValue(index, index, "test")
      file = ConfigValue(index, index, null)
    }
  }
}
object ConfigUtilsTest {
  def writeTemp(text: String): File = {
    val file = File.createTempFile("TestConfigUtils.", ".json")
    val w = new PrintWriter(file)
    w.write(text)
    w.close()
    return file
  }

  val jsonText1 =
    """
       |{
       | "int": 1337,
       | "double": 13.37,
       | "string": "bla",
       | "nested": {
       |   "1": 1,
       |   "2": 1
       | },
       | "list": ["a", "b", "c"],
       | "dummy": { "dummy": 1},
       | "nested3": { "nested2": { "nested1": { "dummy": 1 } } }
       |}
     """.stripMargin

  val file1 = writeTemp(jsonText1)

  val json1 = {
    ("int" := 1337) ->:
      ("double" := 13.37) ->:
      ("string" := "bla") ->:
      ("nested" := (("1" := 1) ->: ("2" := 1) ->: jEmptyObject)) ->:
      ("list" := List("a", "b", "c")) ->:
      ("dummy" := ("dummy" := 1) ->: jEmptyObject) ->:
      ("nested3" := ("nested2" := ("nested1" := ("dummy" := 1) ->: jEmptyObject) ->: jEmptyObject) ->: jEmptyObject) ->:
      jEmptyObject
  }

  val map1 = Map("int" -> 1337,
    "double" -> 13.37,
    "string" -> "bla",
    "nested" -> Map("1" -> 1, "2" -> 1),
    "list" -> List("a", "b", "c"),
    "dummy" -> Map("dummy" -> 1),
    "nested3" -> Map("nested2" -> Map("nested1" -> Map("dummy" -> 1))))

  val jsonText2 =
    """
       |{
       | "int": 7331,
       | "nested": {
       |   "2": 2,
       |   "3": 2
       | },
       | "dummy": 1
       |}
     """.stripMargin

  val file2 = writeTemp(jsonText2)

  val json2 = {
    ("int" := 7331) ->:
      ("nested" := (("2" := 2) ->: ("3" := 2) ->: jEmptyObject)) ->:
      ("dummy" := 1) ->:
      jEmptyObject
  }

  val map2: Map[String, Any] = Map("int" -> 7331,
    "nested" -> Map("2" -> 2, "3" -> 2),
    "dummy" -> 1)

  val corruptJson =
    """
       |{
       | "int": 1337
       | "double": 13.37
       |}
     """.stripMargin

  val corruptFile = writeTemp(corruptJson)
}