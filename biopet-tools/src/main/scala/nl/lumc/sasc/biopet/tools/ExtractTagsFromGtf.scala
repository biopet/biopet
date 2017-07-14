package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.annotation.Feature

import scala.io.Source

/**
  * Created by pjvanthof on 8-6-17.
  */
object ExtractTagsFromGtf extends ToolCommand {
  case class Args(outputFile: File = null,
                  gtfFile: File = null,
                  tags: List[String] = Nil,
                  feature: Option[String] = None)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "Input refFlat file. Mandatory"
    opt[File]('g', "gtfFile") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(gtfFile = x)
    } text "Output gtf file. Mandatory"
    opt[String]('t', "tag") required () unbounded () valueName "<string>" action { (x, c) =>
      c.copy(tags = c.tags ::: x :: Nil)
    } text "Tags to extract"
    opt[String]('f', "feature") unbounded () valueName "<string>" action { (x, c) =>
      c.copy(feature = Some(x))
    } text "Filter for only this feature type"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val reader = Source.fromFile(cmdArgs.gtfFile)
    val writer = new PrintWriter(cmdArgs.outputFile)
    writer.println(cmdArgs.tags.mkString("#", "\t", ""))

    reader
      .getLines()
      .filter(!_.startsWith("#"))
      .map(Feature.fromLine)
      .filter(f => cmdArgs.feature.forall(_ == f.feature))
      .foreach { f =>
        writer.println(cmdArgs.tags.map(f.attributes.get).map(_.getOrElse(".")).mkString("\t"))
      }

    reader.close()
    writer.close()

    logger.info("Done")
  }

}
