package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.utils.config.{ Config, Configurable }
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvanthof on 17/11/15.
 */
class SampleLibraryTagTest extends TestNGSuite with Matchers {
  @Test
  def testDefault: Unit = {
    val o = new SampleLibraryTag {
      override def root: Configurable = null
      override def globalConfig = new Config(Map())
    }
    o.sampleId shouldBe None
    o.libId shouldBe None
  }

  @Test
  def testInherit: Unit = {
    val o1 = new SampleLibraryTag {
      override def root: Configurable = null
      override def globalConfig = new Config(Map())
    }
    o1.sampleId = Some("sampleName")
    o1.libId = Some("libName")
    o1.sampleId shouldBe Some("sampleName")
    o1.libId shouldBe Some("libName")

    val o2 = new SampleLibraryTag {
      override def root: Configurable = o1
      override def globalConfig = new Config(Map())
    }
    o2.sampleId shouldBe o1.sampleId
    o2.libId shouldBe o1.libId

  }
}
