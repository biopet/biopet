package nl.lumc.sasc.biopet.core

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import nl.lumc.sasc.biopet.FullVersion

/**
 * Created by pjvanthof on 16/11/15.
 */
class ToolCommandTest extends TestNGSuite with Matchers {
  @Test
  def testToolCommand: Unit = {
    val tool = new ToolCommandFunction {
      def root = null
      def toolObject = ToolCommandTest
    }

    tool.versionCommand shouldBe empty
    tool.versionRegex.toString() shouldBe empty
    tool.getVersion shouldBe Some("Biopet " + FullVersion)
    tool.beforeGraph

    tool.javaMainClass shouldBe ToolCommandTest.getClass.getName.takeWhile(_ != '$')
  }
}

object ToolCommandTest {

}