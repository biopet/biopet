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
/**
 * Created by wyleung on 11-2-16.
 */

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
package nl.lumc.sasc.biopet.pipelines.tinycap

import java.io.File

import com.google.common.io.Files
import nl.lumc.sasc.biopet.extensions.HtseqCount
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Config
import org.apache.commons.io.FileUtils
import org.broadinstitute.gatk.queue.QSettings
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.{ AfterClass, DataProvider, Test }

class TinyCapTest extends TestNGSuite with Matchers {

  def initPipeline(map: Map[String, Any]): TinyCap = {
    new TinyCap() {
      override def configNamespace = "tinycap"

      override def globalConfig = new Config(map)

      qSettings = new QSettings
      qSettings.runName = "test"
    }
  }

  @DataProvider(name = "tinyCapOptions")
  def tinyCapOptions = {
    val bool = Array(true)

    for (
      s1 <- bool
    ) yield Array("", s1)
  }

  @Test(dataProvider = "tinyCapOptions")
  def testTinyCap(dummy: String, sample1: Boolean): Unit = {
    val map = {
      var m: Map[String, Any] = TinyCapTest.config
      if (sample1) m = ConfigUtils.mergeMaps(TinyCapTest.sample1, m)
      m
    }

    if (!sample1) { // When no samples
      intercept[IllegalArgumentException] {
        initPipeline(map).script()
      }
    }

    val pipeline = initPipeline(map)
    pipeline.script()
    // expect 2 instances of HtSeqCount, one for mirna.gff other for transcripts.gtf
    pipeline.functions.count(_.isInstanceOf[HtseqCount]) shouldBe 2

  }

  // remove temporary run directory all tests in the class have been run
  @AfterClass def removeTempOutputDir() = {
    FileUtils.deleteDirectory(TinyCapTest.outputDir)
  }

}

object TinyCapTest {
  val outputDir = Files.createTempDir()
  new File(outputDir, "input").mkdirs()

  val r1 = new File(outputDir, "input" + File.separator + "R1.fq.gz")
  Files.touch(r1)
  val bam = new File(outputDir, "input" + File.separator + "bamfile.bam")
  Files.touch(bam)

  val referenceFasta = new File(outputDir, "ref.fa")
  Files.touch(referenceFasta)
  val referenceFastaDict = new File(outputDir, "ref.dict")
  Files.touch(referenceFastaDict)
  val bowtieIndex = new File(outputDir, "ref.1.ebwt")
  Files.touch(bowtieIndex)

  val annotationGFF = new File(outputDir, "annot.gff")
  val annotationGTF = new File(outputDir, "annot.gtf")
  val annotationRefflat = new File(outputDir, "annot.refflat")
  Files.touch(annotationGFF)
  Files.touch(annotationGTF)
  Files.touch(annotationRefflat)

  val config = Map(
    "output_dir" -> outputDir,
    "reference_fasta" -> (referenceFasta.getAbsolutePath),
    "bowtie_index" -> (bowtieIndex.getAbsolutePath),

    "annotation_gff" -> annotationGFF,
    "annotation_gtf" -> annotationGTF,
    "annotation_refflat" -> annotationRefflat,

    "md5sum" -> Map("exe" -> "test"),
    "fastqc" -> Map("exe" -> "test"),
    "seqtk" -> Map("exe" -> "test"),
    "sickle" -> Map("exe" -> "test"),
    "cutadapt" -> Map("exe" -> "test"),
    "bowtie" -> Map("exe" -> "test"),
    "htseqcount" -> Map("exe" -> "test"),
    "igvtools" -> Map("exe" -> "test"),
    "wigtobigwig" -> Map("exe" -> "test")
  )

  val sample1 = Map(
    "samples" -> Map("sample1" -> Map("libraries" -> Map(
      "lib1" -> Map(
        "R1" -> r1.getAbsolutePath
      )
    )
    )))

}