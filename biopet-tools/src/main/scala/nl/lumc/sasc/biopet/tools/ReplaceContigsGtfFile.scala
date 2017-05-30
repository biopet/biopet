package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand
import nl.lumc.sasc.biopet.utils.annotation.Feature

import scala.io.Source

/**
  * Created by pjvan_thof on 30-5-17.
  */
object ReplaceContigsGtfFile extends ToolCommand {
  case class Args(input: File = null, output: File = null, contigs: Map[String, String] = Map()) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
    opt[Map[String, String]]("contig") unbounded () action { (x, c) =>
      c.copy(contigs = c.contigs ++ x)
    }
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
