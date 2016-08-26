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
package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
 * Created by ahbbollen on 7-9-15.
 */
class SageCreateTagCountsTest extends TestNGSuite with MockitoSugar with Matchers {

  import SageCreateTagCounts._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testMain = {
    val input = resourcePath("/tagCount.tsv")
    val tagLib = resourcePath("/sageTest.tsv")

    val sense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    sense.deleteOnExit()
    val allSense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    allSense.deleteOnExit()
    val antiSense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    antiSense.deleteOnExit()
    val allAntiSense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    allAntiSense.deleteOnExit()

    noException should be thrownBy main(Array("-I", input, "--tagLib", tagLib,
      "--countSense", sense.getAbsolutePath, "--countAllSense", allSense.getAbsolutePath,
      "--countAntiSense", antiSense.getAbsolutePath, "--countAllAntiSense", allAntiSense.getAbsolutePath))
    noException should be thrownBy main(Array("-I", input, "--tagLib", tagLib,
      "--countSense", sense.getAbsolutePath, "--countAllSense", allSense.getAbsolutePath,
      "--countAntiSense", antiSense.getAbsolutePath))
    noException should be thrownBy main(Array("-I", input, "--tagLib", tagLib,
      "--countSense", sense.getAbsolutePath, "--countAllSense", allSense.getAbsolutePath))
    noException should be thrownBy main(Array("-I", input, "--tagLib", tagLib,
      "--countSense", sense.getAbsolutePath))
    noException should be thrownBy main(Array("-I", input, "--tagLib", tagLib))

  }

  @Test
  def testOutput = {
    val input = resourcePath("/tagCount.tsv")
    val tagLib = resourcePath("/sageTest.tsv")

    val sense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    sense.deleteOnExit()
    val allSense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    allSense.deleteOnExit()
    val antiSense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    antiSense.deleteOnExit()
    val allAntiSense = File.createTempFile("SageCreateTagCountsTEst", ".tsv")
    allAntiSense.deleteOnExit()

    main(Array("-I", input, "--tagLib", tagLib, "--countSense", sense.getAbsolutePath,
      "--countAllSense", allSense.getAbsolutePath, "--countAntiSense", antiSense.getAbsolutePath,
      "--countAllAntiSense", allAntiSense.getAbsolutePath))

    Source.fromFile(sense).mkString should equal("ENSG00000254767\t40\nENSG00000255336\t55\n")
    Source.fromFile(allSense).mkString should equal("ENSG00000254767\t70\nENSG00000255336\t90\n")
    Source.fromFile(antiSense).mkString should equal("ENSG00000254767\t50\nENSG00000255336\t45\n")
    Source.fromFile(allAntiSense).mkString should equal("ENSG00000254767\t75\nENSG00000255336\t65\n")
  }

}
