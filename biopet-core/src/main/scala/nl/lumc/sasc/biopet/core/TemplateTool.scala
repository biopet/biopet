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

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.summary.Summary
import nl.lumc.sasc.biopet.utils.{ConfigUtils, Question, ToolCommand}

import scala.io.Source

/**
  * Created by pjvanthof on 17/12/2016.
  */
trait TemplateTool extends ToolCommand {
  import TemplateTool._
  case class Args(outputConfig: File = null,
                  runScript: Option[File] = None,
                  expert: Boolean = false,
                  template: Option[File] = None)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('o', "outputConfig") required () valueName "<file>" action { (x, c) =>
      c.copy(outputConfig = x)
    } text "Path to output config"
    opt[File]('s', "outputScript") valueName "<file>" action { (x, c) =>
      c.copy(runScript = Some(x))
    } text "Path to output script"
    opt[File]('t', "template") valueName "<file>" action { (x, c) =>
      c.copy(template = Some(x))
    } text "Path to template. By default it will try to fetch this from the ENV value 'BIOPET_SCRIPT_TEMPLATE'"
    opt[Unit]("expert") action { (_, c) =>
      c.copy(expert = true)
    } text "This enables expert options / questions"
  }

  /**
    * Program will split fastq file in multiple fastq files
    *
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    cmdArgs.runScript.foreach(
      writeScript(_, cmdArgs.outputConfig, sampleConfigs, cmdArgs.template))

    val standard: Map[String, Any] = Map(
      "output_dir" -> Question.string("Output directory",
                                      validation = List(isAbsolutePath, parentIsWritable)))
    val config = pipelineMap(standard, cmdArgs.expert)

    val configWriter = new PrintWriter(cmdArgs.outputConfig)
    configWriter.println(ConfigUtils.mapToYaml(config))
    configWriter.close()
  }

  def writeScript(outputFile: File, config: File, samples: List[File], t: Option[File]): Unit = {
    val template = t match {
      case Some(f) => f
      case _ =>
        sys.env.get("BIOPET_SCRIPT_TEMPLATE") match {
          case Some(file) => new File(file)
          case _ =>
            throw new IllegalArgumentException(
              "No template found on argument or 'BIOPET_SCRIPT_TEMPLATE'")
        }
    }

    val templateReader = Source.fromFile(template)
    val scriptWriter = new PrintWriter(outputFile)

    val biopetArgs: String =
      (config :: samples).map(_.getAbsolutePath).mkString("-config ", " \\\n-config ", "")
    templateReader
      .getLines()
      .mkString("\n")
      .format(pipelineName, biopetArgs)
      .foreach(scriptWriter.print)
    templateReader.close()
    scriptWriter.close()
    outputFile.setExecutable(true, false)
  }

  def pipelineName: String

  def pipelineMap(map: Map[String, Any], expert: Boolean): Map[String, Any]

  def sampleConfigs: List[File] = Nil

}

object TemplateTool {
  def isAbsolutePath(value: String): Boolean = {
    if (new File(value).isAbsolute) true
    else {
      println(s"'$value' must be a absulute path")
      false
    }
  }

  def mustExist(value: String): Boolean = {
    if (new File(value).exists()) true
    else {
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
      println(s"No permission to read $parent")
      false
    } else if (!parent.canWrite) {
      println(s"No permission to write $parent")
      false
    } else true
  }

  def askSampleConfigs(currentList: List[File] = Nil): List[File] = {
    val configFile = new File(
      Question.string("Sample config file", validation = List(mustExist, isAbsolutePath)))
    val configMap = new Summary(configFile)
    println(
      s"${configMap.samples.size} samples found in config " +
        s"with in total ${configMap.libraries.map(_._2.size).sum} libraries for '$configFile'")
    if (Question.boolean("Is this correct?")) {
      if (Question.boolean("Add more sample configs?")) askSampleConfigs(configFile :: currentList)
      else {
        val files = configFile :: currentList
        if (files.size > 1) {
          val configs = files.map(f => new Summary(ConfigUtils.fileToConfigMap(f)))
          val samples = configs.flatMap(_.samples.toList)
          val libs = configs.flatMap(_.libraries.flatMap {
            case (s, libs) => libs.toList.map(l => (s, l))
          })
          val mergedConfig = new Summary(
            configs.foldLeft(Map[String, Any]())((a, b) => ConfigUtils.mergeMaps(a, b.map)))
          val mergesSamples = mergedConfig.samples.size
          val mergesLibraries = mergedConfig.libraries.map(_._2.size).sum
          if (mergesSamples != samples.size) {
            val overlappingSamples = samples
              .groupBy(s1 => samples.count(s2 => s1 == s2))
              .filter(_._1 > 1)
              .flatMap(_._2)
              .toList
              .distinct
            println("WARNING: Overlapping samples detected:")
            overlappingSamples.foreach(s => println(s"  - $s"))
          }
          if (mergesLibraries != libs.size) {
            val overlappingLibs = libs
              .groupBy(l1 => libs.count(l2 => l1 == l2))
              .filter(_._1 > 1)
              .flatMap(_._2)
              .toList
              .distinct
            println("WARNING: Overlapping libraries detected")
            overlappingLibs.foreach(l => println(s"  - ${l._1} -> ${l._2}"))
          }
          println(
            s"$mergesSamples samples found in merged config with in total $mergesLibraries libraries")
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
