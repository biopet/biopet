package nl.lumc.sasc.biopet.core.config

import nl.lumc.sasc.biopet.utils.{ ConfigUtils, ConfigUtilsTest }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 1/8/15.
 */
class ConfigurableTest extends TestNGSuite with MockitoSugar with Matchers {
  @Test def testConfigurable: Unit = {
    Config.global.map = Map()
    Config.global.loadConfigFile(ConfigurableTest.file)

    val classC = new ClassC

    classC.configPath shouldBe Nil
    classC.configFullPath shouldBe List("classc")
    classC.classB.configPath shouldBe List("classc")
    classC.classB.configFullPath shouldBe List("classc", "classb")
    classC.classB.classA.configPath shouldBe List("classc", "classb")
    classC.classB.classA.configFullPath shouldBe List("classc", "classb", "classa")

    classC.get("k1").asString shouldBe "c1"
    classC.classB.get("k1").asString shouldBe "c1"
    classC.classB.classA.get("k1").asString shouldBe "c1"

    classC.get("notexist") shouldBe null
    intercept[IllegalStateException] {
      classC.get("notexist", required = true)
    }
    classC.get("notexist", default = "default").asString shouldBe "default"

    classC.get("k1", freeVar = false).asString shouldBe "c1"
    classC.classB.get("k1", freeVar = false).asString shouldBe "b1"
    classC.classB.classA.get("k1", freeVar = false).asString shouldBe "a1"

    classC.get("bla", sample = "sample1", library = "library1").asString shouldBe "bla"
    classC.get("test", sample = "sample1", library = "library1").asString shouldBe "test"
    classC.get("test", sample = "sample1").asString shouldBe "test"
  }
}

abstract class Cfg extends Configurable {
  def get(key: String,
          default: String = null,
          submodule: String = null,
          required: Boolean = false,
          freeVar: Boolean = true,
          sample: String = null,
          library: String = null) = {
    config(key, default, submodule, required, freeVar = freeVar, sample = sample, library = library)
  }
}

class ClassA(val root: Configurable) extends Cfg

class ClassB(val root: Configurable) extends Cfg {
  val classA = new ClassA(this)
}

class ClassC(val root: Configurable) extends Cfg {
  def this() = this(null)
  val classB = new ClassB(this)
}

object ConfigurableTest {
  val map = Map(
    "classa" -> Map(
      "k1" -> "a1"
    ), "classb" -> Map(
      "k1" -> "b1"
    ), "classc" -> Map(
      "k1" -> "c1"
    ), "samples" -> Map(
      "sample1" -> Map(
        "test" -> "test",
        "libraries" -> Map(
          "library1" -> Map(
            "bla" -> "bla"
          )
        )
      )
    )
  )

  val file = ConfigUtilsTest.writeTemp(ConfigUtils.mapToJson(map).spaces2)
}
