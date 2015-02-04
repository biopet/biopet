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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
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
  private val resourceDir: File = new File(Paths.get(getClass.getResource("/").toURI).toString)

  /** Given a resource file name, returns the the absolute path to it as a File object */
  private def resourceFile(p: String): File = new File(resourceDir, p)

  /** Mock output file of a FastQC v0.10.1 run */
  // the file doesn't actually exist, we just need it so the outputDir value can be computed correctly
  private val outputv0101: File = resourceFile("v0101.fq_fastqc.zip")

  @Test def testOutputDir() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    fqc.outputDir shouldBe new File(resourceDir, "v0101.fq_fastqc")
  }

  @Test def testQcModules() = {
    val fqc = new Fastqc(null)
    fqc.output = outputv0101
    // 11 QC modules
    fqc.qcModules.size shouldBe 11
    // first module
    fqc.qcModules.keySet should contain("Basic Statistics")
    // mid (6th module)
    fqc.qcModules.keySet should contain("Per sequence GC content")
    // last module
    fqc.qcModules.keySet should contain("Kmer Content")
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
    val adapters = fqc.foundAdapters
    adapters.size shouldBe 1
    adapters.head.name should ===("TruSeq Adapter, Index 1")
    // from fqc_contaminants_v0101.txt
    adapters.head.seq should ===("GATCGGAAGAGCACACGTCTGAACTCCAGTCACATCACGATCTCGTATGCCGTCTTCTGCTTG")
  }
}