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
class MergeAllelesTest extends TestNGSuite with MockitoSugar with Matchers {
  import MergeAlleles._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vepped_path = resourcePath("/chrQ.vcf.gz")
  val reference = resourcePath("/fake_chrQ.fa")

  // These two have to created
  // resourcepath copies files to another directory
  // hence we need to supply all needed files
  val dict = resourcePath("/fake_chrQ.dict")
  val fai = resourcePath("/fake_chrQ.fa.fai")

  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp_path = "/tmp/MergeAlleles_" + rand.nextString(10) + ".vcf"
    val arguments = Array("-I", vepped_path, "-o", tmp_path, "-R", reference)
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp_path = "/tmp/MergeAlleles_" + rand.nextString(10) + ".vcf.gz"
    val arguments = Array("-I", vepped_path, "-o", tmp_path, "-R", reference)
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp_path = "/tmp/MergeAlleles_" + rand.nextString(10) + ".bcf"
    val arguments = Array("-I", vepped_path, "-o", tmp_path, "-R", reference)
    main(arguments)
  }
}
