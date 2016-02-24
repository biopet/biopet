package nl.lumc.sasc.biopet.core.report

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvanthof on 24/02/16.
 */
class ReportSectionTest extends TestNGSuite with Matchers {

  @Test
  def testSectionRender: Unit = {
    ReportSection("/template.ssp", Map("arg" -> "test")).render() shouldBe "test"
    ReportSection("/template.ssp").render(Map("arg" -> "test")) shouldBe "test"
  }
}
