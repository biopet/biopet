/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.extensions

import scala.sys.process.{ Process, ProcessLogger }

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.SkipException
import org.testng.annotations.{ BeforeClass, Test }

class HtseqCountTest extends TestNGSuite with Matchers {

  @BeforeClass def checkExecutable() = {
    val wrapper = new HtseqCount(null)
    val proc = Process(wrapper.versionCommand)
    val exitCode =
      try {
        proc.run(ProcessLogger(lines => (), lines => ())).exitValue()
      } catch {
        case e: java.io.IOException => -1
        // rethrow if it's not IOException (we only expect IOException if the executable is missing)
        case e: Exception           => throw e
      }
    if (exitCode != 0)
      throw new SkipException("Skipping htseq-count test because the executable can not be found")
  }

  @Test(description = "htseq-count version number capture from executable")
  def testVersion() = new HtseqCount(null).getVersion should not be "N/A"
}
