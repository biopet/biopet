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
      ValidateVcf.main(Array("-i", resourcePath("/chrQ2.vcf"), "-R", resourcePath("/fake_chrQ.fa")))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      ValidateVcf.main(Array("-i", resourcePath("/chrQ_wrong_contig.vcf"), "-R", resourcePath("/fake_chrQ.fa")))
    }
    noException shouldBe thrownBy {
      ValidateVcf.main(Array("-i", resourcePath("/chrQ_wrong_contig.vcf"), "-R", resourcePath("/fake_chrQ.fa"), "--disableFail"))
    }
  }

}
