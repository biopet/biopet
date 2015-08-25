package nl.lumc.sasc.biopet.utils.intervals

import java.io.{PrintWriter, File}

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
      val writer = new PrintWriter(BedRecordListTest.bedFileUcscHeader)
      writer.print(BedRecordListTest.ucscHeader)
      writer.print(BedRecordListTest.bedContent)
      writer.close()
    }
  }

  @Test def test {
    println("test")
  }

  @AfterClass
  def end: Unit = {
    println(BedRecordListTest.bedFile)
    Source.fromFile(BedRecordListTest.bedFile).getLines().foreach(println)
    BedRecordListTest.bedFile.delete()
    BedRecordListTest.bedFileUcscHeader.delete()
  }
}

object BedRecordListTest {
  val ucscHeader = """browser position chr7:127471196-127495720
                     |browser hide all
                     |track name="ItemRGBDemo" description="Item RGB demonstration" visibility=2 itemRgb="On"
                     |""".stripMargin
  val bedContent = """chr22	1000	5000	cloneA	960	+	1000	5000	0	2	567,488,	0,3512
                  |chr22	2000	6000	cloneB	900	-	2000	6000	0	2	433,399,	0,3601""".stripMargin
  val bedFile = File.createTempFile("regions", ".bed")
  val bedFileUcscHeader = File.createTempFile("regions", ".bed")
}