package nl.lumc.sasc.biopet.extensions

import java.io.File
import java.nio.file.Paths

import nl.lumc.sasc.biopet.extensions.xhmm.XhmmDiscover
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmTest extends TestNGSuite with Matchers {

  @Test
  def testXcnvSummaryTest() = {
    val file = new File(Paths.get(getClass.getResource("/test.xcnv").toURI).toString)
    val cnv = new XhmmDiscover(null)
    cnv.outputXcnv = file

    val stats = cnv.summaryStats

    stats.keys.toList.sorted shouldBe List("Sample_01", "Sample_02", "Sample_03").sorted

    stats.getOrElse("Sample_01", Map()) shouldBe Map("DEL" -> 44, "DUP" -> 11)
    stats.getOrElse("Sample_02", Map()) shouldBe Map("DEL" -> 48, "DUP" -> 7)
    stats.getOrElse("Sample_03", Map()) shouldBe Map("DEL" -> 25, "DUP" -> 17)

  }

}
