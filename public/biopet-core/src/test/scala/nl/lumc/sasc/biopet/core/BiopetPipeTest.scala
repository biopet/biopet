package nl.lumc.sasc.biopet.core

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

/**
 * Created by pjvanthof on 09/09/15.
 */
class BiopetPipeTest extends TestNGSuite with Matchers {
  class Pipe1 extends BiopetCommandLineFunction {
    val root = null
    def cmdLine = "pipe1" +
      (if (!inputAsStdin) " input1 " else "") +
      (if (!outputAsStsout) " output1 " + "")
  }

  class Pipe2 extends BiopetCommandLineFunction {
    val root = null
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
