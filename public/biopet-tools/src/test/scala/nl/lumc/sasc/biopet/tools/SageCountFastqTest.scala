package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by ahbbollen on 28-8-15.
 */
class SageCountFastqTest extends TestNGSuite with MockitoSugar with Matchers {
  import SageCountFastq._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val fq = resourcePath("/paired01a.fq")

  @Test
  def testMain() = {
    val temp = File.createTempFile("out", ".fastq")
    temp.deleteOnExit()

    val args = Array("-I", fq, "-o", temp.getAbsolutePath)
    main(args)
  }

}
