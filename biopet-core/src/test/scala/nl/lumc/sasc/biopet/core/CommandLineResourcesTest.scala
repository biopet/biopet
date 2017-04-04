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

import nl.lumc.sasc.biopet.utils.config.{ Config, Configurable }
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import scala.language.reflectiveCalls

/**
 * Created by pjvanthof on 17/11/15.
 */
class CommandLineResourcesTest extends TestNGSuite with Matchers {
  class CommandLineFunctionMock(c: Map[String, Any] = Map()) extends CommandLineFunction with Configurable {
    override def freezeFieldValues() {}
    def commandLine = "command"
    val parent = null
    override def globalConfig = new Config(c)
  }

  @Test
  def testDefaults(): Unit = {
    val cmd = new CommandLineFunctionMock with CommandLineResources
    cmd.coreMemory shouldBe 2.0
    cmd.residentFactor shouldBe 1.2
    cmd.vmemFactor shouldBe 1.4
    cmd.retry shouldBe 0
    cmd.threads shouldBe 1

    cmd.setResources()

    cmd.memoryLimit shouldBe Some(cmd.coreMemory * cmd.threads)
    cmd.residentLimit shouldBe Some(cmd.coreMemory * cmd.residentFactor)
    cmd.vmem shouldBe Some((cmd.coreMemory * cmd.vmemFactor) + "G")

    cmd.jobResourceRequests shouldBe empty

    cmd.freezeFieldValues()

    cmd.jobResourceRequests should contain("h_vmem=" + cmd.vmem.get)

    cmd.setupRetry()
    cmd.retry shouldBe 1
    cmd.setupRetry()
    cmd.retry shouldBe 2
    cmd.setupRetry()
    cmd.retry shouldBe 3

  }

  @Test
  def testMaxThreads(): Unit = {
    val cmd = new CommandLineFunctionMock(Map("maxthreads" -> 5, "threads" -> 10)) with CommandLineResources

    cmd.threads shouldBe 5
  }

  @Test
  def testCombine(): Unit = {
    val cmd1 = new CommandLineFunctionMock with CommandLineResources
    val cmd2 = new CommandLineFunctionMock with CommandLineResources
    val mainCmd = new CommandLineFunctionMock with CommandLineResources {
      def combine(functions: List[CommandLineResources]) = combineResources(functions)
    }
    mainCmd.combine(List(cmd1, cmd2))

    mainCmd.coreMemory shouldBe 2.0
    mainCmd.residentFactor shouldBe 1.2
    mainCmd.vmemFactor shouldBe 1.4
    mainCmd.memoryLimit shouldBe Some(4.0)
    mainCmd.retry shouldBe 0
    mainCmd.threads shouldBe 2

  }
}
