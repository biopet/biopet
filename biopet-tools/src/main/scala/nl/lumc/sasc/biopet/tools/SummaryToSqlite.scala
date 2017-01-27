package nl.lumc.sasc.biopet.tools

import java.io.File

import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

    val db = Database.forURL(s"jdbc:sqlite:${cmdArgs.outputSqlite.getAbsolutePath}", driver = "org.sqlite.JDBC")

    try {
      import nl.lumc.sasc.biopet.utils.summary.db.Schema._

      val setup = DBIO.seq(
        (runs.schema ++ samples.schema ++
          samplesRuns.schema ++ libraries.schema ++
          librariesRuns.schema ++ pipelines.schema).create
      )
      val setupFuture = db.run(setup)
      Await.result(setupFuture, Duration.Inf)
    } finally db.close
    logger.info("Done")
  }

}
