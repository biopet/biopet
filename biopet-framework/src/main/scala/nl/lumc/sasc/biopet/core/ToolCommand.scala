package nl.lumc.sasc.biopet.core

import org.apache.log4j.WriterAppender
import java.io.File

trait ToolCommand extends MainCommand with Logging {
  abstract class AbstractArgs {
  }

  abstract class AbstractOptParser extends scopt.OptionParser[Args](commandName) {
    opt[Unit]("log_nostderr") foreach { _ =>
      logger.removeAppender(stderrAppender)
    } text ("No output to stderr")
    opt[File]("log_file") foreach { x =>
      logger.addAppender(new WriterAppender(logLayout, new java.io.PrintStream(x)))
    } text ("Log file") valueName ("<file>")
    opt[String]('l', "log_level") foreach { x =>
      x.toLowerCase match {
        case "debug" => logger.setLevel(org.apache.log4j.Level.DEBUG)
        case "info"  => logger.setLevel(org.apache.log4j.Level.INFO)
        case "warn"  => logger.setLevel(org.apache.log4j.Level.WARN)
        case "error" => logger.setLevel(org.apache.log4j.Level.ERROR)
        case _       =>
      }
    } text ("Log level") validate { x =>
      x match {
        case "debug" | "info" | "warn" | "error" => success
        case _                                   => failure("Log level must be <debug/info/warn/error>")
      }
    }
    opt[Unit]('h', "help") foreach { _ =>
      System.err.println(this.usage)
      sys.exit(1)
    } text ("Print usage")
    opt[Unit]('v', "version") foreach { _ =>
      System.err.println("Version: " + BiopetExecutable.getVersion)
      sys.exit(1)
    } text ("Print version")
  }

  type Args <: AbstractArgs
  type OptParser <: AbstractOptParser
}