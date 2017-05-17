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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import java.io.File

import org.testng.annotations.Test

class CutadaptTest extends FastqcV0101Test {

  /** Mock output file of a Cutadapt 1.9 run */
  private[flexiprep] val cutadaptOut: File = resourceFile("ct-test.R1.clip.stats")

  def testFastQCinstance: Fastqc = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    fqc.contaminants = Option(resourceFile("fqc_contaminants_v0112.txt"))
    fqc.enableRCtrimming = true
    fqc
  }

  def testCutadaptInst: Cutadapt = {
    val caExe = new Cutadapt(null, testFastQCinstance)
    caExe.statsOutput = cutadaptOut
    caExe
  }

  @Test def testAdapterFound() = {
    val cutadapt = testCutadaptInst
    val adapters = cutadapt.extractClippedAdapters(cutadaptOut)
    adapters.keys.size shouldBe 4

    adapters.get("CAAGCAGAAGACGGCATACGAGATCGTGATGTGACTGGAGTTCAGACGTGTGCTCTTCCGATC") shouldBe Some(
      Map(
        "count" -> 94,
        "histogram" -> Map(
          "5p" -> Map(5 -> 2, 6 -> 4, 9 -> 1, 3 -> 8, 4 -> 3),
          "3p" -> Map(5 -> 21, 6 -> 18, 9 -> 1, 12 -> 1, 7 -> 2, 3 -> 13, 11 -> 1, 4 -> 19)
        )
      )
    )

    adapters.get("CAAGCAGAAGACGGCATACGAGATGCGGACGTGACTGGAGTTCAGACGTGTGCTCTTCCGATC") shouldBe Some(
      Map(
        "count" -> 0,
        "histogram" -> Map()
      )
    )
  }

  @Test def testSummary() = {
    val cutadapt = testCutadaptInst
    val summary = cutadapt.summaryStats

    summary.keys shouldBe Set(
      "num_bases_input",
      "num_reads_input",
      "num_reads_output",
      "num_reads_with_adapters",
      "num_reads_affected",
      "num_reads_discarded_too_long",
      "adapters",
      "num_reads_discarded_many_n",
      "num_reads_discarded_too_short",
      "num_bases_output"
    )

    summary.keys.size shouldBe 10
    summary("adapters").asInstanceOf[Map[String, Map[String, Any]]].keys.size shouldBe 4

    summary("num_bases_input") shouldBe 100000
    summary("num_reads_input") shouldBe 1000
    summary("num_reads_output") shouldBe 985
    summary("num_reads_with_adapters") shouldBe 440
    summary("num_reads_affected") shouldBe 425
    summary("num_reads_discarded_too_long") shouldBe 0
    summary("num_reads_discarded_many_n") shouldBe 0
    summary("num_reads_discarded_too_short") shouldBe 15
    summary("num_bases_output") shouldBe 89423
  }
}
