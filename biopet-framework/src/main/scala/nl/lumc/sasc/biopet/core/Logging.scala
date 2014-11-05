package nl.lumc.sasc.biopet.core

import java.text.SimpleDateFormat
import java.util.Calendar
import org.apache.log4j.Logger
import org.apache.log4j.WriterAppender
import org.apache.log4j.helpers.DateLayout

trait Logging {
  def logger = Logging.logger
}

object Logging {
  val logger = Logger.getLogger("Logging")

  val logLayout = new DateLayout() {
    val ignoresThrowable = false
    def format(event: org.apache.log4j.spi.LoggingEvent): String = {
      val calendar: Calendar = Calendar.getInstance
      calendar.setTimeInMillis(event.getTimeStamp)
      val formatter: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      val formattedDate: String = formatter.format(calendar.getTime)
      var logLevel = event.getLevel.toString
      while (logLevel.size < 6) logLevel += " "
      val className = event.getLocationInformation.getClassName.split("\\.").last.split("\\$").head
      logLevel + " [" + formattedDate + "] [" + className + "] " + event.getMessage + "\n"
    }
  }

  val stderrAppender = new WriterAppender(logLayout, sys.process.stderr)

  logger.setLevel(org.apache.log4j.Level.INFO)
  logger.addAppender(Logging.stderrAppender)
}