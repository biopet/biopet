package nl.lumc.sasc.biopet.core

import org.apache.log4j.Logger

trait Logging {
  def logger = Logging.logger
}

object Logging {
  val logger = Logger.getRootLogger
}