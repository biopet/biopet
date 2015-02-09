/**
 * Copyright (c) 2015 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.pipelines.gentrap

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import nl.lumc.sasc.biopet.core.config.Config

class GentrapTest extends TestNGSuite with Matchers {

  /** Method to set test config */
  // since the pipeline Config is a global value, we first store the
  // initial config into a value, store the supplied value, then return
  // the initial config for restoring later
  private def setConfig(map: Map[String, Any]): Map[String, Any] = {
    val oldMap: Map[String, Any] = Config.global.map.toMap
    Config.global.map = map
    oldMap
  }

  /** Method to set the global config */
  private def restoreConfig(initConfig: Map[String, Any]): Unit = Config.global.map = initConfig

  /** Minimum config required for Gentrap */
  private val minimumConfig = Map(
    "output_dir" -> "/tmp",
    "aligner" -> "gsnap",
    "reference" -> "mock",
    "gsnap" -> Map("db" -> "fixt_gentrap_hg19"),
    "samples" -> Map(
      "sample_1" -> Map(
        "libraries" -> Map(
          "lib_1" -> Map(
            "R1" -> "/tmp/mock.fq"
          )
        )
      )
    )
  )

  // Test pipeline initialization with minimum config -- there should be no exceptions raised
  @Test def testInitMinimumConfig() = {
    val initialConfig = setConfig(minimumConfig)
    val gentrap = new Gentrap()
    restoreConfig(initialConfig)
  }
}
