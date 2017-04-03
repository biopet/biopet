/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.utils.rscript

import java.io.{File, FileOutputStream}

import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.process.Sys
import nl.lumc.sasc.biopet.utils.process.Sys.AsyncExecResult

import scala.collection.parallel.mutable.ParSeq
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.sys.process.ProcessLogger

/**
 * Trait for rscripts, can be used to execute rscripts locally
 *
 * Created by pjvanthof on 13/09/15.
 */
trait Rscript extends Configurable {
  protected var script: File

  def rscriptExecutable: String = config("exe", default = "Rscript", namespace = "rscript")

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
        case Some(d) => new File(d, script.getName)
        case _ =>
          val file = File.createTempFile(script.getName, ".R")
          file.deleteOnExit()
          file
      }
      if (!rScript.getAbsoluteFile.getParentFile.exists) rScript.getParentFile.mkdirs

      val is = getClass.getResourceAsStream(script.getPath)
      val os = new FileOutputStream(rScript)

      org.apache.commons.io.IOUtils.copy(is, os)
      os.close()

      script = rScript
    }
  }

  private val cache: ParSeq[AsyncExecResult] = Nil

  /**
   * Execute rscript on local system
   * @param logger How to handle stdout and stderr
   */
  def runLocal(logger: ProcessLogger)(implicit ec: ExecutionContext): Unit = {
    checkScript()

    Logging.logger.info("Running: " + cmd.mkString(" "))

    while (cache.size > 5) for (c <- cache) Thread.

    val results = Sys.execAsync(cmd)

    while(results.isRunning) {
      Thread.sleep(1000)
    }

    val (exitcode, stdout, stderr) = Await.result(results.map(x => (x._1, x._2, x._3)), Duration.Inf)

    Logging.logger.info("stdout:\n" + stdout + "\n")
    Logging.logger.info("stderr:\n" + stderr)

    Logging.logger.info(exitcode)
  }

  /**
   * Execute rscript on local system
   * Stdout and stderr will go to biopet logger
   */
  def runLocal()(implicit ec: ExecutionContext): Unit = {
    runLocal(ProcessLogger(Logging.logger.info(_)))
  }
}
