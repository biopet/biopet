package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
 * Created by pjvan_thof on 27-9-16.
 */
class FindOverlapMatchTest extends TestNGSuite with Matchers {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testOverlap: Unit = {
    val input = new File(resourcePath("/overlapmetrics.txt"))
    val output = File.createTempFile("overlap.", ".txt")
    val shouldBeOutput = new File(resourcePath("/overlapmetrics.default.output"))
    output.deleteOnExit()
    FindOverlapMatch.main(Array("-i", input.getAbsolutePath, "-c", "0.9", "-o", output.getAbsolutePath))
    Source.fromFile(output).getLines().toList shouldBe Source.fromFile(shouldBeOutput).getLines().toList
  }

  @Test
  def testOverlapSameName: Unit = {
    val input = new File(resourcePath("/overlapmetrics.txt"))
    val output = File.createTempFile("overlap.", ".txt")
    val shouldBeOutput = new File(resourcePath("/overlapmetrics.same_names.output"))
    output.deleteOnExit()
    FindOverlapMatch.main(Array("-i", input.getAbsolutePath, "-c", "0.9", "-o", output.getAbsolutePath, "--use_same_names"))
    Source.fromFile(output).getLines().toList shouldBe Source.fromFile(shouldBeOutput).getLines().toList
  }

}
