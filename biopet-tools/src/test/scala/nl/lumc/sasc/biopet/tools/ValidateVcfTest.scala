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
package nl.lumc.sasc.biopet.tools

import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvan_thof on 12-12-16.
  */
class ValidateVcfTest extends TestNGSuite with Matchers {

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  @Test
  def testMain(): Unit = {
    noException shouldBe thrownBy {
      ValidateVcf.main(
        Array("-i", resourcePath("/chrQ2.vcf"), "-R", resourcePath("/fake_chrQ.fa")))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      ValidateVcf.main(
        Array("-i", resourcePath("/chrQ_wrong_contig.vcf"), "-R", resourcePath("/fake_chrQ.fa")))
    }
    noException shouldBe thrownBy {
      ValidateVcf.main(
        Array("-i",
              resourcePath("/chrQ_wrong_contig.vcf"),
              "-R",
              resourcePath("/fake_chrQ.fa"),
              "--disableFail"))
    }
  }
}
