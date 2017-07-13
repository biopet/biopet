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
package nl.lumc.sasc.biopet.utils

import java.io.{File, FileNotFoundException, PrintWriter}
import java.nio.file.Files

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvanthof on 05/05/16.
  */
class IoUtilsTest extends TestNGSuite with Matchers {

  def createTempTestFile(file: File): Unit = {
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(file)
    writer.println("test")
    writer.close()
    file.deleteOnExit()
  }

  @Test
  def testCopyFile(): Unit = {
    val temp1 = File.createTempFile("test.", ".txt")
    temp1.deleteOnExit()
    val temp2 = File.createTempFile("test.", ".txt")
    temp2.deleteOnExit()
    createTempTestFile(temp1)
    IoUtils.copyFile(temp1, temp2)
    val reader = Source.fromFile(temp2)
    reader.getLines().toList shouldBe List("test")
    reader.close()
  }

  @Test
  def testCopyFileNonExistingDir(): Unit = {
    val temp1 = File.createTempFile("test.", ".txt")
    val tempDir = new File(Files.createTempDirectory("test").toFile, "non-exist")
    tempDir.deleteOnExit()
    tempDir shouldNot exist
    val temp2 = new File(tempDir, "test.txt")
    createTempTestFile(temp1)
    intercept[FileNotFoundException] {
      IoUtils.copyFile(temp1, temp2)
    }
    IoUtils.copyFile(temp1, temp2, createDirs = true)
    val reader = Source.fromFile(temp2)
    reader.getLines().toList shouldBe List("test")
    reader.close()
  }

  @Test
  def testCopyDir(): Unit = {
    val tempDir1 = Files.createTempDirectory("test").toFile
    tempDir1.deleteOnExit()
    val tempDir2 = Files.createTempDirectory("test").toFile
    tempDir2.deleteOnExit()
    val relativePaths: List[String] = List(
      "test1.txt",
      "test2.txt",
      "dir1" + File.separator + "test1.txt",
      "dir1" + File.separator + "test2.txt",
      "dir2" + File.separator + "test1.txt",
      "dir2" + File.separator + "test2.txt"
    )
    relativePaths.foreach { x =>
      createTempTestFile(new File(tempDir1, x))
      new File(tempDir2, x) shouldNot exist
    }
    IoUtils.copyDir(tempDir1, tempDir2)
    relativePaths.foreach { x =>
      val file = new File(tempDir2, x)
      file should exist
      val reader = Source.fromFile(file)
      reader.getLines().toList shouldBe List("test")
      reader.close()
    }
  }
}
