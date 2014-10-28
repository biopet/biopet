/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.extensions

import java.io.File
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class LnUnitTest extends TestNGSuite with Matchers {

  @Test(description = "Target and link in the same directory, relative set to true")
  def testSameLevelRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.in = new File("/dir/nested/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s 'target.txt' '/dir/nested/link.txt'")
  }

  @Test(description = "Target is one level above link, relative set to true")
  def testTargetOneLevelAboveRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.in = new File("/dir/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s '../target.txt' '/dir/nested/link.txt'")
  }

  @Test(description = "Target is two levels above link, relative set to true")
  def testTargetTwoLevelsAboveRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.in = new File("/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s '../../target.txt' '/dir/nested/link.txt'")
  }

  @Test(description = "Target is a child of a directory one level above link, relative set to true")
  def testTargetOneLevelAboveChildRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.in = new File("/dir/another_nested/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s '../another_nested/target.txt' '/dir/nested/link.txt'")
  }

  @Test(description = "Target is one level below link, relative set to true")
  def testTargetOneLevelBelowRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.in = new File("/dir/nested/deeper/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s 'deeper/target.txt' '/dir/nested/link.txt'")
  }

  @Test(description = "Target is two levels below link, relative set to true")
  def testTargetTwoLevelsBelowRelative() {
    val ln = new Ln(null)
    ln.relative = true
    ln.in = new File("/dir/nested/even/deeper/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s 'even/deeper/target.txt' '/dir/nested/link.txt'")
  }

  @Test(description = "Relative set to false")
  def testSameLevelAbsolute() {
    val ln = new Ln(null)
    ln.relative = false
    ln.in = new File("/dir/nested/target.txt")
    ln.out = new File("/dir/nested/link.txt")
    ln.cmd should ===("ln -s '/dir/nested/target.txt' '/dir/nested/link.txt'")
  }

  // TODO: test for case where abosolute is true and input paths are relative?
}
