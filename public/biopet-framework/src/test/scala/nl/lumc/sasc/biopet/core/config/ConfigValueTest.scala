package nl.lumc.sasc.biopet.core.config

import java.io.File

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 1/8/15.
 */
class ConfigValueTest extends TestNGSuite with Matchers {
  val index = ConfigValueIndex("", Nil, "")
  @Test def testAs: Unit = {
    ConfigValue(index, index, "bla").asString shouldBe "bla"
    ConfigValue(index, index, 1).asInt shouldBe 1
    ConfigValue(index, index, 1.0).asDouble shouldBe 1.0
    ConfigValue(index, index, List("bla")).asList shouldBe List("bla")
    ConfigValue(index, index, true).asBoolean shouldBe true
    ConfigValue(index, index, Map("1" -> 1)).asMap shouldBe Map("1" -> 1)
    ConfigValue(index, index, List("bla")).asStringList shouldBe List("bla")
    ConfigValue(index, index, List("bla")).asFileList shouldBe List(new File("bla"))
  }

  @Test def testToString: Unit = {
    ConfigValue(index, index, "bla", true).toString.getClass.getSimpleName shouldBe "String"
  }
}
