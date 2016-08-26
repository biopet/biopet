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

import nl.lumc.sasc.biopet.FullVersion

/**
 * Trait for biopet tools, sets some default args
 */
trait ToolCommand extends MainCommand with Logging {
  /** Placeholder for args */
  protected abstract class AbstractArgs {}

  /**
   * Abstract opt parser to add efault args to each biopet tool
   */
  protected abstract class AbstractOptParser extends scopt.OptionParser[Args](commandName) {
    opt[String]('l', "log_level") foreach { x =>
      x.toLowerCase match {
        case "debug" => logger.setLevel(org.apache.log4j.Level.DEBUG)
        case "info"  => logger.setLevel(org.apache.log4j.Level.INFO)
        case "warn"  => logger.setLevel(org.apache.log4j.Level.WARN)
        case "error" => logger.setLevel(org.apache.log4j.Level.ERROR)
        case _       =>
      }
    } text "Level of log information printed. Possible levels: 'debug', 'info', 'warn', 'error'" validate {
      case "debug" | "info" | "warn" | "error" => success
      case _                                   => failure("Log level must be <debug/info/warn/error>")
    }
    opt[Unit]('h', "help") foreach { _ =>
      System.err.println(this.usage)
      sys.exit(1)
    } text "Print usage"
    opt[Unit]('v', "version") foreach { _ =>
      System.err.println("Version: " + FullVersion)
      sys.exit(1)
    } text "Print version"
  }

  protected type Args <: AbstractArgs
  protected type OptParser <: AbstractOptParser
}

