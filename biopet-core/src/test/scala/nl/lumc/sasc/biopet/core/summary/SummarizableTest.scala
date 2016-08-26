/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
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
