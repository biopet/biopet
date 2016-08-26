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
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class FastqcV0101Test extends TestNGSuite with Matchers {

  /** Returns the absolute path to test resource directory as a File object */
  private[flexiprep] val resourceDir: File = new File(Paths.get(getClass.getResource("/").toURI).toString)

  /** Given a resource file name, returns the the absolute path to it as a File object */
  private[flexiprep] def resourceFile(p: String): File = new File(resourceDir, p)

  /** Mock output file of a FastQC v0.10.1 run */
  // the file doesn't actually exist, we just need it so the outputDir value can be computed correctly
  private[flexiprep] val outputv0101: File = resourceFile("v0101.fq_fastqc.zip")

  @Test def testOutputDir() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    fqc.outputDir shouldBe new File(resourceDir, "v0101.fq_fastqc")
  }

  @Test def testQcModules() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    // 11 QC modules
    fqc.qcModules.size shouldBe 12
    // first module
    fqc.qcModules.keySet should contain("Basic Statistics")
    // mid (6th module)
    fqc.qcModules.keySet should contain("Per sequence GC content")
    // last module
    fqc.qcModules.keySet should contain("Kmer Content")
  }

  @Test def testSingleQcModule() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    fqc.qcModules("Basic Statistics").name should ===("Basic Statistics")
    fqc.qcModules("Basic Statistics").status should ===("pass")
    fqc.qcModules("Basic Statistics").lines.size shouldBe 8
  }

  @Test def testEncoding() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    fqc.encoding shouldBe "Sanger / Illumina 1.9"
  }

  @Test def testFoundAdapter() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    fqc.contaminants = Option(resourceFile("fqc_contaminants_v0101.txt"))
    // found adapters also contain the adapters in reverse complement (done within flexiprep/fastqc only)
    val adapters = fqc.foundAdapters

    if (fqc.enableRCtrimming) {
      // we find 1 adapter which comes with the Reverse Complement counterpart
      adapters.size shouldBe 2
      adapters.head.name shouldEqual "TruSeq Adapter, Index 1_RC"
      adapters.head.seq shouldEqual "CAAGCAGAAGACGGCATACGAGATCGTGATGTGACTGGAGTTCAGACGTGTGCTCTTCCGATC"
    } else {
      adapters.size shouldBe 1
    }

    adapters.last.name shouldEqual "TruSeq Adapter, Index 1"
    adapters.last.seq shouldEqual "GATCGGAAGAGCACACGTCTGAACTCCAGTCACATCACGATCTCGTATGCCGTCTTCTGCTTG"

  }

  @Test def testPerBaseSequenceQuality() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101

    val perBaseSequenceQuality = fqc.perBaseSequenceQuality
    perBaseSequenceQuality.size shouldBe 55
    perBaseSequenceQuality.keys should contain("54-55")
  }

  @Test def testPerBaseSequenceContent() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101

    val perBaseSequenceContent: Map[String, Map[String, Double]] = fqc.perBaseSequenceContent
    perBaseSequenceContent.size shouldBe 55
    perBaseSequenceContent.keys should contain("1")
  }

}