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

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvanthof on 09/09/15.
 */
class BiopetPipeTest extends TestNGSuite with Matchers {
  class Pipe1 extends BiopetCommandLineFunction {
    val parent = null
    def cmdLine = "pipe1" +
      (if (!inputAsStdin) " input1 " else "") +
      (if (!outputAsStsout) " output1 " + "")
  }

  class Pipe2 extends BiopetCommandLineFunction {
    val parent = null
    def cmdLine = "pipe2" +
      (if (!inputAsStdin) " input2 " else "") +
      (if (!outputAsStsout) " output2 " + "")
  }

  @Test def testPipeCommands: Unit = {
    val pipe1 = new Pipe1
    val pipe2 = new Pipe2
    pipe1.commandLine.contains("pipe1") shouldBe true
    pipe1.commandLine.contains("input1") shouldBe true
    pipe1.commandLine.contains("output1") shouldBe true
    pipe2.commandLine.contains("pipe2") shouldBe true
    pipe2.commandLine.contains("input2") shouldBe true
    pipe2.commandLine.contains("output2") shouldBe true
  }

  @Test def testPipe: Unit = {
    val pipe = new Pipe1 | new Pipe2
    pipe.commandLine.contains("pipe1") shouldBe true
    pipe.commandLine.contains("input1") shouldBe true
    pipe.commandLine.contains("output1") shouldBe false
    pipe.commandLine.contains("pipe2") shouldBe true
    pipe.commandLine.contains("input2") shouldBe false
    pipe.commandLine.contains("output2") shouldBe true
  }
}
