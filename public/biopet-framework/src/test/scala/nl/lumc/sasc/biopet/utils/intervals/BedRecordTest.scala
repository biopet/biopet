package nl.lumc.sasc.biopet.utils.intervals

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvanthof on 24/08/15.
 */
class BedRecordTest extends TestNGSuite with Matchers {
  @Test def testLineParse: Unit = {
    BedRecord("chrQ", 0, 4) shouldBe BedRecord("chrQ", 0, 4)
    BedRecord.fromLine("chrQ\t0\t4") shouldBe BedRecord("chrQ", 0, 4)
    BedRecord.fromLine("chrQ\t0\t4\tname\t3\t+") shouldBe BedRecord("chrQ", 0, 4, Some("name"), Some(3.0), Some(true))
    BedRecord.fromLine("chrQ\t0\t4\tname\t3\t+\t1\t3") shouldBe
      BedRecord("chrQ", 0, 4, Some("name"), Some(3.0), Some(true), Some(1), Some(3))
    BedRecord.fromLine("chrQ\t0\t4\tname\t3\t+\t1\t3\t255,0,0") shouldBe
      BedRecord("chrQ", 0, 4, Some("name"), Some(3.0), Some(true), Some(1), Some(3), Some((255,0,0)))
    BedRecord.fromLine("chrQ\t0\t100\tname\t3\t+\t1\t3\t255,0,0\t2\t10,20\t20,50") shouldBe
      BedRecord("chrQ", 0, 100, Some("name"), Some(3.0), Some(true), Some(1), Some(3), Some((255,0,0)),
        Some(2), IndexedSeq(10,20), IndexedSeq(20,50))
  }

  @Test def testLineOutput: Unit = {
    BedRecord("chrQ", 0, 4).toString shouldBe "chrQ\t0\t4"
    BedRecord.fromLine("chrQ\t0\t4").toString shouldBe "chrQ\t0\t4"
  }

  @Test def testOverlap: Unit = {
    BedRecord("chrQ", 0, 4).overlapWith(BedRecord("chrQ", 0, 4)) shouldBe true
    BedRecord("chrQ", 0, 4).overlapWith(BedRecord("chrX", 0, 4)) shouldBe false
    BedRecord("chrQ", 0, 4).overlapWith(BedRecord("chrQ", 4, 8)) shouldBe false
    BedRecord("chrQ", 0, 4).overlapWith(BedRecord("chrQ", 3, 8)) shouldBe true
    BedRecord("chrQ", 4, 8).overlapWith(BedRecord("chrQ", 0, 4)) shouldBe false
    BedRecord("chrQ", 3, 4).overlapWith(BedRecord("chrQ", 0, 4)) shouldBe true
    BedRecord("chrQ", 3, 4).overlapWith(BedRecord("chrQ", 4, 5)) shouldBe false
  }

  @Test def testLength: Unit = {
    BedRecord("chrQ", 0, 4).length shouldBe 4
    BedRecord("chrQ", 0, 1).length shouldBe 1
    BedRecord("chrQ", 3, 4).length shouldBe 1
    BedRecord.fromLine("chrQ\t0\t100\tname\t3\t+\t1\t3\t255,0,0\t2\t10,20\t20,50").exons.get.foldLeft(0)(_ + _.length) shouldBe 30
    BedRecord.fromLine("chrQ\t0\t100\tname\t3\t+\t1\t3\t255,0,0\t2\t10,20\t20,50").introns.get.foldLeft(0)(_ + _.length) shouldBe 20
  }

  @Test def testErrors: Unit = {
    BedRecord("chrQ", 0, 3).validate
    BedRecord.fromLine("chrQ\t0\t100\tname\t3\t+\t1\t3\t255,0,0\t2\t10,20\t20,50").validate
    intercept[IllegalArgumentException] {
      BedRecord("chrQ", 0, 0).validate
    }
    intercept[IllegalArgumentException] {
      BedRecord("chrQ", 4, 3).validate
    }
    intercept[IllegalArgumentException] {
      BedRecord.fromLine("chrQ\t0\t100\tname\t3\t+\t1\t3\t255,0,0\t2\t10\t50").validate
    }
    intercept[IllegalStateException] {
      BedRecord.fromLine("chrQ\t0\t100\tname\t3\tx\t1\t3\t255,0,0\t2\t10,20\t20,50").validate
    }
  }
}
