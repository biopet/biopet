package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommand
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList

/**
 * Created by pjvanthof on 22/08/15.
 */
object SquishBed extends ToolCommand {

  case class Args(input: File = null, output: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    if (!cmdArgs.input.exists) throw new IllegalStateException("Input file not found, file: " + cmdArgs.input)

    val records = BedRecordList.fromFile(cmdArgs.input).squishBed()

  }
}
