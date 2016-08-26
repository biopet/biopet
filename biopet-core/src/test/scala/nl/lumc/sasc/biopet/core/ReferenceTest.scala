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
package nl.lumc.sasc.biopet.core

import java.nio.file.Paths

import nl.lumc.sasc.biopet.utils.{ ConfigUtils, Logging }
import nl.lumc.sasc.biopet.utils.config.{ Configurable, Config }
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.mockito.Mockito._
import org.testng.annotations.Test

/**
 * Created by pjvan_thof on 12/30/15.
 */
class ReferenceTest extends TestNGSuite with Matchers with MockitoSugar {

  import ReferenceTest._

  @Test
  def testDefault: Unit = {
    Logging.errors.clear()
    make(config :: testReferenceNoIndex :: Nil).referenceFasta()
    Logging.checkErrors(true)

    make(config :: testReference :: Nil).referenceFasta()
    Logging.checkErrors(true)
  }

  @Test
  def testIndexes: Unit = {
    make(config :: testReferenceNoIndex :: Nil, fai = true, dict = true).referenceFasta()

    intercept[IllegalStateException] {
      Logging.checkErrors(true)
    }

    val a = make(config :: testReference :: Nil, fai = true, dict = true)
    a.referenceFasta()
    a.referenceSummary shouldBe Map(
      "contigs" -> Map("chrQ" -> Map("md5" -> Some("94445ec460a68206ae9781f71697d3db"), "length" -> 16571)),
      "species" -> "test_species",
      "name" -> "test_genome")
    Logging.checkErrors(true)
  }

}

object ReferenceTest {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val config = Map("species" -> "test_species", "reference_name" -> "test_genome")

  val testReferenceNoIndex = Map(
    "references" -> Map(
      "test_species" -> Map(
        "test_genome" -> Map(
          "reference_fasta" -> resourcePath("/fake_chrQ_no_index.fa")))))

  val testReference = Map(
    "references" -> Map(
      "test_species" -> Map(
        "test_genome" -> Map(
          "reference_fasta" -> resourcePath("/fake_chrQ.fa")))))

  def make(configs: List[Map[String, Any]],
           r: Configurable = null,
           fai: Boolean = false,
           dict: Boolean = false) = new Reference {
    val root = r
    override def globalConfig = new Config(configs
      .foldLeft(Map[String, Any]()) { case (a, b) => ConfigUtils.mergeMaps(a, b) })
    override def dictRequired = if (dict) true else super.dictRequired
    override def faiRequired = if (fai) true else super.faiRequired
  }
}