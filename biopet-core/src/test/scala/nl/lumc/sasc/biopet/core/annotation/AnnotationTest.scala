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
package nl.lumc.sasc.biopet.core.annotation

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.core.annotations.{AnnotationGff, AnnotationGtf, AnnotationRefFlat}
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Config
import org.broadinstitute.gatk.queue.QScript
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 18/11/2016.
  */
class AnnotationTest extends TestNGSuite with Matchers {
  @Test
  def testAnnotationGtf: Unit = {
    val s1 = new AnnotationTest.Script(AnnotationTest.config) with AnnotationGtf
    s1.annotationGtf shouldBe new File("")
    an[IllegalStateException] shouldBe thrownBy(Logging.checkErrors())

    val s2 = new AnnotationTest.Script(
      AnnotationTest.config ++ Map("species" -> "s1", "reference_name" -> "g1")) with AnnotationGtf
    s2.annotationGtf shouldBe new File("no_set.gtf")
    noException should be thrownBy (Logging.checkErrors())

    val s3 = new AnnotationTest.Script(
      AnnotationTest.config ++ Map("species" -> "s1",
                                   "reference_name" -> "g1",
                                   "gene_annotation_name" -> "set1")) with AnnotationGtf
    s3.annotationGtf shouldBe new File("set1.gtf")
    noException should be thrownBy (Logging.checkErrors())
  }

  @Test
  def testAnnotationGff: Unit = {
    val s1 = new AnnotationTest.Script(AnnotationTest.config) with AnnotationGff
    s1.annotationGff shouldBe new File("")
    an[IllegalStateException] shouldBe thrownBy(Logging.checkErrors())

    val s2 = new AnnotationTest.Script(
      AnnotationTest.config ++ Map("species" -> "s1", "reference_name" -> "g1")) with AnnotationGff
    s2.annotationGff shouldBe new File("no_set.gff")
    noException should be thrownBy (Logging.checkErrors())

    val s3 = new AnnotationTest.Script(
      AnnotationTest.config ++ Map("species" -> "s1",
                                   "reference_name" -> "g1",
                                   "gene_annotation_name" -> "set1")) with AnnotationGff
    s3.annotationGff shouldBe new File("set1.gff")
    noException should be thrownBy (Logging.checkErrors())
  }

  @Test
  def testAnnotationRefFlat: Unit = {
    val s1 = new AnnotationTest.Script(AnnotationTest.config) with AnnotationRefFlat
    s1.annotationRefFlat.get shouldBe new File("")
    an[IllegalStateException] shouldBe thrownBy(Logging.checkErrors())

    val s2 = new AnnotationTest.Script(
      AnnotationTest.config ++ Map("species" -> "s1", "reference_name" -> "g1"))
    with AnnotationRefFlat
    s2.annotationRefFlat.get shouldBe new File("no_set.refFlat")
    noException should be thrownBy (Logging.checkErrors())

    val s3 = new AnnotationTest.Script(
      AnnotationTest.config ++ Map("species" -> "s1",
                                   "reference_name" -> "g1",
                                   "gene_annotation_name" -> "set1")) with AnnotationRefFlat
    s3.annotationRefFlat.get shouldBe new File("set1.refFlat")
    noException should be thrownBy (Logging.checkErrors())
  }
}

object AnnotationTest {
  class Script(c: Map[String, Any]) extends QScript with BiopetQScript {
    override def globalConfig: Config = new Config(c)
    val parent = null

    /** Init for pipeline */
    def init(): Unit = ???

    /** Pipeline itself */
    def biopetScript(): Unit = ???
  }

  val config: Map[String, Any] = Map(
    "references" -> Map("s1" -> Map("g1" -> Map(
      "annotation_gtf" -> "no_set.gtf",
      "annotation_gff" -> "no_set.gff",
      "annotation_refflat" -> "no_set.refFlat",
      "gene_annotations" -> Map("set1" -> Map(
        "annotation_gtf" -> "set1.gtf",
        "annotation_gff" -> "set1.gff",
        "annotation_refflat" -> "set1.refFlat"
      ))
    ))))
}
