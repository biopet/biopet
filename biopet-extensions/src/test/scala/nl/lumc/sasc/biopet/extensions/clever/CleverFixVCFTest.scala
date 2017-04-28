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
package nl.lumc.sasc.biopet.extensions.clever

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import scala.io.Source

/**
  * Created by wyleung on 13-5-16.
  */
class CleverFixVCFTest extends TestNGSuite with Matchers {

  /** Returns the absolute path to test resource directory as a File object */
  private[clever] val resourceDir: File = new File(
    Paths.get(getClass.getResource(".").toURI).toString)

  /** Given a resource file name, returns the the absolute path to it as a File object */
  private[clever] def resourceFile(p: String): File = new File(resourceDir, p)

  val rawCleverVCF = resourceFile("test.clever.vcf")
  val expectedCleverVCF = resourceFile("expectedresult.clever.vcf")

  @Test
  def replacementSucces = {
    CleverFixVCF.replaceHeaderLine(
      CleverFixVCF.vcfColHeader,
      CleverFixVCF.vcfColHeader,
      CleverFixVCF.vcfColReplacementHeader + "testsample",
      CleverFixVCF.extraHeader
    ) should equal(
      CleverFixVCF.extraHeader + "\n" + CleverFixVCF.vcfColReplacementHeader + "testsample" + "\n")
  }

  @Test
  def replacementOther = {
    val vcfRecord =
      "chrM\t312\tL743020\t.\t<DEL>\t.\tPASS\tBPWINDOW=313,16189;CILEN=15866,15888;IMPRECISE;SVLEN=-15877;SVTYPE=DEL\tGT:DP\t1/.:103"
    val vcfRecordExpected =
      "chrM\t312\tL743020\tN\t<DEL>\t.\tPASS\tBPWINDOW=313,16189;CILEN=15866,15888;IMPRECISE;SVLEN=-15877;SVTYPE=DEL\tGT:DP\t1/.:103"
    CleverFixVCF.replaceHeaderLine(
      vcfRecord,
      CleverFixVCF.vcfColHeader,
      CleverFixVCF.vcfColReplacementHeader + "testsample",
      CleverFixVCF.extraHeader
    ) should equal(vcfRecordExpected + "\n")
  }

  @Test
  def mainTest = {
    val output = File.createTempFile("clever", ".test.vcf")
    output.deleteOnExit()

    val result = CleverFixVCF.main(
      Array(
        "-i",
        rawCleverVCF.getAbsolutePath,
        "-o",
        output.getAbsolutePath,
        "-s",
        "testsample"
      ))

    val exp = Source.fromFile(expectedCleverVCF).getLines()
    val obs = Source.fromFile(output).getLines()

    (exp zip obs).foreach(_ match {
      case (a, b) => {
        a shouldEqual (b)
      }
      case _ =>
    })
  }

  @Test
  def javaCommand = {
    val output = File.createTempFile("clever", ".test.vcf")
    output.deleteOnExit()
    val cfvcf = new CleverFixVCF(null)
    cfvcf.input = rawCleverVCF
    cfvcf.output = output
    cfvcf.sampleName = "testsample"

    cfvcf.cmdLine should include("'-s' 'testsample'")
    cfvcf.cmdLine should include(s"'-i' '${rawCleverVCF}'")
    cfvcf.cmdLine should include(s"'-o' '${output}'")
  }
}
