package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.core.{ ToolCommandFuntion, ToolCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.utils.intervals.BedRecordList
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvanthof on 22/08/15.
 */
class SquishBed(val root: Configurable) extends ToolCommandFuntion {
  javaMainClass = getClass.getName

  @Input(doc = "Input Bed file", required = true)
  var input: File = _

  @Output(doc = "Output interval list", required = true)
  var output: File = _

  var strandSensitive: Boolean = config("strandSensitive", default = false)

  override def commandLine = super.commandLine +
    required("-I", input) +
    required("-o", output) +
    conditional(strandSensitive, "-s")
}

object SquishBed extends ToolCommand {

  case class Args(input: File = null,
                  output: File = null,
                  strandSensitive: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
    opt[Unit]('s', "strandSensitive") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(strandSensitive = true)
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse sys.exit(1)

    if (!cmdArgs.input.exists) throw new IllegalStateException("Input file not found, file: " + cmdArgs.input)

    logger.info("Start")

    val records = BedRecordList.fromFile(cmdArgs.input)
    val length = records.length
    val refLength = records.combineOverlap.length
    logger.info(s"Total bases: $length")
    logger.info(s"Total bases on reference: $refLength")
    logger.info("Start squishing")
    val squishBed = records.squishBed(cmdArgs.strandSensitive).sorted
    logger.info("Done squishing")
    val squishLength = squishBed.length
    val squishRefLength = squishBed.combineOverlap.length
    logger.info(s"Total bases left: $squishLength")
    logger.info(s"Total bases left on reference: $squishRefLength")
    logger.info(s"Total bases removed from ref: ${refLength - squishRefLength}")
    squishBed.writeToFile(cmdArgs.output)

    logger.info("Done")
  }
}
