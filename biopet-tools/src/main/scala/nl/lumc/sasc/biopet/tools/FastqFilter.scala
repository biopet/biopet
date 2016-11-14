package nl.lumc.sasc.biopet.tools

import java.io.File

import htsjdk.samtools.fastq.{ AsyncFastqWriter, BasicFastqWriter, FastqReader }
import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.util.matching.Regex
import scala.collection.JavaConversions._

/**
 * Created by pjvan_thof on 28-10-16.
 */
object FastqFilter extends ToolCommand {
  /**
   * Arg for commandline program
   * @param inputFile input fastq file
   * @param outputFile output fastq files
   */
  case class Args(inputFile: File = null,
                  outputFile: File = null,
                  idRegex: Option[Regex] = None) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputFile") required () valueName "<file>" action { (x, c) =>
      c.copy(inputFile = x)
    } text "Path to input file"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "Path to output file"
    opt[String]("idRegex") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(idRegex = Some(x.r))
    } text "Regex to match ID"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info("Start")

    val reader = new FastqReader(cmdArgs.inputFile)
    val writer = new AsyncFastqWriter(new BasicFastqWriter(cmdArgs.outputFile), 10000)

    var total = 0
    var kept = 0
    for (record <- reader.iterator()) {
      if (cmdArgs.idRegex.map(_.findFirstIn(record.getReadHeader.takeWhile(_ != ' ')).isDefined).getOrElse(true)) {
        writer.write(record)
        kept += 1
      }
      total += 1
      if (total % 100000 == 0) logger.info(s"Total reads: $total,  reads left: $kept")
    }
    logger.info(s"Total reads: $total,  reads left: $kept")

    writer.close()
    reader.close()

    logger.info("Done")
  }
}
