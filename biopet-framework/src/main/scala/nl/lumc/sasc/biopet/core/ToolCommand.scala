package nl.lumc.sasc.biopet.core

import java.text.SimpleDateFormat
import java.util.Calendar
import org.apache.log4j.Logger
import org.apache.log4j.WriterAppender
import org.apache.log4j.helpers.DateLayout
import org.apache.log4j.spi.LoggingEvent
import java.text.DateFormat

trait ToolCommand extends MainCommand {
  abstract class AbstractArgs {
  }
  
  abstract class AbstractOptParser extends scopt.OptionParser[Args](getClass.getSimpleName) {
    head(getClass.getSimpleName)
    opt[String]('l', "log") foreach { x =>
        x.toLowerCase match {
          case "debug" => logger.setLevel(org.apache.log4j.Level.DEBUG)
          case "info" => logger.setLevel(org.apache.log4j.Level.INFO)
          case "warn" => logger.setLevel(org.apache.log4j.Level.WARN)
          case "error" => logger.setLevel(org.apache.log4j.Level.ERROR)
          case _ =>
        } } text("Log level") // TODO: add Enum to validation
  }
  
  type Args <: AbstractArgs
  type OptParser <: AbstractOptParser
   
  protected val logger = Logger.getLogger(this.getClass.getSimpleName.stripSuffix("$"))
  logger.setLevel(org.apache.log4j.Level.INFO)
  logger.addAppender(new WriterAppender(new DateLayout() {
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
      }, sys.process.stderr))
}