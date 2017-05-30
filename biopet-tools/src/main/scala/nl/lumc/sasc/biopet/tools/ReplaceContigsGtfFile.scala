package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.annotation.Feature

import scala.io.Source

/**
  * Created by pjvan_thof on 30-5-17.
  */
object ReplaceContigsGtfFile extends ToolCommand {
  case class Args(input: File = null, output: File = null, contigs: Map[String, String] = Map())
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    } text "Input gtf file"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    } text "Output gtf file"
    opt[Map[String, String]]("contig") unbounded () action { (x, c) =>
      c.copy(contigs = c.contigs ++ x)
    }
    opt[File]("contigMappingFile") unbounded () action { (x, c) =>
      val reader = Source.fromFile(x)
      val map = reader
        .getLines()
        .flatMap { line =>
          val columns = line.split("\t")
          val newContig = columns(0)
          columns(1).split(",").map(alterniveName => (alterniveName, newContig))
        }
        .toMap
      reader.close()
      c.copy(contigs = c.contigs ++ map)
    } text "File how to map contig names, first column is the new name, second column is coma separated list of alternative names"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (!cmdArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + cmdArgs.input)

    logger.info("Start")

    val reader = Source.fromFile(cmdArgs.input)
    val writer = new PrintWriter(cmdArgs.output)
    reader.getLines().foreach { line =>
      if (line.startsWith("#")) writer.println(line)
      else {
        val feature = Feature.fromLine(line)
        if (cmdArgs.contigs.contains(feature.contig))
          writer.println(feature.copy(contig = cmdArgs.contigs(feature.contig)).asGtfLine)
        else writer.println(feature.asGtfLine)
      }
    }
    reader.close()
    writer.close()
    logger.info("Done")
  }

}
