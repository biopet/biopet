package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.core.summary.Summary

/**
 * This is a tools to extract values from a summary to a tsv file
 *
 * Created by pjvan_thof on 4/23/15.
 */
object SummaryToTsv extends ToolCommand {
  case class Args(summary: File = null,
                  outputFile: Option[File] = None,
                  values: List[String] = Nil,
                  mode: String = "root") extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('s', "summary") required () unbounded () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(summary = x)
    }
    opt[File]('o', "output") maxOccurs 1 unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    }
    opt[String]('p', "path") required () unbounded () valueName "<value>" action { (x, c) =>
      c.copy(values = c.values ::: x :: Nil)
    }
    opt[String]('m', "mode") maxOccurs 1 unbounded () valueName "<root|sample|lib>" action { (x, c) =>
      c.copy(mode = x)
    }

  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val summary = new Summary(cmdArgs.summary)

    val paths = cmdArgs.values.map(x => {
      val split = x.split("=", 2)
      split(0) -> split(1).split(":")
    })

    val values = fetchValues(summary, paths.toMap, sample = cmdArgs.mode == "sample", lib = cmdArgs.mode == "lib")

    println(paths.map(_._1).mkString("\t", "\t", ""))

    for (lineId <- values.head._2.keys) {
      println(paths.map(x => values(x._1)(lineId).getOrElse("")).mkString(lineId + "\t", "\t", ""))
    }
  }

  def fetchValues(summary: Summary, paths: Map[String, Array[String]],
                  sample: Boolean = false,
                  lib: Boolean = false) = {
    for ((name, path) <- paths) yield name -> {
      if (lib) summary.getLibraryValues(path: _*).map(a => (a._1._1 + "-" + a._1._2) -> a._2)
      else if (sample) summary.getSampleValues(path: _*)
      else Map("value" -> summary.getValue(path: _*))
    }
  }
}
