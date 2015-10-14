package nl.lumc.sasc.biopet.extensions.manwe

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Output, Argument }

/**
 * Created by ahbbollen on 23-9-15.
 * This is python, but not accessed like a script; i.e. called by simply
 * manwe [subcommand]
 */
abstract class Manwe extends BiopetCommandLineFunction {
  executable = config("exe", default = "manwe", submodule = "manwe")

  override def defaultCoreMemory = 2.0
  override def defaultThreads = 1

  @Argument(doc = "Path to manwe config file containing your database settings")
  var manweConfig: File = createManweConfig

  @Output(doc = "the output file")
  var output: File = _

  var manweHelp: Boolean = false

  def subCommand: String

  final def cmdLine = {
    required(executable) +
      subCommand +
      required("-c", manweConfig) +
      conditional(manweHelp, "-h") +
      " > " +
      required(output)
  }

  /**
   * Convert cmdLine into line without quotes and double spaces
   * primarily for testing
   * @return
   */
  final def cmd = {
    val a = cmdLine
    a.replace("'", "").replace("  ", " ").trim
  }

  /**
   * Create Manwe config from biopet config
   * @return Manwe config file
   */
  def createManweConfig: File = {
    val url: String = config("varda_root")
    val token: String = config("varda_token")
    val sslSettings: Option[String] = config("varda_verify_certificate")
    val collectionCacheSize: Option[Int] = config("varda_cache_size", default = 20)
    val dataBufferSize: Option[Int] = config("varda_buffer_size", default = 1024 * 1024)
    val taskPollWait: Option[Int] = config("varda_task_poll_wait", default = 2)

    val urlString = s"API_ROOT = '${url.toString}'"
    val tokenString = s"TOKEN = '${token.toString}'"
    val sslSettingString = sslSettings match {
      case Some("true") => "VERIFY_CERTIFICATE = True"
      case Some("false") => "VERIFY_CERTIFICATE = False"
      case Some(x) => s"VERIFY_CERTIFICATE = '$x"
      case _ => "VERIFY_CERTIFICATE = True"
    }

    val collectionString = s"COLLECTION_CACHE_SIZE = ${collectionCacheSize.getOrElse(20)}"
    val dataString = s"DATA_BUFFER_SIZE = ${dataBufferSize.getOrElse(1048576)}"
    val taskString = s"TASK_POLL_WAIT = ${taskPollWait.getOrElse(2)}"

    val file = File.createTempFile("manwe_config", ".py")
    val writer = new PrintWriter(file)
    writer.println(urlString)
    writer.println(tokenString)
    writer.println(sslSettingString)
    writer.println(collectionString)
    writer.println(dataString)
    writer.println(taskString)

    writer.close()
    file
  }
}
