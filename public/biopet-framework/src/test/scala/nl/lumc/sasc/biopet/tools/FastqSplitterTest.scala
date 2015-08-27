package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by ahbbollen on 27-8-15.
 */
class FastqSplitterTest extends TestNGSuite with MockitoSugar with Matchers {

  import FastqSplitter._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val fq = resourcePath("/paired01a.fq")

  @Test
  def testMain() = {
    val temp = File.createTempFile("out", ".fastq")

    val args = Array("-I", fq, "-o", temp.getAbsolutePath)
    main(args)
  }

  @Test
  def testManyOutMain() = {
    val files = (0 until 10).map(_ => File.createTempFile("out", ".fastq"))
    var args = Array("-I", fq)
    files.foreach(x => args ++= Array("-o", x.getAbsolutePath))
    main(args)
  }



}
