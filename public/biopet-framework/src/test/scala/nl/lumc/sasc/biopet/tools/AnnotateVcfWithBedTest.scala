package nl.lumc.sasc.biopet.tools
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random
/**
 * Created by ahbbollen on 9-4-15.
 */
class AnnotateVcfWithBedTest extends TestNGSuite with MockitoSugar with Matchers {
  import AnnotateVcfWithBed._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val vepped_path = resourcePath("/VEP_oneline.vcf")
  val bed = resourcePath("/rrna01.bed")
  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp_path = "/tmp/VcfFilter_" + rand.nextString(10) + ".vcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-o", tmp_path, "-B", bed, "-f", "testing")
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp_path = "/tmp/VcfFilter_" + rand.nextString(10) + ".bcf"
    val arguments: Array[String] = Array("-I", vepped_path, "-o", tmp_path, "-B", bed, "-f", "testing")
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp_path = "/tmp/VcfFilter_" + rand.nextString(10) + ".vcf.gz"
    val arguments: Array[String] = Array("-I", vepped_path, "-o", tmp_path, "-B", bed, "-f", "testing")
    main(arguments)
  }

}
