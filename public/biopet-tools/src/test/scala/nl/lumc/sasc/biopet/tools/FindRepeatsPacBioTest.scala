package nl.lumc.sasc.biopet.tools

import java.io.File
import java.nio.file.Paths

import htsjdk.samtools.{ SamReaderFactory, QueryInterval }
import nl.lumc.sasc.biopet.tools.FastqSplitter._
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.collection.immutable.Nil

/**
 * Created by ahbbollen on 27-8-15.
 */
class FindRepeatsPacBioTest extends TestNGSuite with MockitoSugar with Matchers {

  import FindRepeatsPacBio._
  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val bed = resourcePath("/rrna01.bed")
  val bam = resourcePath("/paired01.bam")

  @Test
  def testMain() = {

    val outputFile = File.createTempFile("repeats", ".tsv")
    val args = Array("-I", bam, "-b", bed, "-o", outputFile.toString)
    main(args)
  }

  @Test
  def testResult() = {
    val samReader = SamReaderFactory.makeDefault().open(new File(bam))
    val header = samReader.getFileHeader
    val record = samReader.iterator().next()
    val interval = new QueryInterval(header.getSequenceIndex("chrQ"), 50, 55)
    val result = procesSamrecord(record, interval)

    result.isEmpty shouldBe false

    result.get.samRecord shouldEqual record
    result.get.beginDel should be >= 0
    result.get.endDel should be >= 0
  }

  @Test
  def testResultObject = {
    val record = SamReaderFactory.makeDefault().open(new File(bam)).iterator().next()
    val result = new Result
    result.samRecord = record

    result.samRecord shouldEqual record
    result.beginDel shouldBe 0
    result.endDel shouldBe 0
    result.dels shouldEqual Nil
    result.ins shouldEqual Nil
  }

}
