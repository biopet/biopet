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
package nl.lumc.sasc.biopet.utils.intervals

import java.io.{PrintWriter, File}

import htsjdk.samtools.util.Interval
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{Test, AfterClass, BeforeClass}

import scala.io.Source

/**
  * Created by pjvan_thof on 8/25/15.
  */
class BedRecordListTest extends TestNGSuite with Matchers {
  @BeforeClass
  def start: Unit = {
    {
      val writer = new PrintWriter(BedRecordListTest.bedFile)
      writer.print(BedRecordListTest.bedContent)
      writer.close()
    }
    {
      val writer = new PrintWriter(BedRecordListTest.corruptBedFile)
      writer.print(BedRecordListTest.corruptBedContent)
      writer.close()
    }
    {
      val writer = new PrintWriter(BedRecordListTest.bedFileUcscHeader)
      writer.print(BedRecordListTest.ucscHeader)
      writer.print(BedRecordListTest.bedContent)
      writer.close()
    }
  }

  @Test
  def testReadBedFile {
    val records = BedRecordList.fromFile(BedRecordListTest.bedFile)
    records.allRecords.size shouldBe 2
    records.header shouldBe Nil

    val tempFile = File.createTempFile("region", ".bed")
    tempFile.deleteOnExit()
    records.writeToFile(tempFile)
    BedRecordList.fromFile(tempFile) shouldBe records
    tempFile.delete()
  }

  @Test
  def testReadBedFileUcscHeader {
    val records = BedRecordList.fromFile(BedRecordListTest.bedFileUcscHeader)
    records.allRecords.size shouldBe 2
    records.header shouldBe BedRecordListTest.ucscHeader.split("\n").toList

    val tempFile = File.createTempFile("region", ".bed")
    tempFile.deleteOnExit()
    records.writeToFile(tempFile)
    BedRecordList.fromFile(tempFile) shouldBe records
    tempFile.delete()
  }

  @Test def testSorted: Unit = {
    val unsorted =
      BedRecordList.fromList(List(BedRecord("chrQ", 10, 20), BedRecord("chrQ", 0, 10)))
    unsorted.isSorted shouldBe false
    unsorted.sorted.isSorted shouldBe true
    val sorted = BedRecordList.fromList(List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 10, 20)))
    sorted.isSorted shouldBe true
    sorted.sorted.isSorted shouldBe true
    sorted.hashCode() shouldBe sorted.sorted.hashCode()
  }

  @Test def testOverlap: Unit = {
    val list = BedRecordList.fromList(List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 10, 20)))
    list.overlapWith(BedRecord("chrQ", 5, 15)).size shouldBe 2
    list.overlapWith(BedRecord("chrQ", 0, 10)).size shouldBe 1
    list.overlapWith(BedRecord("chrQ", 10, 20)).size shouldBe 1
    list.overlapWith(BedRecord("chrQ", 19, 25)).size shouldBe 1
    list.overlapWith(BedRecord("chrQ", 20, 25)).size shouldBe 0
  }

  @Test def testLength: Unit = {
    val list = BedRecordList.fromList(List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 10, 20)))
    list.length shouldBe 20
  }

  @Test def testCombineOverlap: Unit = {
    val noOverlapList =
      BedRecordList.fromList(List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 10, 20)))
    noOverlapList.length shouldBe 20
    noOverlapList.combineOverlap.length shouldBe 20

    val overlapList = BedRecordList.fromList(
      List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 5, 15), BedRecord("chrQ", 10, 20)))
    overlapList.length shouldBe 30
    overlapList.combineOverlap.length shouldBe 20
  }

  @Test def testSquishBed: Unit = {
    val noOverlapList =
      BedRecordList.fromList(List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 10, 20)))
    noOverlapList.length shouldBe 20
    noOverlapList.squishBed().length shouldBe 20

    val overlapList = BedRecordList.fromList(
      List(
        BedRecord("chrQ", 0, 10),
        BedRecord("chrQ", 5, 15),
        BedRecord("chrQ", 10, 20),
        BedRecord("chrQ", 25, 35),
        BedRecord("chrQ", 50, 80),
        BedRecord("chrQ", 60, 70)
      ))
    overlapList.length shouldBe 80
    val squishedList = overlapList.squishBed(strandSensitive = false, nameSensitive = false)
    squishedList.allRecords.size shouldBe 5
    squishedList.length shouldBe 40
  }

  @Test def testSamInterval: Unit = {
    val list = BedRecordList.fromList(List(BedRecord("chrQ", 0, 10), BedRecord("chrQ", 5, 15)))
    list.toSamIntervals.toList shouldBe List(new Interval("chrQ", 1, 10),
                                             new Interval("chrQ", 6, 15))
  }

  @Test def testTraversable: Unit = {
    val list = List(BedRecord("chrQ", 0, 10))
    BedRecordList.fromList(list) shouldBe BedRecordList.fromList(list.toIterator)
  }

  @Test def testErrors: Unit = {
    intercept[IllegalArgumentException] {
      val records = BedRecordList.fromFile(BedRecordListTest.corruptBedFile)
    }
  }

  @Test def testScatter: Unit = {
    val list =
      BedRecordList.fromList(
        List(BedRecord("chrQ", 0, 1000),
             BedRecord("chrQ", 3000, 3500),
             BedRecord("chrQ", 3600, 3610),
             BedRecord("chrQ", 3610, 3620),
             BedRecord("chrQ", 3620, 3630)))
    val scatterList = list.scatter(100)
    scatterList.flatten.size shouldBe 18
    scatterList.size shouldBe 16
    scatterList.flatten.map(_.length).sum shouldBe 1530
  }
}

object BedRecordListTest {
  val ucscHeader =
    """browser position chr7:127471196-127495720
                     |browser hide all
                     |track name="ItemRGBDemo" description="Item RGB demonstration" visibility=2 itemRgb="On"
                     |""".stripMargin
  val bedContent = """chr22	1000	5000	cloneA	960	+	1000	5000	0	2	567,488	0,3512
                  |chr22	2000	6000	cloneB	900	-	2000	6000	0	2	433,399	0,3601""".stripMargin
  val corruptBedContent = """chr22	5000	1000	cloneA	960	+	1000	5000	0	2	567,488	0,3512
                     |chr22	2000	6000	cloneB	900	-	2000	6000	0	2	433,399	0,3601""".stripMargin

  val bedFile = File.createTempFile("regions", ".bed")
  bedFile.deleteOnExit()
  val corruptBedFile = File.createTempFile("regions", ".bed")
  corruptBedFile.deleteOnExit()
  val bedFileUcscHeader = File.createTempFile("regions", ".bed")
  bedFileUcscHeader.deleteOnExit()
}
