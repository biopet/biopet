package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import nl.lumc.sasc.biopet.tools.SamplesTsvToJson._
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import nl.lumc.sasc.biopet.utils.summary.Summary

/**
 * Created by ahbbollen on 31-8-15.
 */
class SummaryToTsvTest extends TestNGSuite with MockitoSugar with Matchers {
  import SummaryToTsv._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testMain = {
    val tsv = resourcePath("/test.summary.json")
    val output = File.createTempFile("main", "tsv")

    noException should be thrownBy main(Array("-s", tsv, "-p", "something=flexiprep:settings:skip_trim",
      "-m", "root", "-o", output.toString))
    noException should be thrownBy main(Array("-s", tsv, "-p", "something=flexiprep:settings:skip_trim",
      "-m", "sample", "-o", output.toString))
    noException should be thrownBy main(Array("-s", tsv, "-p", "something=flexiprep:settings:skip_trim",
      "-m", "lib", "-o", output.toString))
  }

  @Test
  def testHeader = {
    val tsv = resourcePath("/test.summary.json")
    val path = List("something=flexiprep:settings:skip_trim")

    val paths = path.map(x => {
      val split = x.split("=", 2)
      split(0) -> split(1).split(":")
    }).toMap

    createHeader(paths) should equal("\tsomething")
  }

  @Test
  def testLine = {
    val tsv = resourcePath("/test.summary.json")
    val path = List("something=flexiprep:settings:skip_trim")

    val paths = path.map(x => {
      val split = x.split("=", 2)
      split(0) -> split(1).split(":")
    }).toMap

    val summary = new Summary(new File(tsv))
    val values = fetchValues(summary, paths)

    val line = values.head._2.keys.map(x => createLine(paths, values, x)).head
    line should equal("value\t")
    val sample_values = fetchValues(summary, paths, true, false)
    val sample_line = sample_values.head._2.keys.map(x => createLine(paths, sample_values, x)).head
    sample_line should equal("016\t")

    val lib_values = fetchValues(summary, paths, false, true)
    val lib_line = lib_values.head._2.keys.map(x => createLine(paths, lib_values, x)).head
    lib_line should equal("016-L001\tfalse")
  }

}
