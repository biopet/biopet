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
package nl.lumc.sasc.biopet.extensions

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by ahbbollen on 3-3-16.
 */
class VariantEffectPredictorTest extends TestNGSuite with Matchers {

  @Test
  def testSummaryStats = {
    val file = new File(Paths.get(getClass.getResource("/vep.metrics").toURI).toString)

    val vep = new VariantEffectPredictor(null)
    val stats = vep.parseStatsFile(file)

    stats.contains("VEP_run_statistics") shouldBe true
    stats.contains("General_statistics") shouldBe true
    stats.contains("Consequences_(most_severe)") shouldBe true
    stats.contains("Consequences_(all)") shouldBe true
    stats.contains("Coding_consequences") shouldBe true
    stats.contains("SIFT_summary") shouldBe true
    stats.contains("PolyPhen_summary") shouldBe true
    stats.contains("Variants_by_chromosome") shouldBe true
    stats.contains("Distribution_of_variants_on_chromosome_1") shouldBe true
    stats.contains("Position_in_protein") shouldBe true

  }

}
