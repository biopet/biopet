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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class LnTest extends TestNGSuite with Matchers {

  @Test(description = "Target and link in the same directory, relative set to true")
  def testSameLevelRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/nested/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s target.txt /dir/nested/link.txt")
  }

  @Test(description = "Target is one level above link, relative set to true")
  def testTargetOneLevelAboveRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s ../target.txt /dir/nested/link.txt")
  }

  @Test(description = "Target is two levels above link, relative set to true")
  def testTargetTwoLevelsAboveRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s ../../target.txt /dir/nested/link.txt")
  }

  @Test(
    description = "Target is a child of a directory one level above link, relative set to true")
  def testTargetOneLevelAboveChildRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/another_nested/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s ../another_nested/target.txt /dir/nested/link.txt")
  }

  @Test(
    description = "Target is a child of a directory multi level above link, relative set to true")
  def testTargetMultiLevelAboveChildRelative1() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/another_nested/1/2/3/4/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s ../another_nested/1/2/3/4/target.txt /dir/nested/link.txt")
  }

  @Test(
    description = "Target is a child of a directory multi level above link, relative set to true")
  def testTargetMultiLevelAboveChildRelative2() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/another_nested/1/2/3/4/target.txt")
    ln.output = new File("/dir/nested/2/link.txt")
    ln.cmd should ===("ln -s ../../another_nested/1/2/3/4/target.txt /dir/nested/2/link.txt")
  }

  @Test(
    description = "Source is a child of a directory multi level above link, relative set to true")
  def testSourceMultiLevelAboveChildRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.output = new File("/dir/another_nested/1/2/3/4/link.txt")
    ln.input = new File("/dir/nested/2/output.txt")
    ln.cmd should ===(
      "ln -s ../../../../../nested/2/output.txt /dir/another_nested/1/2/3/4/link.txt")
  }

  @Test(
    description = "Target is a child of a directory multi level above link, relative set to false")
  def testTargetMultiLevelAboveChild() {
    val ln = new Ln(null)
    ln.relative = false
    ln.input = new File("/dir/another_nested/1/2/3/4/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s /dir/another_nested/1/2/3/4/target.txt /dir/nested/link.txt")
  }

  @Test(description = "Target is one level below link, relative set to true")
  def testTargetOneLevelBelowRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/nested/deeper/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s deeper/target.txt /dir/nested/link.txt")
  }

  @Test(description = "Target is two levels below link, relative set to true")
  def testTargetTwoLevelsBelowRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.input = new File("/dir/nested/even/deeper/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s even/deeper/target.txt /dir/nested/link.txt")
  }

  @Test(description = "Relative set to false")
  def testSameLevelAbsolute() {
    val ln = new Ln(null)
    ln.relative = false
    ln.input = new File("/dir/nested/target.txt")
    ln.output = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s /dir/nested/target.txt /dir/nested/link.txt")
  }

  // TODO: test for case where abosolute is true and input paths are relative?
}
