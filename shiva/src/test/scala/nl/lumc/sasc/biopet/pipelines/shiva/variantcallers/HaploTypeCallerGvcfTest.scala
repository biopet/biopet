package nl.lumc.sasc.biopet.pipelines.shiva.variantcallers

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by Sander Bollen on 13-7-16.
  */
class HaploTypeCallerGvcfTest extends TestNGSuite with Matchers {

  @Test
  def testGvcfFiles = {
    val samples = List("sample01", "sample02", "sample03")
    val hc = new HaplotypeCallerGvcf(null)
    hc.inputBams = createInputMap(samples)
    hc.biopetScript()

    hc.getGvcfs.size shouldBe 3
    hc.getGvcfs.keys.toList shouldEqual samples
  }

  def createInputMap(samples: List[String]): Map[String, File] = {
    samples map { x =>
      val file = File.createTempFile(x, ".bam")
      file.deleteOnExit()
      x -> file
    } toMap
  }

}
