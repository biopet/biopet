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

import scala.util.Random

/**
 * Test class for [[MergeAlleles]]
 *
 * Created by ahbbollen on 10-4-15.
 */
class MergeAllelesTest extends TestNGSuite with MockitoSugar with Matchers {
  import MergeAlleles._

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val veppedPath = resourcePath("/chrQ.vcf.gz")
  val reference = resourcePath("/fake_chrQ.fa")

  // These two have to created
  // resourcepath copies files to another directory
  // hence we need to supply all needed files
  val dict = resourcePath("/fake_chrQ.dict")
  val fai = resourcePath("/fake_chrQ.fa.fai")

  val rand = new Random()

  @Test def testOutputTypeVcf() = {
    val tmp = File.createTempFile("MergeAlleles", ".vcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", veppedPath, "-o", tmpPath, "-R", reference)
    main(arguments)
  }

  @Test def testOutputTypeVcfGz() = {
    val tmp = File.createTempFile("MergeAlleles", ".vcf.gz")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", veppedPath, "-o", tmpPath, "-R", reference)
    main(arguments)
  }

  @Test def testOutputTypeBcf() = {
    val tmp = File.createTempFile("MergeAlleles", ".bcf")
    tmp.deleteOnExit()
    val tmpPath = tmp.getAbsolutePath
    val arguments = Array("-I", veppedPath, "-o", tmpPath, "-R", reference)
    main(arguments)
  }
}
