package nl.lumc.sasc.biopet.utils.summary

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvanthof on 06/05/16.
 */
class SummaryValueTest extends TestNGSuite with Matchers {
  @Test
  def testConstructor: Unit = {
    val summary = new Summary(SummaryValueTest.testSummaryFile)
    new SummaryValue(None).value shouldBe None
    new SummaryValue(Some(1)).value shouldBe Some(1)
    new SummaryValue(List("key_1"), summary, None, None).value shouldBe Some("test_1")
    new SummaryValue(List("key_2"), summary, Some("sample1"), None).value shouldBe Some("test_2")
    new SummaryValue(List("key_3"), summary, Some("sample1"), Some("lib1")).value shouldBe Some("test_3")
  }

  @Test
  def testPlus: Unit = {
    new SummaryValue(Some(1.0)) + new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some(1.0)) + new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some(1)) + new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(2))
    new SummaryValue(Some("1")) + new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some("1")) + new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(None) + new SummaryValue(Some(1.0)) shouldBe new SummaryValue(None)
  }

  @Test
  def testMin: Unit = {
    new SummaryValue(Some(1.0)) - new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(0.0))
    new SummaryValue(Some(1.0)) - new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(0.0))
    new SummaryValue(Some(1)) - new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(0))
    new SummaryValue(Some("1")) - new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(0.0))
    new SummaryValue(Some("1")) - new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(0.0))
    new SummaryValue(None) - new SummaryValue(Some(1.0)) shouldBe new SummaryValue(None)
  }

  @Test
  def testMultiply: Unit = {
    new SummaryValue(Some(1.0)) * new SummaryValue(Some(2.0)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some(1.0)) * new SummaryValue(Some(2)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some(1)) * new SummaryValue(Some(2)) shouldBe new SummaryValue(Some(2))
    new SummaryValue(Some("1")) * new SummaryValue(Some(2)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some("1")) * new SummaryValue(Some(2.0)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(None) * new SummaryValue(Some(2.0)) shouldBe new SummaryValue(None)
  }

  @Test
  def testDivide: Unit = {
    new SummaryValue(Some(2.0)) / new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some(2.0)) / new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some(2)) / new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(2))
    new SummaryValue(Some("2")) / new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(Some("2")) / new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(2.0))
    new SummaryValue(None) / new SummaryValue(Some(1.0)) shouldBe new SummaryValue(None)
  }

  @Test
  def testLeft: Unit = {
    new SummaryValue(Some(2.0)) % new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(0))
    new SummaryValue(Some(2.0)) % new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(0))
    new SummaryValue(Some(2)) % new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(0))
    new SummaryValue(Some("2")) % new SummaryValue(Some(1)) shouldBe new SummaryValue(Some(0))
    new SummaryValue(Some("2")) % new SummaryValue(Some(1.0)) shouldBe new SummaryValue(Some(0))
    new SummaryValue(None) % new SummaryValue(Some(1.0)) shouldBe new SummaryValue(None)
  }

}

object SummaryValueTest {
  val testSummary = Map(
    "key_1" -> "test_1",
    "samples" -> Map(
      "sample1" -> Map(
        "key_2" -> "test_2",
        "libraries" -> Map(
          "lib1" -> Map("key_3" -> "test_3")
        )
      )
    )
  )

  val testSummaryFile = File.createTempFile("summary.", ".json")
  testSummaryFile.deleteOnExit()

  val writer = new PrintWriter(testSummaryFile)
  writer.println(ConfigUtils.mapToJson(testSummary).nospaces)
  writer.close()
}
