package nl.lumc.sasc.biopet.utils.rscript

import java.io.{ File, FileOutputStream }

import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable

import scala.sys.process.{ Process, ProcessLogger }

/**
 * Created by pjvanthof on 13/09/15.
 */
trait Rscript extends Configurable {
  protected var script: File

  def rscriptExecutable: String = config("exe", default = "Rscript", submodule = "Rscript")

  /** This is the defaul implementation, to add arguments override this */
  def cmd: Seq[String] = Seq(rscriptExecutable, script.getAbsolutePath)

  /**
   * If script not exist in file system it try to copy it from the jar
   * @param dir Directory to store temp script, if None or not given File.createTempFile is called
   */
  protected def checkScript(dir: Option[File] = None): Unit = {
    if (script.exists()) {
      script = script.getAbsoluteFile
    } else {
      val rScript: File = dir match {
        case Some(dir) => new File(dir, script.getName)
        case _         => File.createTempFile(script.getName, ".R")
      }
      if (!rScript.getParentFile.exists) rScript.getParentFile.mkdirs

      val is = getClass.getResourceAsStream(script.getPath)
      val os = new FileOutputStream(rScript)

      org.apache.commons.io.IOUtils.copy(is, os)
      os.close()

      script = rScript
    }
  }

  /**
   * Execute rscript on local system
   * @param logger How to handle stdout and stderr
   */
  def runLocal(logger: ProcessLogger): Unit = {
    checkScript()

    Logging.logger.info("Running: " + cmd.mkString(" "))

    val process = Process(cmd).run(logger)
    Logging.logger.info(process.exitValue())
  }

  /**
   * Execute rscript on local system
   * Stdout and stderr will go to biopet logger
   */
  def runLocal(): Unit = {
    runLocal(ProcessLogger(Logging.logger.info(_)))
  }
}
