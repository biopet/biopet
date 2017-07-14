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

import nl.lumc.sasc.biopet.extensions.manwe._
import nl.lumc.sasc.biopet.utils.config.Config
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by ahbbollen on 24-9-15.
  */
class ManweTest extends TestNGSuite with Matchers {

  @Test
  def testManweAnnotatedBed(): Unit = {
    val manwe = new ManweAnnotateBed(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    val bed = File.createTempFile("manwe", "bed")
    out.deleteOnExit()
    bed.deleteOnExit()

    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.bed = bed
    manwe.alreadyUploaded = false
    manwe.cmd should equal(
      s"manwe annotate-bed ${bed.getAbsolutePath} -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.queries = List("/uri/1&&/uri/2")
    manwe.cmd should equal(
      s"manwe annotate-bed ${bed.getAbsolutePath} -q /uri/1&&/uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(
      s"manwe annotate-bed ${bed.getAbsolutePath} -u -q /uri/1&&/uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.waitToComplete = true
    manwe.cmd should equal(
      s"manwe annotate-bed ${bed.getAbsolutePath} -u -q /uri/1&&/uri/2 --wait -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweAnnotateVcf(): Unit = {
    val manwe = new ManweAnnotateVcf(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    val vcf = File.createTempFile("manwe", "vcf")
    out.deleteOnExit()
    vcf.deleteOnExit()

    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.vcf = vcf
    manwe.alreadyUploaded = false
    manwe.cmd should equal(
      s"manwe annotate-vcf ${vcf.getAbsolutePath} -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.queries = List("/uri/1&&/uri/2")
    manwe.cmd should equal(
      s"manwe annotate-vcf ${vcf.getAbsolutePath} -q /uri/1&&/uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(
      s"manwe annotate-vcf ${vcf.getAbsolutePath} -u -q /uri/1&&/uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.waitToComplete = true
    manwe.cmd should equal(
      s"manwe annotate-vcf ${vcf.getAbsolutePath} -u -q /uri/1&&/uri/2 --wait -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesAnnotate(): Unit = {
    val manwe = new ManweDataSourcesAnnotate(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()

    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(
      s"manwe data-sources annotate /uri/1 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.queries = List("/uri/1&&/uri/2")
    manwe.cmd should equal(
      s"manwe data-sources annotate /uri/1 -q /uri/1&&/uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.waitToComplete = true
    manwe.cmd should equal(
      s"manwe data-sources annotate /uri/1 -q /uri/1&&/uri/2 --wait -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesDownload(): Unit = {
    val manwe = new ManweDataSourcesDownload(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile

    manwe.output = out
    manwe.uri = "/uri/1"
    manwe.cmd should equal(
      s"manwe data-sources download /uri/1 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesList(): Unit = {
    val manwe = new ManweDataSourcesList(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.cmd should equal(
      s"manwe data-sources list -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesShow(): Unit = {
    val manwe = new ManweDataSourcesShow(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(
      s"manwe data-sources show /uri/1 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesActivate(): Unit = {
    val manwe = new ManweSamplesActivate(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.uri = "/uri/1"
    manwe.cmd should equal(
      s"manwe samples activate /uri/1 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesAdd(): Unit = {
    val manwe = new ManweSamplesAdd(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.name = Some("pietje")
    manwe.cmd should equal(
      s"manwe samples add pietje -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.group = List("/uri/1", "/uri/2")
    manwe.cmd should equal(
      s"manwe samples add pietje -g /uri/1 -g /uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.poolSize = Some(3)
    manwe.cmd should equal(
      s"manwe samples add pietje -s 3 -g /uri/1 -g /uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesAnnotateVariations(): Unit = {
    val manwe = new ManweSamplesAnnotateVariations(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(
      s"manwe samples annotate-variations /uri/1 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.queries = List("/uri/1&&/uri/2", "/uri/3")
    manwe.cmd should equal(
      s"manwe samples annotate-variations /uri/1 -q /uri/1&&/uri/2 -q /uri/3 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesImport(): Unit = {
    val manwe = new ManweSamplesImport(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.name = Some("pietje")
    manwe.cmd should equal(
      s"manwe samples import pietje -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.group = List("/uri/1&&/uri/2", "/uri/3")
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    val vcfs: List[File] = (0 until 4).map(_ => File.createTempFile("manwe", "test")).toList
    val beds: List[File] = (0 until 4).map(_ => File.createTempFile("manwe", "test")).toList
    vcfs.foreach(x => x.deleteOnExit())
    beds.foreach(x => x.deleteOnExit())
    manwe.vcfs = vcfs
    manwe.beds = beds

    val vcfLine = vcfs.foldLeft("")((r, f) => r + s"--vcf ${f.getAbsolutePath} ").trim
    val bedLine = beds.foldLeft("")((r, f) => r + s"--bed ${f.getAbsolutePath} ").trim

    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.poolSize = Some(4)
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -s 4 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -s 4 -u -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.public = true
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -s 4 -u -p -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.preferLikelihood = true
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -s 4 -u -p -l -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.noCoverage = true
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -s 4 -u -p -l --no-coverage-profile -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.waitToComplete = true
    manwe.cmd should equal(
      s"manwe samples import pietje -g /uri/1&&/uri/2 -g /uri/3 $vcfLine $bedLine -s 4 -u -p -l --no-coverage-profile --wait -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesImportBed(): Unit = {

    val manwe = new ManweSamplesImportBed(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out

    val bed = File.createTempFile("manwe", "test")
    bed.deleteOnExit()
    manwe.bed = bed

    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(
      s"manwe samples import-bed /uri/1 ${bed.getAbsolutePath} -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(
      s"manwe samples import-bed /uri/1 ${bed.getAbsolutePath} -u -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.waitToComplete = true
    manwe.cmd should equal(
      s"manwe samples import-bed /uri/1 ${bed.getAbsolutePath} -u --wait -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesImportVcf(): Unit = {
    val manwe = new ManweSamplesImportVcf(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out

    val vcf = File.createTempFile("manwe", "test")
    vcf.deleteOnExit()
    manwe.vcf = vcf

    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(
      s"manwe samples import-vcf /uri/1 ${vcf.getAbsolutePath} -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(
      s"manwe samples import-vcf /uri/1 ${vcf.getAbsolutePath} -u -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.preferLikelihoods = true
    manwe.cmd should equal(
      s"manwe samples import-vcf /uri/1 ${vcf.getAbsolutePath} -u -l -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.waitToComplete = true
    manwe.cmd should equal(
      s"manwe samples import-vcf /uri/1 ${vcf.getAbsolutePath} -u -l --wait -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesList(): Unit = {
    val manwe = new ManweSamplesList(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out

    manwe.cmd should equal(
      s"manwe samples list -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.group = List("/uri/1", "/uri/2")
    manwe.cmd should equal(
      s"manwe samples list -g /uri/1 -g /uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.user = Some("/uri/3")
    manwe.cmd should equal(
      s"manwe samples list -u /uri/3 -g /uri/1 -g /uri/2 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")

    manwe.onlyPublic = true
    manwe.cmd should equal(
      s"manwe samples list -u /uri/3 -g /uri/1 -g /uri/2 -p -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test
  def testManweSamplesShow(): Unit = {
    val manwe = new ManweSamplesShow(null) {
      override def globalConfig =
        new Config(Map("manwe_config" -> "${manwe.manweConfig.getAbsolutePath}"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()
    manwe.manweConfig = out.getParentFile
    manwe.output = out
    manwe.uri = Some("/uri/1")

    manwe.cmd should equal(
      s"manwe samples show /uri/1 -c ${manwe.manweConfig.getAbsolutePath} > ${out.getAbsolutePath}")
  }

  @Test def testConfigCreation(): Unit = {
    val manwe = new ManweAnnotateBed(null) {
      override def globalConfig =
        new Config(
          Map(
            "varda_root" -> "http://127.0.0.1:5000",
            "varda_token" -> "QWERTYUIOPASDFGHJKLZXCVBNM",
            "varda_cache_size" -> 25,
            "varda_buffer_size" -> 200,
            "varda_task_poll_wait" -> 5,
            "varda_verify_certificate" -> true
          ))
    }

    val file: File = manwe.createManweConfig(None)
    val contents = Source.fromFile(file).getLines().toList

    val supposedContent = List(
      "API_ROOT = 'http://127.0.0.1:5000'",
      "TOKEN = 'QWERTYUIOPASDFGHJKLZXCVBNM'",
      "VERIFY_CERTIFICATE = True",
      "COLLECTION_CACHE_SIZE = 25",
      "DATA_BUFFER_SIZE = 200",
      "TASK_POLL_WAIT = 5"
    )

    supposedContent.sorted should equal(contents.sorted)

    val manwe2 = new ManweAnnotateBed(null) {
      override def globalConfig =
        new Config(
          Map(
            "varda_root" -> "http://127.0.0.1:5000",
            "varda_token" -> "QWERTYUIOPASDFGHJKLZXCVBNM",
            "varda_cache_size" -> 25,
            "varda_buffer_size" -> 200,
            "varda_task_poll_wait" -> 5,
            "varda_verify_certificate" -> "/a/b/c/d.crt"
          ))
    }

    val file2: File = manwe2.createManweConfig(None)
    val contents2 = Source.fromFile(file2).getLines().toList
    val supposedContent2 = List(
      "API_ROOT = 'http://127.0.0.1:5000'",
      "TOKEN = 'QWERTYUIOPASDFGHJKLZXCVBNM'",
      "VERIFY_CERTIFICATE = '/a/b/c/d.crt'",
      "COLLECTION_CACHE_SIZE = 25",
      "DATA_BUFFER_SIZE = 200",
      "TASK_POLL_WAIT = 5"
    )

    supposedContent2.sorted should equal(contents2.sorted)
  }

}
