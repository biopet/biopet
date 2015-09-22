package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * This class can pipe multiple BiopetCommandFunctions to 1 job
 *
 * Created by pjvanthof on 08/09/15.
 */
class BiopetPipe(val commands: List[BiopetCommandLineFunction]) extends BiopetCommandLineFunction {

  @Input
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

  override def beforeGraph() {
    super.beforeGraph()
    commands.foreach(_.beforeGraph())

    stdoutFile = stdoutFile.map(_.getAbsoluteFile)
    stdinFile = stdinFile.map(_.getAbsoluteFile)

    if (stdoutFile.isDefined) {
      commands.last.stdoutFile = None
      commands.last._outputAsStdout = true
    }

    if (commands.head.stdinFile.isDefined) commands.head._inputAsStdin = true

    val inputOutput = input.filter(x => output.contains(x))
    require(inputOutput.isEmpty, "File found as input and output in the same job, files: " + inputOutput.mkString(", "))
  }

  override def defaultCoreMemory = commands.map(_.defaultCoreMemory).sum
  override def defaultThreads = commands.map(_.defaultThreads).sum

  val root: Configurable = commands.head.root
  override def configName = commands.map(_.configName).mkString("-")
  def cmdLine: String = {
    "(" + commands.head.cmdLine + (if (commands.head.stdinFile.isDefined) {
      " < " + required(commands.head.stdinFile.map(_.getAbsoluteFile))
    } else "") + " | " + commands.tail.map(_.cmdLine).mkString(" | ") +
      (if (commands.last.stdoutFile.isDefined) " > " + required(commands.last.stdoutFile.map(_.getAbsoluteFile)) else "") + ")"
  }
}