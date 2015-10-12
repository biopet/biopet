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
class BcfToolsTest extends TestNGSuite with MockitoSugar with Matchers {

  @Test
  def BcfToolsViewTest = {
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

    // view.cmdLine should equal(s"bcftools -o $outputPath $inputPath}")
  }

}
