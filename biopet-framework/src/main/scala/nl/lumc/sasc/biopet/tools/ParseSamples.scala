package nl.lumc.sasc.biopet.tools

import java.io.File
import nl.lumc.sasc.biopet.core.ToolCommand
import scala.io.Source
import nl.lumc.sasc.biopet.core.config.Config

object ParseSamples extends ToolCommand {
  case class Args(inputFiles: List[File] = Nil) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('i', "inputFiles") required () unbounded () valueName ("<file>") action { (x, c) =>
      c.copy(inputFiles = x :: c.inputFiles)
    } text ("Input must be a tsv file, first line is seen as header and must at least have a 'sample' column, 'library' column is optional, multiple files allowed")
  }
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    val fileMaps = for (inputFile <- commandArgs.inputFiles) yield {
      val reader = Source.fromFile(inputFile)
      val lines = reader.getLines.toList
      val header = lines.head.split("\t")
      val sampleColumn = header.indexOf("sample")
      val libraryColumn = header.indexOf("library")
      if (sampleColumn == -1) throw new IllegalStateException("sample column does not exist in: " + inputFile)

      val librariesValues: List[Map[String, Any]] = for (tsvLine <- lines.tail) yield {
        val values = tsvLine.split("\t")
        val sample = values(sampleColumn)
        val library = if (libraryColumn != -1) values(libraryColumn) else null
        val valuesMap = (for (t <- 0 until values.size if t != sampleColumn if t != libraryColumn) yield (header(t) -> values(t))).toMap
        val map: Map[String, Any] = if (library != null) {
          Map("samples" -> Map(sample -> Map("libraries" -> Map(library -> valuesMap))))
        } else {
          Map("samples" -> Map(sample -> valuesMap))
        }
        map
      }
      librariesValues.foldLeft(Map[String, Any]())((acc, kv) => Config.mergeMaps(acc, kv))
    }
    val map = fileMaps.foldLeft(Map[String, Any]())((acc, kv) => Config.mergeMaps(acc, kv))
    val json = Config.mapToJson(map)
    println(json.spaces2)
  }
}
