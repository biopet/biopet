package nl.lumc.sasc.biopet.core.summary

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 14/01/16.
  */
class SummarizableTest extends TestNGSuite with Matchers {
  @Test
  def testDefaultMerge: Unit = {
    val summarizable = new Summarizable {
      def summaryFiles: Map[String, File] = ???
      def summaryStats: Any = ???
    }
    intercept[IllegalStateException] {
      summarizable.resolveSummaryConflict("1", "1", "key")
    }
  }

  def testOverrideMerge: Unit = {
    val summarizable = new Summarizable {
      def summaryFiles: Map[String, File] = ???
      def summaryStats: Any = ???
      override def resolveSummaryConflict(v1: Any, v2: Any, key: String) = v1
    }
    summarizable.resolveSummaryConflict("1", "1", "key") shouldBe "1"
  }
}
