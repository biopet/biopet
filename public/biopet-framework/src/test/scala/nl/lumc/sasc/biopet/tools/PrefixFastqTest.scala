package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import htsjdk.samtools.fastq.FastqReader
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.JavaConversions._


/**
 * Created by ahbbollen on 28-8-15.
 */
class PrefixFastqTest extends TestNGSuite with MockitoSugar with Matchers {

  import PrefixFastq._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val fq = resourcePath("/paired01a.fq")

  @Test
  def testMain() = {
    val temp = File.createTempFile("out", ".fastq")

    val args = Array("-i", fq, "-o", temp.getAbsolutePath, "-s", "AAA")
    main(args)
  }

  @Test
  def testOutput() = {
    val temp = File.createTempFile("out", ".fastq")

    val args = Array("-i", fq, "-o", temp.getAbsolutePath, "-s", "AAA")
    main(args)

    val reader = new FastqReader(temp)

    for (read <- reader.iterator()){
      read.getReadString.startsWith("AAA") shouldBe true
    }
  }
}
