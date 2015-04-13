package nl.lumc.sasc.biopet.tools

import java.nio.file.Paths
import java.util
import scala.collection.JavaConversions._

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.util.Random

/**
 * Created by ahbbollen on 13-4-15.
 */
class VcfToTsvTest extends TestNGSuite with MockitoSugar with Matchers {
  import VcfToTsv._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val rand = new Random()

  val vepped = resourcePath("/VEP_oneline.vcf")
  val unvepped = resourcePath("/unvepped.vcf")

  @Test def testAllFields() = {
    val tmp_path = "/tmp/VcfToTsv_" + rand.nextString(10) + ".tsv"
    val arguments = Array("-I", unvepped, "-o", tmp_path, "--all_info")
    main(arguments)
  }

  @Test def testSpecificField() = {
    val tmp_path = "/tmp/VcfToTsv_" + rand.nextString(10) + ".tsv"
    val arguments = Array("-I", vepped, "-o", tmp_path, "-i", "CSQ")
    main(arguments)
  }

  @Test def testNewSeparators() = {
    val tmp_path = "/tmp/VcfToTsv_" + rand.nextString(10) + ".tsv"
    val arguments = Array("-I", vepped, "-o", tmp_path, "--all_info", "--separator", ",", "--list_separator", "|")
    main(arguments)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def testIdenticalSeparators() = {
    val tmp_path = "/tmp/VcfToTsv_" + rand.nextString(10) + ".tsv"
    val arguments = Array("-I", vepped, "-o", tmp_path, "--all_info", "--separator", ",")
    main(arguments)
  }

  @Test def testFormatter() = {
    val formatter = createFormatter(2)
    formatter.format(5000.12345) should be("5000.12")
    val nformatter = createFormatter(3)
    nformatter.format(5000.12345) should be("5000.123")
  }

  @Test def testSortFields() = {
    val unsortedFields = Set("Child01-GT", "Mother02-GT", "Father03-GT", "INFO-Something", "INFO-ScoreSomething",
      "INFO-AlleleScoreSomething", "WeirdField")
    val samples = List("Child01", "Father03", "Mother02")

    val sorted = sortFields(unsortedFields, samples)
    sorted should be(List("WeirdField", "INFO-AlleleScoreSomething", "INFO-ScoreSomething", "INFO-Something",
    "Child01-GT", "Father03-GT", "Mother02-GT"))
  }

}
