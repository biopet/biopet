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
package nl.lumc.sasc.biopet.pipelines.gwastest.impute

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{AfterClass, Test}

/**
  * Created by pjvan_thof on 31-5-16.
  */
class Impute2VcfTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): Impute2Vcf = {
    new Impute2Vcf {
      override def configNamespace = "impute2vcf"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  private var dirs: List[File] = Nil

  @Test
  def testFromGens: Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(
      Impute2VcfTest.config(outputDir) ++
        Map("input_gens" -> List(Map("genotypes" -> Impute2VcfTest.vcfFile, "contig" -> "chrQ"))))
    pipeline.script()
  }

  @Test
  def testWrongContig: Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Impute2VcfTest.config(outputDir) ++
      Map("input_gens" -> List(Map("genotypes" -> Impute2VcfTest.vcfFile, "contig" -> "chrBla"))))
    intercept[IllegalStateException] {
      pipeline.script()
    }
  }

  @Test
  def testFromSpecs: Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(
      Impute2VcfTest.config(outputDir) ++
        Map("imute_specs_file" -> Impute2VcfTest.resourcePath("/specs/files.specs")))
    pipeline.script()
  }

  @Test
  def testEmpty: Unit = {
    val outputDir = Files.createTempDir()
    dirs :+= outputDir
    val pipeline = initPipeline(Impute2VcfTest.config(outputDir))
    intercept[IllegalArgumentException] {
      pipeline.script()
    }
  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    dirs.foreach(FileUtils.deleteDirectory)
  }
}

object Impute2VcfTest {
  val vcfFile = File.createTempFile("gwas.", ".vcf")
  Files.touch(vcfFile)
  vcfFile.deleteOnExit()

  val phenotypeFile = File.createTempFile("gwas.", ".txt")
  phenotypeFile.deleteOnExit()

  val reference = new File(resourcePath("/fake_chrQ.fa"))

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  def config(outputDir: File) = Map(
    "skip_write_dependencies" -> true,
    "reference_fasta" -> reference.toString,
    "phenotype_file" -> phenotypeFile.toString,
    "output_dir" -> outputDir,
    "md5sum" -> Map("exe" -> "test"),
    "gatk_jar" -> "test"
  )

}
