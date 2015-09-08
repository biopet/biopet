package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

/**
 * Created by pjvanthof on 08/09/15.
 */
class BiopetPipe(val commands: List[BiopetCommandLineFunction]) extends BiopetCommandLineFunction {

  @Input
  lazy val input: List[File] = try {
    commands.map(_.inputs).flatten
  } catch {
    case e: Exception => Nil
  }

  @Output
  lazy val output: List[File] = try {
    commands.map(_.outputs).flatten
  } catch {
    case e: Exception => Nil
  }

  override def beforeGraph {
    super.beforeGraph()
    commands.foreach(_.beforeGraph())

    stdoutFile = stdoutFile.map(_.getAbsoluteFile)
    stdinFile = stdinFile.map(_.getAbsoluteFile)


    if (stdoutFile.isDefined) {
      commands.last.stdoutFile = None
      commands.last._outputAsStdout = true
    }

    if (commands.head.stdinFile.isDefined) commands.head._inputAsStdin = true

    val inputOutput = input.filter(x => output.exists(y => x == y))
    require(inputOutput.isEmpty, "File found as input and output in the same job, files: " + inputOutput.mkString(", "))
  }

  val root: Configurable = commands.head.root
  override def configName = commands.map(_.configName).mkString("-")
  def cmdLine: String = {
    "(" + commands.head.cmdLine + (if (commands.head.stdinFile.isDefined) {
      " < " + required(commands.head.stdinFile.map(_.getAbsoluteFile))
    } else "") + " | " + commands.tail.map(_.cmdLine).mkString(" | ") +
      (if (commands.last.stdoutFile.isDefined) " > " + required(commands.last.stdoutFile.map(_.getAbsoluteFile)) else "") + ")"
  }
}