package nl.lumc.sasc.biopet.extensions.clever

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by wyleung on 13-5-16.
  */
class CleverFixVCFTest extends TestNGSuite with Matchers {

  @Test
  def replacementSucces = {
    CleverFixVCF.replaceHeaderLine(
      CleverFixVCF.vcfColHeader,
      CleverFixVCF.vcfColHeader,
      CleverFixVCF.vcfColReplacementHeader + "testsample",
      CleverFixVCF.extraHeader
    ) should equal(CleverFixVCF.extraHeader + "\n" + CleverFixVCF.vcfColReplacementHeader + "testsample"+ "\n")
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
    ) should equal(vcfRecordExpected+ "\n")
  }

}
