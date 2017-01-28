package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.utils.summary.db.Schema
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, ToolCommand }

/**
  * Created by pjvanthof on 26/01/2017.
  */
object SummaryToSqlite extends ToolCommand {

  case class Args(inputJson: File = null,
                  outputSqlite: File = null,
                  force: Boolean = false) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "inputJson") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(inputJson = x)
    } text "Input json file"
    opt[File]('o', "outputHdf5") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputSqlite = x)
    } text "Output hdf5 file"
    opt[Unit]('f', "force") action { (x, c) =>
      c.copy(force = true)
    } text "If database already exist it will be moved"
  }

  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)
    logger.info("Start")

    val jsonMap = ConfigUtils.fileToConfigMap(cmdArgs.inputJson)

    if (cmdArgs.outputSqlite.exists()) {
      if (cmdArgs.force) {
        logger.warn("Deleting old database")
        cmdArgs.outputSqlite.delete()
      } else throw new IllegalArgumentException(s"Db already exist: ${cmdArgs.outputSqlite}")
    }

    Schema.createEmptySqlite(cmdArgs.outputSqlite)

    logger.info("Done")
  }

}
