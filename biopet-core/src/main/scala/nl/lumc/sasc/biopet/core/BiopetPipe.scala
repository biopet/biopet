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

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * This class can pipe multiple BiopetCommandFunctions to 1 job
  *
  * Created by pjvanthof on 08/09/15.
  */
class BiopetPipe(val commands: List[BiopetCommandLineFunction]) extends BiopetCommandLineFunction {

  @Input(required = false)
  lazy val input: List[File] = try {
    commands.flatMap(_.inputs)
  } catch {
    case e: Exception => Nil
  }

  @Output
  lazy val output: List[File] = try {
    commands.flatMap(_.outputs)
  } catch {
    case e: Exception => Nil
  }

  _pipesJobs :::= commands

  override def beforeGraph() {
    super.beforeGraph()

    stdoutFile = stdoutFile.map(_.getAbsoluteFile)
    stdinFile = stdinFile.map(_.getAbsoluteFile)

    if (stdoutFile.isDefined || _outputAsStdout) {
      commands.last.stdoutFile = None
      commands.last._outputAsStdout = true
    }

    if (commands.head.stdinFile.isDefined) commands.head._inputAsStdin = true

    val inputOutput = input.filter(x => output.contains(x))
    require(inputOutput.isEmpty,
            "File found as input and output in the same job, files: " + inputOutput.mkString(", "))
  }

  override def setResources(): Unit = {
    combineResources(_pipesJobs)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    commands.foreach(_.setupRetry())
    combineResources(commands)
  }

  override def defaultCoreMemory = 0.0
  override def defaultThreads = 0

  val parent: Configurable = commands.head.parent
  override def configNamespace = commands.map(_.configNamespace).mkString("-")
  def cmdLine: String = {
    "(" + commands.head.cmdLine + (if (commands.head.stdinFile.isDefined) {
                                     " < " + required(
                                       commands.head.stdinFile.map(_.getAbsoluteFile))
                                   } else
                                     "") + " | " + commands.tail.map(_.cmdLine).mkString(" | ") +
      (if (commands.last.stdoutFile.isDefined)
         " > " + required(commands.last.stdoutFile.map(_.getAbsoluteFile))
       else "") + ")"
  }

  override def freezeFieldValues(): Unit = {
    super.freezeFieldValues()
    commands.foreach(_.qSettings = qSettings)
  }
}