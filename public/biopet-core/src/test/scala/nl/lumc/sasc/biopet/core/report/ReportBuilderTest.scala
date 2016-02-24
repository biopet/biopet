package nl.lumc.sasc.biopet.core.report

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 24/02/16.
  */
class ReportBuilderTest extends TestNGSuite with Matchers {

  @Test
  def testCountPages: Unit = {
    ReportBuilder.countPages(ReportPage(Nil, Nil, Map())) shouldBe 1
    ReportBuilder.countPages(ReportPage(
      "p1" -> ReportPage(Nil, Nil, Map()) :: Nil,
      Nil, Map())) shouldBe 2
    ReportBuilder.countPages(ReportPage(
      "p1" -> ReportPage(Nil, Nil, Map()) :: "p2" -> ReportPage(Nil, Nil, Map()) :: Nil,
      Nil, Map())) shouldBe 3
    ReportBuilder.countPages(ReportPage(
      "p1" -> ReportPage("p1" -> ReportPage(Nil, Nil, Map()) :: Nil, Nil, Map()) :: Nil,
      Nil, Map())) shouldBe 3
    ReportBuilder.countPages(ReportPage(
      "p1" -> ReportPage(Nil, Nil, Map()) :: "p2" -> ReportPage("p1" -> ReportPage(Nil, Nil, Map()) :: Nil, Nil, Map()) :: Nil,
      Nil, Map())) shouldBe 4
  }

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
