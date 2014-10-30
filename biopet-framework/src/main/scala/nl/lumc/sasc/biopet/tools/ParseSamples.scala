package nl.lumc.sasc.biopet.tools

import java.io.File
import nl.lumc.sasc.biopet.core.ToolCommand
import scala.io.Source
import java.io.PrintWriter
import argonaut._, Argonaut._

object ParseSamples extends ToolCommand {
  case class Args(librariesTsv: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "librariesTsv") required () maxOccurs (1) valueName ("<file>") action { (x, c) =>
      c.copy(librariesTsv = x)
    } text ("output file, default to stdout")
  }
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    // Libraries tsv
    val reader = Source.fromFile(commandArgs.librariesTsv)
    val lines = reader.getLines.toList
    val header = lines.head.split("\t")
    val sampleColumn = header.indexOf("sample")
    val libraryColumn = header.indexOf("library")
    if (sampleColumn == -1) throw new IllegalStateException("sample column does not exist in: " + commandArgs.librariesTsv)
    if (libraryColumn == -1) throw new IllegalStateException("library column does not exist in: " + commandArgs.librariesTsv)

    val librariesValues = (for (tsvLine <- lines.tail) yield {
      val values = tsvLine.split("\t")
      val sample = values(sampleColumn)
      val library = values(libraryColumn)
      val libraryValues = (for (t <- 0 until values.size if t != sampleColumn if t != libraryColumn) yield (header(t) -> values(t))).toMap
      ((sample, library) -> libraryValues)
    }).toMap

    for (((sample, library), values) <- librariesValues) {
      println("sample=" + sample + ", library=" + library + " = " + values)
      val summary =
        ("samples" := (sample :=
          ("libraries" := (library := (
            jEmptyObject))))) ->: jEmptyObject
      // format: ON
      val summeryText = summary.spaces2
      println(summeryText)
      //logger.debug("\n" + summeryText)
      //val writer = new PrintWriter(out)
      //writer.write(summeryText)
      //writer.close()
      //logger.debug("Stop")
    }
  }
}
