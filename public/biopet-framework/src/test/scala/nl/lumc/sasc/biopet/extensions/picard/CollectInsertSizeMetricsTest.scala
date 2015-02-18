package nl.lumc.sasc.biopet.extensions.picard

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 2/18/15.
 */
class CollectInsertSizeMetricsTest extends TestNGSuite with Matchers {

  @Test
  def summaryData: Unit = {
    val file = new File(Paths.get(getClass.getResource("/picard.insertsizemetrics").toURI).toString)
    val job = new CollectInsertSizeMetrics(null)
    job.output = file

    job.summaryData
  }
}
