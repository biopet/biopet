package nl.lumc.sasc.biopet.core.report

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 24/02/16.
  */
class ReportBuilderTest extends TestNGSuite with Matchers {

  @Test
  def testRenderTemplate: Unit = {
    ReportBuilder.templateCache = Map()
    ReportBuilder.templateCache shouldBe empty
    ReportBuilder.renderTemplate("/template.ssp", Map("arg" -> "test")) shouldBe "test"
    ReportBuilder.templateCache.size shouldBe 1
    ReportBuilder.renderTemplate("/template.ssp", Map("arg" -> "bla")) shouldBe "bla"
    ReportBuilder.templateCache.size shouldBe 1
  }
}
