package nl.lumc.sasc.biopet.tools

import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random

/**
 * Created by ahbbollen on 10-4-15.
 */
class CheckAllelesVcfInBamTest extends TestNGSuite with MockitoSugar with Matchers {
  import CheckAllelesVcfInBam._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vcf = resourcePath("/chrQ.vcf")
  val bam = resourcePath("/single01.bam")
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp_path = "/tmp/CheckAllesVcfInBam_" + rand.nextString(10) + ".vcf"
    val arguments = Array("-I", vcf, "-b", bam, "-s", "sample01", "-o", tmp_path)
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp_path = "/tmp/CheckAllesVcfInBam_" + rand.nextString(10) + ".vcf.gz"
    val arguments = Array("-I", vcf, "-b", bam, "-s", "sample01", "-o", tmp_path)
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp_path = "/tmp/CheckAllesVcfInBam_" + rand.nextString(10) + ".bcf"
    val arguments = Array("-I", vcf, "-b", bam, "-s", "sample01", "-o", tmp_path)
    main(arguments)
  }

}
