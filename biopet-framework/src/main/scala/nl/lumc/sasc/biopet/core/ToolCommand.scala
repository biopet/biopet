package nl.lumc.sasc.biopet.core

import java.text.SimpleDateFormat
import java.util.Calendar
import org.apache.log4j.Logger
import org.apache.log4j.WriterAppender
import org.apache.log4j.helpers.DateLayout
import org.apache.log4j.spi.LoggingEvent
import java.io.File

trait ToolCommand extends MainCommand {
  abstract class AbstractArgs {
  }
  
  abstract class AbstractOptParser extends scopt.OptionParser[Args](commandName) {
    opt[Unit]("log_nostderr") foreach { _ =>
      logger.removeAppender(stderrAppender) } text("No output to stderr")
    opt[File]("log_file") foreach { x =>
      logger.addAppender(new WriterAppender(logLayout, new java.io.PrintStream(x))) } text("Log file") valueName("<file>")
    opt[String]('l', "log_level") foreach { x =>
        x.toLowerCase match {
          case "debug" => logger.setLevel(org.apache.log4j.Level.DEBUG)
          case "info" => logger.setLevel(org.apache.log4j.Level.INFO)
          case "warn" => logger.setLevel(org.apache.log4j.Level.WARN)
          case "error" => logger.setLevel(org.apache.log4j.Level.ERROR)
          case _ =>
        } } text("Log level") validate { x => x match {
            case "debug" | "info" | "warn" | "error" => success
            case _ => failure("Log level must be <debug/info/warn/error>")
          }
        }
    opt[Unit]('h', "help") foreach { _ =>
      System.err.println(this.usage); sys.exit(1)} text("Print usage")
  }
  
  type Args <: AbstractArgs
  type OptParser <: AbstractOptParser
  
  protected val logger = Logger.getLogger(commandName)
  
  private val logLayout = new DateLayout() {
        val ignoresThrowable = false
        def format(event:org.apache.log4j.spi.LoggingEvent): String = {
          val calendar: Calendar = Calendar.getInstance
          calendar.setTimeInMillis(event.getTimeStamp)
          val formatter: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          val formattedDate: String = formatter.format(calendar.getTime)
          var logLevel = event.getLevel.toString
          while (logLevel.size < 6) logLevel += " "
          logLevel + " [" + formattedDate + "] [" + event.getLoggerName + "] " + event.getMessage + "\n"
        }
    }
  private val stderrAppender = new WriterAppender(logLayout, sys.process.stderr)
  logger.setLevel(org.apache.log4j.Level.INFO)
  logger.addAppender(stderrAppender)
}