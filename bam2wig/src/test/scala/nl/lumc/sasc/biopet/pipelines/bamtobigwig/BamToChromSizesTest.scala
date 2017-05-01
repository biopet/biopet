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
package nl.lumc.sasc.biopet.pipelines.bamtobigwig

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvanthof on 09/05/16.
  */
class BamToChromSizesTest extends TestNGSuite with Matchers {
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def testChromSizes: Unit = {
    val bamFile = new File(resourcePath("/empty.bam"))
    val bamToChromSizes = new BamToChromSizes(null)
    bamToChromSizes.bamFile = bamFile
    bamToChromSizes.chromSizesFile = File.createTempFile("chrom.", ".sizes")
    bamToChromSizes.chromSizesFile.deleteOnExit()
    bamToChromSizes.run()
    Source.fromFile(bamToChromSizes.chromSizesFile).getLines().toList shouldBe List("chrQ\t10000",
                                                                                    "chrR\t10000")
  }
}
