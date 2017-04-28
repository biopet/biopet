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
package nl.lumc.sasc.biopet.utils

import org.apache.log4j.Logger

import scala.collection.mutable.ListBuffer

/**
  * Trait to implement logger function on local class/object
  */
trait Logging {

  /**
    *
    * @return Global biopet logger
    */
  def logger = Logging.logger
}

/**
  * Logger object, has a global logger
  */
object Logging {
  val logger = Logger.getRootLogger

  private[biopet] val errors: ListBuffer[Exception] = ListBuffer()

  def addError(error: String, debug: String = null): Unit = {
    val msg = error + (if (debug != null && logger.isDebugEnabled) "; " + debug else "")
    logger.error(msg)
    errors.append(new Exception(msg))
  }

  def checkErrors(debug: Boolean = false): Unit = {
    if (errors.nonEmpty) {
      logger.error("*************************")
      logger.error("Biopet found some errors:")
      if (debug || logger.isDebugEnabled) {
        for (e <- errors) {
          logger.error(e.getMessage)
          logger.error(e.getStackTrace.mkString("Stack trace:\n", "\n", "\n"))
        }
      } else {
        errors.map(_.getMessage).sorted.distinct.foreach(logger.error(_))
      }
      errors.clear()
      throw new IllegalStateException("Biopet found errors")
    }
  }
}
