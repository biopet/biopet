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
package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, Test}

/**
  * Created by pjvan_thof on 13-5-16.
  */
class DownloadGenomesTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any], dir: File): DownloadGenomes = {
    new DownloadGenomes() {
      override def configNamespace = "generateindexes"
      override def globalConfig =
        new Config(ConfigUtils.mergeMaps(map, DownloadGenomesTest.config(dir)))
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  private var dirs: List[File] = Nil

  @Test
  def testNoFastaUri(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map(), outputDir)
    pipeline.referenceConfig = Map("s1" -> Map("g1" -> Map("" -> "")))
    an[IllegalArgumentException] should be thrownBy pipeline.script()
  }

  @Test
  def testNcbiAssembly(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map(), outputDir)
    pipeline.referenceConfig = Map("s1" -> Map("g1" -> Map("ncbi_assembly_report" -> "id")))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testSingleFasta(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map(), outputDir)
    pipeline.referenceConfig = Map("s1" -> Map("g1" -> Map("fasta_uri" -> "uri")))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testMultiFasta(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map(), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map("g1" -> Map("fasta_uri" -> List("uri", "uri2", "uri3.gz"))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testSingleDbsnp(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map("g1" -> Map("fasta_uri" -> "uri",
                              "dbsnp" -> Map("version" -> Map("vcf_uri" -> "uri.vcf.gz")))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testContigMapDbsnp(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map(
        "g1" -> Map("fasta_uri" -> "uri",
                    "dbsnp" -> Map("version" -> Map("vcf_uri" -> "uri.vcf.gz",
                                                    "contig_map" -> Map("1" -> "chr1"))))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testUnzippedContigMapDbsnp(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map(
        "g1" -> Map("fasta_uri" -> "uri",
                    "dbsnp" -> Map("version" -> Map("vcf_uri" -> "uri.vcf",
                                                    "contig_map" -> Map("1" -> "chr1"))))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testSingleUnzippedDbsnp(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map("s1" -> Map(
      "g1" -> Map("fasta_uri" -> "uri", "dbsnp" -> Map("version" -> Map("vcf_uri" -> "uri.vcf")))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testMultiDbsnp(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map(
        "g1" -> Map(
          "fasta_uri" -> "uri",
          "dbsnp" -> Map("version" -> Map("vcf_uri" -> List("uri.vcf.gz", "uri2.vcf.gz"))))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testVep(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map(
        "g1" -> Map(
          "fasta_uri" -> "uri",
          "vep" -> Map("version" -> Map("cache_uri" -> "something/human_vep_80_hg19.tar.gz")))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testGtfZipped(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map(
        "g1" -> Map("fasta_uri" -> "uri",
                    "gene_annotation" -> Map("version" -> Map("gtf_uri" -> "bla.gf.gz")))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testGtf(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map("g1" -> Map("fasta_uri" -> "uri",
                              "gene_annotation" -> Map("version" -> Map("gtf_uri" -> "bla.gf")))))
    noException should be thrownBy pipeline.script()
  }

  @Test
  def testGff(): Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Map("download_annotations" -> true), outputDir)
    pipeline.referenceConfig = Map(
      "s1" -> Map("g1" -> Map("fasta_uri" -> "uri",
                              "gene_annotation" -> Map("version" -> Map("gff_uri" -> "bla.gf")))))
    noException should be thrownBy pipeline.script()
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir(): Unit = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

object DownloadGenomesTest {
  def config(outputDir: File) = Map(
    "skip_write_dependencies" -> true,
    "output_dir" -> outputDir,
    "bwa" -> Map("exe" -> "test"),
    "star" -> Map("exe" -> "test"),
    "hisat2build" -> Map("exe" -> "test"),
    "bowtiebuild" -> Map("exe" -> "test"),
    "bowtie2build" -> Map("exe" -> "test"),
    "gmapbuild" -> Map("exe" -> "test"),
    "samtools" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "gatk_jar" -> "test",
    "tabix" -> Map("exe" -> "test"),
    "gffread" -> Map("exe" -> "test")
  )
}
