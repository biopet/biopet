package nl.lumc.sasc.biopet.pipelines.gwastest

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Files
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvan_thof on 4/11/16.
  */
class GwasTestTest extends TestNGSuite with Matchers {
  def initPipeline(map: Map[String, Any]): GwasTest = {
    new GwasTest {
      override def configNamespace = "gwastest"
      override def globalConfig = new Config(map)
      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @Test
  def testFromVcf: Unit = {
    val pipeline = initPipeline(GwasTestTest.config ++
      Map("input_vcf" -> GwasTestTest.vcfFile.toString
      )
    )
    pipeline.script()
  }

  @Test
  def testFromGens: Unit = {
    val pipeline = initPipeline(GwasTestTest.config ++
      Map("input_gens" -> List(Map("genotypes" -> GwasTestTest.vcfFile, "contig" -> "chrQ"))
      )
    )
    pipeline.script()
  }

  @Test
  def testWrongContig: Unit = {
    val pipeline = initPipeline(GwasTestTest.config ++
      Map("input_gens" -> List(Map("genotypes" -> GwasTestTest.vcfFile, "contig" -> "chrBla"))
      )
    )
    intercept[IllegalStateException] {
      pipeline.script()
    }
  }

  @Test
  def testFromSpecs: Unit = {
    val pipeline = initPipeline(GwasTestTest.config ++
      Map("imute_specs_file" -> GwasTestTest.resourcePath("/specs/files.specs"))
    )
    pipeline.script()
  }


  @Test
  def testEmpty: Unit = {
    val pipeline = initPipeline(GwasTestTest.config)
    intercept[IllegalArgumentException] {
      pipeline.script()
    }
  }
}

object GwasTestTest {
  val vcfFile = File.createTempFile("gwas.", ".vcf")
  Files.touch(vcfFile)
  vcfFile.deleteOnExit()

  val phenotypeFile = File.createTempFile("gwas.", ".txt")

  val outputDir = Files.createTempDir()
  outputDir.deleteOnExit()

  val reference = new File(resourcePath("/fake_chrQ.fa"))

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  val config = Map(
    "reference_fasta" -> GwasTestTest.reference.toString,
    "phenotype_file" -> GwasTestTest.phenotypeFile.toString,
    "output_dir" -> outputDir,
    "snptest" -> Map("exe" -> "test"),
    "md5sum" -> Map("exe" -> "test"),
    "gatk_jar" -> "test"
  )

}
