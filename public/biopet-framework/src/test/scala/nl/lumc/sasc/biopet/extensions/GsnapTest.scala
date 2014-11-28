/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.extensions

import java.io.File
import org.testng.{ Assert, SkipException }
import org.testng.annotations.{ BeforeClass, Test }
import scala.sys.process.{ Process, ProcessLogger }

class GsnapUnitTest {

  @BeforeClass
  def checkExecutable() {
    val wrapper = new Gsnap(null)
    val proc = Process(wrapper.versionCommand)
    var exitCode = 0
    try {
      val run = proc.run(ProcessLogger(lines => (), lines => ()))
      exitCode = run.exitValue
    } catch {
      case e: java.io.IOException => exitCode = -1;
      // rethrow if it's not IOException (we only expect IOException if the executable is missing)
      case e: Exception           => throw e
    }
    if (exitCode != 0)
      throw new SkipException("Skipping GSNAP test because the executable can not be found")
  }

  @Test(description = "Version number capture from executable")
  def testVersion() {
    var wrapper = new Gsnap(null)
    Assert.assertNotEquals("N/A", wrapper.getVersion)
  }
}
