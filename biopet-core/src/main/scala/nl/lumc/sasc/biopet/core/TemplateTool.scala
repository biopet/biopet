package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Config
import nl.lumc.sasc.biopet.utils.summary.Summary
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

    val standard: Map[String, Any] = Map("output_dir" -> Question.string("Output directory",
      validation = List(isAbsolutePath, parentIsWritable)))
    val config = pipelineMap(standard, cmdArgs.expert)

    println(ConfigUtils.mapToYaml(config))
  }

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any]

  val sampleConfigs: List[File] = Nil

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

  def askSampleConfigs(currentList: List[File] = Nil): List[File] = {
    val configFile = new File(Question.string("Sample config file", validation = List(mustExist, isAbsolutePath)))
    val configMap = new Summary(configFile)
    println(s"${configMap.samples.size} samples found in config " +
      s"with in total ${configMap.libraries.map(_._2.size).sum} libraries for '$configFile'")
    if (Question.boolean("Is this correct?")) {
      if (Question.boolean("Add more sample configs?")) askSampleConfigs(configFile :: currentList)
      else {
        val files = configFile :: currentList
        if (files.size > 1) {
          val configs = files.map(ConfigUtils.fileToConfigMap(_))
          val sizes = configs.map(new Summary(_)).map(x => (x.samples.size, x.libraries.map(_._2.size).sum))
          val samples = sizes.map(_._1).sum
          val libs = sizes.map(_._2).sum
          val mergedConfig = new Summary(configs.foldLeft(Map[String, Any]())((a,b) => ConfigUtils.mergeMaps(a, b)))
          val mergesSamples = mergedConfig.samples.size
          val mergesLibraries = mergedConfig.libraries.map(_._2.size).sum
          if (mergesSamples != samples) println("WARNING: Overlapping samples detected")
          if (mergesLibraries != libs) println("WARNING: Overlapping libraries detected")
          println(s"$mergesSamples samples found in merged config with in total $mergesLibraries libraries")
          if (Question.boolean("Is this correct?")) files
          else {
            println("Resetting sample configs")
            askSampleConfigs()
          }
        } else files
      }
    } else askSampleConfigs(currentList)
  }
}