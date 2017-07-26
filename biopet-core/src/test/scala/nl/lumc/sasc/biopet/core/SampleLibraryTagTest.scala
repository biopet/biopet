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

import nl.lumc.sasc.biopet.utils.config.{Config, Configurable}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
  * Created by pjvanthof on 17/11/15.
  */
class SampleLibraryTagTest extends TestNGSuite with Matchers {
  @Test
  def testDefault(): Unit = {
    val o = new SampleLibraryTag {
      override def parent: Configurable = null
      override def globalConfig = new Config(Map())
    }
    o.sampleId shouldBe None
    o.libId shouldBe None
  }

  @Test
  def testInherit(): Unit = {
    val o1 = new SampleLibraryTag {
      override def parent: Configurable = null
      override def globalConfig = new Config(Map())
    }
    o1.sampleId = Some("sampleName")
    o1.libId = Some("libName")
    o1.sampleId shouldBe Some("sampleName")
    o1.libId shouldBe Some("libName")

    val o2 = new SampleLibraryTag {
      override def parent: Configurable = o1
      override def globalConfig = new Config(Map())
    }
    o2.sampleId shouldBe o1.sampleId
    o2.libId shouldBe o1.libId

  }
}
