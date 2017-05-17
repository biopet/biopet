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
package nl.lumc.sasc.biopet.tools

import java.io.{BufferedWriter, File}
import java.nio.file.Paths

import org.mockito.Mockito.{inOrder => inOrd, when}
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.BufferedSource

class MergeTablesTest extends TestNGSuite with MockitoSugar with Matchers {

  import MergeTables._

  private def resourceFile(p: String): File =
    new File(resourcePath(p))

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource("/" + p).toURI).toString

  @Test def testProcessLine() = {
    val line = "a\tb\tc"
    processLine(line, Seq(0), 2) shouldBe ("a", "c")
  }

  @Test def testProcessLineValIdxTooLarge() = {
    val line = "a\tb\tc"
    val thrown = intercept[IllegalArgumentException] {
      processLine(line, Seq(0), 3)
    }
    thrown.getMessage shouldBe "requirement failed: Value index must be smaller than number of columns"
  }

  @Test def testProcessLineIdIdcesTooLarge() = {
    val line = "a\tb\tc"
    val thrown = intercept[IllegalArgumentException] {
      processLine(line, Seq(0, 3), 1)
    }
    thrown.getMessage shouldBe "requirement failed: All feature ID indices must be smaller than number of columns"
  }

  @Test def testProcessLineMultipleIdIdces() = {
    val line = "a\tb\tc\td\te"
    processLine(line, Seq(0, 2), 4) shouldBe ("a,c", "e")
  }

  @Test def testProcessLineCustomDelimiter() = {
    val line = "a|b|c"
    processLine(line, Seq(0), 2, delimiter = '|') shouldBe ("a", "c")
  }

  @Test def testProcessLineCustomIdSeparator() = {
    val line = "a\tb\tc\td\te"
    processLine(line, Seq(0, 1), 3, idSeparator = "_") shouldBe ("a_b", "d")
  }

  @Test def testProcessLineAdjacentDelimiters() = {
    val line = "a\t\tb\tc"
    processLine(line, Seq(0), 2) shouldBe ("a", "c")
  }

  @Test def testMergeTables() = {
    val in1 = InputTable("file1", mock[BufferedSource])
    val in2 = InputTable("file2", mock[BufferedSource])

    when(in1.source.getLines()) thenReturn Iterator("feature\trandom\tvalue", "a\tb\t1")
    when(in2.source.getLines()) thenReturn Iterator("feature\trandom\tvalue",
                                                    "a\tb\t100",
                                                    "x\ty\t9")

    val result = mergeTables(Seq(in1, in2), Seq(0), 2, 1)
    result.keySet shouldBe Set("file1", "file2")
    result("file1") shouldBe Map("a" -> "1")
    result("file2") shouldBe Map("a" -> "100", "x" -> "9")
  }

  @Test def testWriteOutput() = {
    val map =
      Map("sample1" -> Map("a" -> "1", "x" -> "900"), "sample2" -> Map("a" -> "100", "x" -> "9"))
    val out = mock[BufferedWriter]
    val obs = inOrd(out)

    writeOutput(map, out, "-", "feature")
    obs.verify(out).write("feature\tsample1\tsample2\n")
    obs.verify(out).write("a\t1\t100\n")
    obs.verify(out).write("x\t900\t9\n")
    obs.verify(out).flush()
  }

  @Test def testWriteOutputHasMissingValue() = {
    val map = Map("sample1" -> Map("a" -> "1"), "sample2" -> Map("a" -> "100", "x" -> "9"))
    val out = mock[BufferedWriter]
    val obs = inOrd(out)

    writeOutput(map, out, "-", "feature")
    obs.verify(out).write("feature\tsample1\tsample2\n")
    obs.verify(out).write("a\t1\t100\n")
    obs.verify(out).write("x\t-\t9\n")
    obs.verify(out).flush()
  }

  @Test def testPrepInputCustomExtension() = {
    // file content doesn't matter
    val inFiles = Seq(resourceFile("paired01.sam"), resourceFile("paired02.sam"))
    prepInput(inFiles, ".sam", None).map(_.name) shouldBe Seq("paired01", "paired02")
  }

  @Test def testPrepInputDuplicate() = {
    val inFiles = Seq(new File("README.txt"), new File("README.txt"))
    val thrown = intercept[IllegalArgumentException] {
      prepInput(inFiles, "", None)
    }
    thrown.getMessage shouldBe "requirement failed: Duplicate samples exist in inputs"
  }

  @Test def testArgsMinimum() = {
    val args = Array("-i",
                     "1,7",
                     "-a",
                     "10",
                     "-o",
                     "-",
                     // file content doesn't matter ~ they just need to exist
                     resourcePath("README.txt"),
                     resourcePath("README.txt"))
    val parsed = parseArgs(args)
    // remember we are doing the 1-based to 0-based coordinate conversion in the argument parser
    parsed.idColumnIndices shouldBe Seq(0, 6)
    parsed.valueColumnIndex shouldBe 9
    parsed.out shouldBe new File("-")
    parsed.inputTables shouldBe Seq(resourceFile("README.txt"), resourceFile("README.txt"))
    // default arguments
    parsed.fallbackString shouldBe "-"
    parsed.fileExtension shouldBe ""
    parsed.numHeaderLines shouldBe 0
    parsed.delimiter shouldBe '\t'
  }
}
