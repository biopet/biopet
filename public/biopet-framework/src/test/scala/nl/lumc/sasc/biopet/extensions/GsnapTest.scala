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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import scala.sys.process.{ Process, ProcessLogger }

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.SkipException
import org.testng.annotations.{ BeforeClass, Test }

import nl.lumc.sasc.biopet.core.config.Config

class GsnapTest extends TestNGSuite with Matchers {

  private def setConfig(key: String, value: String): Map[String, Any] = {
    val oldMap: Map[String, Any] = Config.global.map.toMap
    Config.global.map += (key -> value)
    oldMap
  }

  private def restoreConfig(oldMap: Map[String, Any]): Unit = Config.global.map = oldMap

  @BeforeClass def checkExecutable() = {
    val oldMap = setConfig("db", "mock")
    val wrapper = new Gsnap(null)
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
    restoreConfig(oldMap)
  }

  @Test(description = "GSNAP version number capture from executable")
  def testVersion() = {
    val oldMap = setConfig("db", "mock")
    new Gsnap(null).getVersion should not be "N/A"
    restoreConfig(oldMap)
  }
}
