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
package nl.lumc.sasc.biopet.core.summary

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.core.extensions.Md5sum
import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}
import org.broadinstitute.gatk.queue.QScript
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import SummaryQScriptTest._

/**
  * Created by pjvanthof on 14/01/16.
  */
class SummaryQScriptTest extends TestNGSuite with Matchers {
  @Test
  def testNoJobs(): Unit = {
    SummaryQScript.md5sumCache.clear()
    val script = makeQscript()
    script.addSummaryJobs()
    SummaryQScript.md5sumCache shouldBe empty
  }

  @Test
  def testFiles(): Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript(files = Map("file" -> file))
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 2
    assert(
      script.functions
        .filter(_.isInstanceOf[Md5sum])
        .map(_.asInstanceOf[Md5sum])
        .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testDuplicateFiles(): Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript(files = Map("file" -> file, "file2" -> file))
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 2
    assert(
      script.functions
        .filter(_.isInstanceOf[Md5sum])
        .map(_.asInstanceOf[Md5sum])
        .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testAddSummarizable(): Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript()
    script.addSummarizable(makeSummarizable(files = Map("file" -> file, "file2" -> file)), "test")
    script.summarizables.size shouldBe 1
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 2
    assert(
      script.functions
        .filter(_.isInstanceOf[Md5sum])
        .map(_.asInstanceOf[Md5sum])
        .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testInputFile(): Unit = {
    SummaryQScript.md5sumCache.clear()
    val file = new File(s".${File.separator}bla")
    val script = makeQscript()
    script.addSummarizable(makeSummarizable(files = Map("file" -> file, "file2" -> file)), "test")
    script.summarizables.size shouldBe 1
    script.inputFiles :+= InputFile(file, Some("md5sum"))
    script.inputFiles :+= InputFile(file, None)
    script.addSummaryJobs()
    SummaryQScript.md5sumCache should not be empty
    SummaryQScript.md5sumCache.toMap shouldBe Map(
      new File(s".${File.separator}bla") -> new File(s".${File.separator}bla.md5"))
    script.functions.size shouldBe 3
    assert(
      script.functions
        .filter(_.isInstanceOf[Md5sum])
        .map(_.asInstanceOf[Md5sum])
        .exists(_.cmdLine.contains(" || ")))
  }

  @Test
  def testAddQscript(): Unit = {
    SummaryQScript.md5sumCache.clear()
    val script = makeQscript()
    script.addSummaryQScript(script)
    script.summaryQScripts.head shouldBe script
  }
}

object SummaryQScriptTest {
  def makeQscript(settings: Map[String, Any] = Map(),
                  files: Map[String, File] = Map(),
                  c: Map[String, Any] = Map()) =
    new SummaryQScript with QScript {
      outputDir = new File(".")
      override def globalConfig = new Config(c)
      def summarySettings: Map[String, Any] = settings
      def summaryFiles: Map[String, File] = files
      val tempFile: File = File.createTempFile("summary", ".json")
      tempFile.deleteOnExit()
      def summaryFile: File = tempFile
      def init(): Unit = ???
      def biopetScript(): Unit = ???
      def parent: Configurable = null
    }

  def makeSummarizable(files: Map[String, File] = Map(), stats: Map[String, Any] = Map()) =
    new Summarizable {
      def summaryFiles: Map[String, File] = files
      def summaryStats: Any = stats
    }
}
