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
package nl.lumc.sasc.biopet.tools

import java.io.{PrintWriter, File}

import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.summary.Summary

/**
  * This is a tools to extract values from a summary to a tsv file
  *
  * Created by pjvan_thof on 4/23/15.
  */
object SummaryToTsv extends ToolCommand {
  case class Args(summary: File = null,
                  outputFile: Option[File] = None,
                  values: List[String] = Nil,
                  mode: String = "root")
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('s', "summary") required () unbounded () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(summary = x)
    }
    opt[File]('o', "outputFile") unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    }
    opt[String]('p', "path") required () unbounded () valueName "<string>" action { (x, c) =>
      c.copy(values = c.values ::: x :: Nil)
    } text
      """
        |String that determines the values extracted from the summary. Should be of the format:
        |<header_name>=<namespace>:<lower_namespace>:<even_lower_namespace>...
      """.stripMargin
    opt[String]('m', "mode") maxOccurs 1 unbounded () valueName "<root|sample|lib>" action {
      (x, c) =>
        c.copy(mode = x)
    } validate { x =>
      if (Set("root", "sample", "lib").contains(x)) success else failure("Unsupported mode")
    } text
      """
        |Determines on what level to aggregate data.
        |root: at the root level
        |sample: at the sample level
        |lib: at the library level
      """.stripMargin

  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val summary = new Summary(cmdArgs.summary)

    val paths = cmdArgs.values
      .map(x => {
        val split = x.split("=", 2)
        split(0) -> split(1).split(":")
      })
      .toMap

    val values =
      fetchValues(summary, paths, sample = cmdArgs.mode == "sample", lib = cmdArgs.mode == "lib")

    cmdArgs.outputFile match {
      case Some(file) => {
        val writer = new PrintWriter(file)
        writer.println(createHeader(paths))
        for (lineId <- values.head._2.keys)
          writer.println(createLine(paths, values, lineId))
        writer.close()
      }
      case _ => {
        println(createHeader(paths))
        for (lineId <- values.head._2.keys)
          println(createLine(paths, values, lineId))
      }
    }
  }

  def fetchValues(summary: Summary,
                  paths: Map[String, Array[String]],
                  sample: Boolean = false,
                  lib: Boolean = false) = {
    for ((name, path) <- paths)
      yield
        name -> {
          if (lib) {
            summary.getLibraryValues(path: _*).map(a => (a._1._1 + "-" + a._1._2) -> a._2)
          } else if (sample) summary.getSampleValues(path: _*)
          else Map("value" -> summary.getValue(path: _*))
        }
  }

  def createHeader(paths: Map[String, Array[String]]): String = {
    paths.map(_._1).mkString("\t", "\t", "")
  }

  def createLine(paths: Map[String, Array[String]],
                 values: Map[String, Map[String, Option[Any]]],
                 lineId: String): String = {
    paths.map(x => values(x._1)(lineId).getOrElse("")).mkString(lineId + "\t", "\t", "")
  }
}
