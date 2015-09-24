package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.extensions.manwe._
import nl.lumc.sasc.biopet.utils.config.Config
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweTest extends TestNGSuite with Matchers {

  @Test
  def testManweAnnotatedBed = {
    val manwe = new ManweAnnotateBed(null) {
      override def globalConfig = new Config(Map("manwe_config" -> "/usr/local/nonexistent.conf"))
    }

    val out = File.createTempFile("manwe", "test")
    val bed = File.createTempFile("manwe", "bed")
    out.deleteOnExit()
    bed.deleteOnExit()

    manwe.output = out
    manwe.bed = bed
    manwe.alreadyUploaded = false
    manwe.queries = List("/uri/1&&/uri/2")
    manwe.cmd should equal(s"manwe annotate-bed ${bed.getAbsolutePath} -q /uri/1&&/uri/2 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(s"manwe annotate-bed ${bed.getAbsolutePath} -u -q /uri/1&&/uri/2 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")
  }

  @Test
  def testManweAnnotateVcf = {
    val manwe = new ManweAnnotateVcf(null) {
      override def globalConfig = new Config(Map("manwe_config" -> "/usr/local/nonexistent.conf"))
    }

    val out = File.createTempFile("manwe", "test")
    val vcf = File.createTempFile("manwe", "vcf")
    out.deleteOnExit()
    vcf.deleteOnExit()

    manwe.output = out
    manwe.vcf = vcf
    manwe.alreadyUploaded = false
    manwe.queries = List("/uri/1&&/uri/2")
    manwe.cmd should equal(s"manwe annotate-vcf ${vcf.getAbsolutePath} -q /uri/1&&/uri/2 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")

    manwe.alreadyUploaded = true
    manwe.cmd should equal(s"manwe annotate-vcf ${vcf.getAbsolutePath} -u -q /uri/1&&/uri/2 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesAnnotate = {
    val manwe = new ManweDataSourcesAnnotate(null) {
      override def globalConfig = new Config(Map("manwe_config" -> "/usr/local/nonexistent.conf"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()

    manwe.output = out
    manwe.uri = Some("/uri/1")
    manwe.queries = List("/uri/1&&/uri/2")
    manwe.cmd should equal(s"manwe data-sources annotate /uri/1 -q /uri/1&&/uri/2 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesDownload = {
    val manwe = new ManweDataSourcesDownload(null) {
      override def globalConfig = new Config(Map("manwe_config" -> "/usr/local/nonexistent.conf"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()

    manwe.output = out
    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(s"manwe data-sources download /uri/1 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesList = {
    val manwe = new ManweDataSourcesList(null) {
      override def globalConfig = new Config(Map("manwe_config" -> "/usr/local/nonexistent.conf"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()

    manwe.output = out
    manwe.cmd should equal(s"manwe data-sources list -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")
  }

  @Test
  def testManweDataSourcesShow = {
    val manwe = new ManweDataSourcesShow(null) {
      override def globalConfig = new Config(Map("manwe_config" -> "/usr/local/nonexistent.conf"))
    }

    val out = File.createTempFile("manwe", "test")
    out.deleteOnExit()

    manwe.output = out
    manwe.uri = Some("/uri/1")
    manwe.cmd should equal(s"manwe data-sources show /uri/1 -c /usr/local/nonexistent.conf > ${out.getAbsolutePath}")
  }

}
