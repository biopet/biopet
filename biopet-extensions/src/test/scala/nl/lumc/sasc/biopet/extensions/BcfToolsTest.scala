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

import java.io.File

import nl.lumc.sasc.biopet.extensions.bcftools.BcftoolsView
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by ahbbollen on 12-10-15.
  */
class BcfToolsTest extends TestNGSuite with Matchers with MockitoSugar {

  @Test
  def BcfToolsViewTest(): Unit = {
    val view = new BcftoolsView(null)

    view.executable = "bcftools"

    val tmpInput = File.createTempFile("bcftoolstest", ".vcf")
    tmpInput.deleteOnExit()
    val tmpOutput = File.createTempFile("bcftoolstest", ".vcf.gz")
    tmpOutput.deleteOnExit()
    val inputPath = tmpInput.getAbsolutePath
    val outputPath = tmpOutput.getAbsolutePath

    view.input = tmpInput
    view.output = tmpOutput

    view.cmd should equal(s"bcftools view -l 9 -O z -o $outputPath $inputPath")
  }

}
