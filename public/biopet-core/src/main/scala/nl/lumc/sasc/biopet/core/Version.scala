package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.utils.Logging
import org.broadinstitute.gatk.queue.function.QFunction

import scala.collection.mutable
import scala.sys.process.{ Process, ProcessLogger }
import scala.util.matching.Regex

/**
 * Created by pjvan_thof on 10/13/15.
 */
trait Version extends QFunction {
  /** Command to get version of executable */
  def versionCommand: String

  /** Regex to get version from version command output */
  def versionRegex: Regex

  /** Allowed exit codes for the version command */
  protected[core] def versionExitcode = List(0)

  /** Executes the version command */
  private[core] def getVersionInternal: Option[String] = {
    if (versionCommand == null || versionRegex == null) None
    else Version.getVersionInternal(versionCommand, versionRegex, versionExitcode)
  }

  /** Get version from cache otherwise execute the version command  */
  def getVersion: Option[String] = {
    if (!Version.versionCache.contains(versionCommand))
      getVersionInternal match {
        case Some(version) => Version.versionCache += versionCommand -> version
        case _             =>
      }
    Version.versionCache.get(versionCommand)
  }

  override def freezeFieldValues(): Unit = {
    super.freezeFieldValues()
    addJobReportBinding("version", getVersion.getOrElse("NA"))
  }
}

object Version extends Logging {
  private[core] val versionCache: mutable.Map[String, String] = mutable.Map()

  /** Executes the version command */
  private[core] def getVersionInternal(versionCommand: String,
                                       versionRegex: Regex,
                                       versionExitcode: List[Int] = List(0)): Option[String] = {
    if (versionCache.contains(versionCommand)) return versionCache.get(versionCommand)
    else if (versionCommand == null || versionRegex == null) return None
    else {
      val stdout = new StringBuffer()
      val stderr = new StringBuffer()
      def outputLog = "Version command: \n" + versionCommand +
        "\n output log: \n stdout: \n" + stdout.toString +
        "\n stderr: \n" + stderr.toString
      val process = Process(versionCommand).run(ProcessLogger(stdout append _ + "\n", stderr append _ + "\n"))
      if (!versionExitcode.contains(process.exitValue())) {
        logger.warn("getVersion give exit code " + process.exitValue + ", version not found \n" + outputLog)
        return None
      }
      for (line <- stdout.toString.split("\n") ++ stderr.toString.split("\n")) {
        line match {
          case versionRegex(m) => return Some(m)
          case _               =>
        }
      }
      logger.warn("getVersion give a exit code " + process.exitValue + " but no version was found, executable correct? \n" + outputLog)
      None
    }
  }

}