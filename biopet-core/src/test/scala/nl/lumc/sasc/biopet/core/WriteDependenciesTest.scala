package nl.lumc.sasc.biopet.core

import java.io.File
import java.nio.file.Files

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
  def test: Unit = {
    val outputFile = File.createTempFile("deps.", ".json")
    outputFile.deleteOnExit()
    WriteDependencies.writeDependencies(Qfunc(file1 :: Nil, file2 :: Nil) :: Qfunc(file2 :: Nil, file3 :: Nil) :: Nil, outputFile)
    println(Source.fromFile(outputFile).getLines().mkString("\n"))
  }
}

object WriteDependenciesTest {
  val tempDir = Files.createTempDirectory("test").toFile
  tempDir.deleteOnExit()
  val file1 = new File(tempDir, "file1.txt")
  val file2 = new File(tempDir, "file2.txt")
  val file3 = new File(tempDir, "file3.txt")
}