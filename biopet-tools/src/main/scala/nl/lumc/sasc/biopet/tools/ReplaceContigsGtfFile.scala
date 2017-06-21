package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.{FastaUtils, ToolCommand}
import nl.lumc.sasc.biopet.utils.annotation.Feature

import scala.io.Source

/**
  * Created by pjvan_thof on 30-5-17.
  */
object ReplaceContigsGtfFile extends ToolCommand {
  case class Args(input: File = null,
                  output: File = null,
                  contigs: Map[String, String] = Map(),
                  writeAsGff: Boolean = false)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    } text "Input gtf file"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    } text "Output gtf file"
    opt[Map[String, String]]("contig") unbounded () action { (x, c) =>
      c.copy(contigs = c.contigs ++ x)
    }
    opt[Unit]("writeAsGff") unbounded () action { (_, c) =>
      c.copy(writeAsGff = true)
    }
    opt[File]("contigMappingFile") unbounded () action { (x, c) =>
      c.copy(contigs = c.contigs ++ FastaUtils.readContigMapReverse(x))
    } text "File how to map contig names, first column is the new name, second column is semicolon separated list of alternative names"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

    if (!cmdArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + cmdArgs.input)

    logger.info("Start")

    val reader = Source.fromFile(cmdArgs.input)
    val writer = new PrintWriter(cmdArgs.output)

    def writeLine(feature: Feature): Unit = {
      if (cmdArgs.writeAsGff) writer.println(feature.asGff3Line)
      else writer.println(feature.asGtfLine)
    }

    reader.getLines().foreach { line =>
      if (line.startsWith("#")) writer.println(line)
      else {
        val feature = Feature.fromLine(line)
        if (cmdArgs.contigs.contains(feature.contig))
          writeLine(feature.copy(contig = cmdArgs.contigs(feature.contig)))
        else writeLine(feature)
      }
    }
    reader.close()
    writer.close()
    logger.info("Done")
  }

}
