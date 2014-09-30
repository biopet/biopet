/**
 * Copyright (c) 2014 Leiden University Medical Center - Sequencing Analysis Support Core <sasc@lumc.nl>
 * @author Wibowo Arindrarto <w.arindrarto@lumc.nl>
 */
package nl.lumc.sasc.biopet.core.apps

import java.nio.file.Paths
import java.io.{ File, IOException }

import org.scalatest.Assertions
import org.testng.annotations.Test


class WipeReadsUnitTest extends Assertions {

  import WipeReads._

  private def resourcePath(p: String): String =
    Paths.get(getClass.getResource(p).toURI).toString

  val bam01 = resourcePath("/single.bam")
  val bed01 = resourcePath("/rrna01.bed")
  val minArgList = List("-I", bam01.toString, "-l", bed01.toString, "-o", "mock.bam")

  @Test def testOptMinimum() = {
    val opts = parseOption(Map(), minArgList)
    assert(opts.contains("inputBAM"))
    assert(opts.contains("targetRegions"))
    assert(opts.contains("outputBAM"))
  }

  @Test def testOptMissingBAI() = {
    val pathBAM = File.createTempFile("WipeReads", java.util.UUID.randomUUID.toString)
    assert(pathBAM.exists)
    val argList = List(
      "--inputBAM", pathBAM.toPath.toString,
      "--targetRegions", bed01.toString,
      "--outputBAM", "mock.bam")
    val thrown = intercept[IOException] {
      parseOption(Map(), argList)
    }
    assert(thrown.getMessage === "Index for input BAM file " + pathBAM + " not found")
    pathBAM.deleteOnExit()
  }

  @Test def testOptMissingRegions() = {
    val pathRegion = "/i/dont/exist.bed"
    val argList = List(
      "--inputBAM", bam01,
      "--targetRegions", pathRegion,
      "--outputBAM", "mock.bam"
    )
    val thrown = intercept[IOException] {
      parseOption(Map(), argList)
    }
    assert(thrown.getMessage === "Input file " + pathRegion + " not found")
  }

  @Test def testOptUnexpected() = {
    val argList = List("--strand", "sense") ::: minArgList
    val thrown = intercept[IllegalArgumentException] {
      parseOption(Map(), argList)
    }
    assert(thrown.getMessage === "Unexpected or duplicate option flag: --strand")
  }

  @Test def testOptMinOverlapFraction() = {
    val argList = List("--minOverlapFraction", "0.8") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("minOverlapFraction") == 0.8)
  }

  @Test def testOptMinMapQ() = {
    val argList = List("--minMapQ", "13") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("minMapQ") == 13)
  }

  @Test def testOptStrandPlus() = {
    val argList = List("--strand", "plus") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("strand") == Strand.Plus)
  }

  @Test def testOptStrandMinus() = {
    val argList = List("--strand", "minus") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("strand") == Strand.Minus)
  }

  @Test def testOptStrandIgnore() = {
    val argList = List("--strand", "ignore") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("strand") == Strand.Ignore)
  }

  @Test def testOptMakeIndex() = {
    val argList = List("--makeIndex") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("makeIndex") == true) // why can't we evaluate directly??
  }

  @Test def testOptLimitToRegion() = {
    val argList = List("--limitToRegion") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(opts("limitToRegion") == true)
  }

  @Test def testOptSingleReadGroup() = {
    val argList = List("--readGroup", "g1") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(Array("g1").sameElements(opts("readGroup")))
  }

  @Test def testOptMultipleReadGroup() = {
    val argList = List("--readGroup", "g1,g2") ::: minArgList
    val opts = parseOption(Map(), argList)
    assert(Array("g1", "g2").sameElements(opts("readGroup")))
  }
}

  /*
    TODO: missing whole argument
   */
