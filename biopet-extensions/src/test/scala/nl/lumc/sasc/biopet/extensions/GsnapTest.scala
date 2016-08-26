/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.extensions.gmap.Gsnap
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.SkipException
import org.testng.annotations.{ BeforeClass, Test }

import scala.sys.process.{ Process, ProcessLogger }

class GsnapTest extends TestNGSuite with Matchers {

  @BeforeClass def checkExecutable() = {
    val wrapper = new Gsnap(null) {
      override def globalConfig = new Config(Map("db" -> "mock"))
    }
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
      throw new SkipException("Skipping GSNAP test because the executable can not be found")
  }

  @Test(description = "GSNAP version number capture from executable")
  def testVersion() = {
    new Gsnap(null) {
      override def globalConfig = new Config(Map("db" -> "mock"))
    }.getVersion should not be "N/A"
  }
}
