package nl.lumc.sasc.biopet.pipelines.gwastest.impute

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

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

  @Test
  def testFromGens: Unit = {
    val pipeline = initPipeline(Impute2VcfTest.config ++
      Map("input_gens" -> List(Map("genotypes" -> Impute2VcfTest.vcfFile, "contig" -> "chrQ"))
      )
    )
    pipeline.script()
  }

  @Test
  def testWrongContig: Unit = {
    val pipeline = initPipeline(Impute2VcfTest.config ++
      Map("input_gens" -> List(Map("genotypes" -> Impute2VcfTest.vcfFile, "contig" -> "chrBla"))
      )
    )
    intercept[IllegalStateException] {
      pipeline.script()
    }
  }

  @Test
  def testFromSpecs: Unit = {
    val pipeline = initPipeline(Impute2VcfTest.config ++
      Map("imute_specs_file" -> Impute2VcfTest.resourcePath("/specs/files.specs"))
    )
    pipeline.script()
  }

  @Test
  def testEmpty: Unit = {
    val pipeline = initPipeline(Impute2VcfTest.config)
    intercept[IllegalArgumentException] {
      pipeline.script()
    }
  }

}

object Impute2VcfTest {
  val vcfFile = File.createTempFile("gwas.", ".vcf")
  Files.touch(vcfFile)
  vcfFile.deleteOnExit()

  val phenotypeFile = File.createTempFile("gwas.", ".txt")
  phenotypeFile.deleteOnExit()

  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()

  val reference = new File(resourcePath("/fake_chrQ.fa"))

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val config = Map(
    "reference_fasta" -> reference.toString,
    "phenotype_file" -> phenotypeFile.toString,
    "output_dir" -> outputDir,
    "md5sum" -> Map("exe" -> "test"),
    "gatk_jar" -> "test"
  )

}
