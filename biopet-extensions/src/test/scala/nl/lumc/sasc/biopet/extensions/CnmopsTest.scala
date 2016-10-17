package nl.lumc.sasc.biopet.extensions

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by Sander Bollen on 17-10-16.
 */
class CnmopsTest extends TestNGSuite with Matchers {

  @Test
  def testVersionCommand() = {
    val cn = new Cnmops(null)
    cn.versionCommand.endsWith("--version") shouldBe true
    cn.versionCommand.startsWith("Rscript") shouldBe true
  }

}
