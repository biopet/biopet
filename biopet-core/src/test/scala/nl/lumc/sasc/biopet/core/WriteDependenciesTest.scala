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
package nl.lumc.sasc.biopet.core

import java.io.File
import java.nio.file.Files

import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.function.QFunction
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
 * Created by pjvanthof on 09/05/16.
 */
class WriteDependenciesTest extends TestNGSuite with Matchers {

  import WriteDependenciesTest._

  case class Qfunc(in: Seq[File], out: Seq[File]) extends QFunction {
    override def inputs = in
    override def outputs = out
    override def doneOutputs = out.map(x => new File(x.getParentFile, s".${x.getName}.done"))
    override def failOutputs = out.map(x => new File(x.getParentFile, s".${x.getName}.fail"))
    jobOutputFile = new File(out.head + ".out")
  }

  @Test
  def testDeps: Unit = {
    val outputFile = File.createTempFile("deps.", ".json")
    outputFile.deleteOnExit()
    val func1 = Qfunc(file1 :: Nil, file2 :: Nil)
    val func2 = Qfunc(file2 :: Nil, file3 :: Nil)
    WriteDependencies.writeDependencies(func1 :: func2 :: Nil, outputFile)
    val deps = ConfigUtils.fileToConfigMap(outputFile)
    deps("jobs") shouldBe a[Map[_, _]]
    val jobs = deps("jobs").asInstanceOf[Map[String, Map[String, Any]]]
    jobs.count(_._1.contains("Qfunc")) shouldBe 2

    deps("files") shouldBe a[List[_]]
    val files = deps("files").asInstanceOf[List[Map[String, Any]]]
    val paths = files.map(x => x.get("path")).flatten
    assert(paths.contains(file1.toString))
    assert(paths.contains(file2.toString))
    assert(paths.contains(file3.toString))

    files.find(_.get("path") == Some(file1.toString)).flatMap(_.get("pipeline_input")) shouldBe Some(true)
    files.find(_.get("path") == Some(file2.toString)).flatMap(_.get("pipeline_input")) shouldBe Some(false)
    files.find(_.get("path") == Some(file3.toString)).flatMap(_.get("pipeline_input")) shouldBe Some(false)
  }
}

object WriteDependenciesTest {
  val tempDir = Files.createTempDirectory("test").toFile
  tempDir.deleteOnExit()
  val file1 = new File(tempDir, "file1.txt")
  val file2 = new File(tempDir, "file2.txt")
  val file3 = new File(tempDir, "file3.txt")
}