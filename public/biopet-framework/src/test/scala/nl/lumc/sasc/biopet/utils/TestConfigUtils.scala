package nl.lumc.sasc.biopet.utils

import java.io.File

import nl.lumc.sasc.biopet.core.config.{ ConfigValueIndex, ConfigValue }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 1/5/15.
 */
class TestConfigUtils extends TestNGSuite with MockitoSugar with Matchers {
  import ConfigUtils._

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
      var map: Map[String, Any] = ConfigValue(index, index, Map())
      map = ConfigValue(index, index, null)

      configValue2list(ConfigValue(index, index, List(""))) shouldBe List("")
      configValue2list(ConfigValue(index, index, null)) shouldBe null

      configValue2stringList(ConfigValue(index, index, List(""))) shouldBe List("")
      configValue2stringList(ConfigValue(index, index, null)) shouldBe null

      configValue2stringSet(ConfigValue(index, index, List(""))) shouldBe Set("")
      configValue2stringSet(ConfigValue(index, index, null)) shouldBe null

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
