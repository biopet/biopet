package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Question, ToolCommand}

/**
  * Created by pjvanthof on 17/12/2016.
  */
trait TemplateTool extends ToolCommand {
  import TemplateTool._
  case class Args(expert: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[Unit]("expert") valueName "<file>" action { (x, c) =>
      c.copy(expert = true)
    } text "Path to input file"
  }

  /**
    * Program will split fastq file in multiple fastq files
    *
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val standard: Map[String, Any] = Map("output_dir" -> Question.askValue("Output directory", validation = List(isAbsolutePath, parentIsWritable)))
    val config = pipelineMap(standard, cmdArgs.expert)

    println(ConfigUtils.mapToYaml(config))
  }

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any]

}

object TemplateTool {
  def isAbsolutePath(value: String): Boolean = {
    if (new File(value).isAbsolute) true else {
      println(s"'$value' must be a absulute path")
      false
    }
  }

  def mustExist(value: String): Boolean = {
    if (new File(value).exists()) true else {
      println(s"'$value' does not exist")
      false
    }
  }

  def parentIsWritable(value: String): Boolean = {
    val parent = new File(value).getParentFile
    if (!parent.exists()) {
      println(s"$parent does not exist")
      false
    } else if (!parent.canRead) {
      println(s"No premision to read $parent")
      false
    } else if (!parent.canWrite) {
      println(s"No premision to write $parent")
      false
    } else true
  }
}