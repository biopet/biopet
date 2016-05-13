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
  private[clever] val resourceDir: File = new File(Paths.get(getClass.getResource(".").toURI).toString)

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
    ) should equal(CleverFixVCF.extraHeader + "\n" + CleverFixVCF.vcfColReplacementHeader + "testsample" + "\n")
  }

  @Test
  def replacementOther = {
    val vcfRecord = "chrM\t312\tL743020\t.\t<DEL>\t.\tPASS\tBPWINDOW=313,16189;CILEN=15866,15888;IMPRECISE;SVLEN=-15877;SVTYPE=DEL\tGT:DP\t1/.:103"
    val vcfRecordExpected = "chrM\t312\tL743020\tN\t<DEL>\t.\tPASS\tBPWINDOW=313,16189;CILEN=15866,15888;IMPRECISE;SVLEN=-15877;SVTYPE=DEL\tGT:DP\t1/.:103"
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

    val result = CleverFixVCF.main(Array(
      "-i", rawCleverVCF.getAbsolutePath,
      "-o", output.getAbsolutePath,
      "-s", "testsample"
    ))

    val exp = Source.fromFile(expectedCleverVCF).getLines()
    val obs = Source.fromFile(output).getLines()

    (exp zip obs).foreach(_ match {
      case (a,b) => {
        a shouldEqual(b)
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

    cfvcf.cmdLine should include ("'-s' 'testsample'")
    cfvcf.cmdLine should include (s"'-i' '${rawCleverVCF}'")
    cfvcf.cmdLine should include (s"'-o' '${output}'")
  }
}
