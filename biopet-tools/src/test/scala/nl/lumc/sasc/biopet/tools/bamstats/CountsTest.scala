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
package nl.lumc.sasc.biopet.tools.bamstats

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
 * Created by pjvan_thof on 19-7-16.
 */
class CountsTest extends TestNGSuite with Matchers {
  @Test
  def testValues(): Unit = {
    val data: Map[String, Long] = Map("1" -> 1, "2" -> 2, "3" -> 3)
    val c1 = new Counts[String](data)
    c1.countsMap shouldBe data
    c1.get("1") shouldBe Some(1)
    c1.get("2") shouldBe Some(2)
    c1.get("3") shouldBe Some(3)
    c1.get("4") shouldBe None

    c1.add("1")
    c1.get("1") shouldBe Some(2)
    c1.add("4")
    c1.get("4") shouldBe Some(1)

    val c2 = new Counts[String](data)
    c1 += c2

    c1.get("1") shouldBe Some(3)
    c1.get("2") shouldBe Some(4)
    c1.get("3") shouldBe Some(6)
    c1.get("4") shouldBe Some(1)
  }

  @Test
  def testEmpty(): Unit = {
    val c1 = new Counts[Int]()
    c1.countsMap.isEmpty shouldBe true
  }

  @Test
  def testEqual(): Unit = {
    val c1 = new Counts[Int]()
    val c2 = new Counts[Int]()

    c1 should not be "be a string"

    c1 shouldBe c1
    c2 shouldBe c2
    c1 shouldBe c2

    c1.add(1)
    c1 shouldBe c1
    c2 shouldBe c2
    c1 should not be c2

    c2.add(1)
    c1 shouldBe c1
    c2 shouldBe c2
    c1 shouldBe c2
  }

  @Test
  def testTsv(): Unit = {
    val data: Map[Int, Long] = Map(1 -> 1, 2 -> 2, 3 -> 3)
    val c1 = new Counts[Int](data)

    val tsvFile = File.createTempFile("counts.", ".tsv")
    tsvFile.deleteOnExit()

    c1.writeToTsv(tsvFile)

    val reader = Source.fromFile(tsvFile)
    reader.getLines().toList shouldBe List("value\tcount", "1\t1", "2\t2", "3\t3")
    reader.close()
  }
}
