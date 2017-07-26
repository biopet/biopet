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
package nl.lumc.sasc.biopet.extensions.manwe

import java.io.{PrintWriter, File}

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{Output, Argument}

/**
  * Created by ahbbollen on 23-9-15.
  * This is python, but not accessed like a script; i.e. called by simply
  * manwe [subcommand]
  */
abstract class Manwe extends BiopetCommandLineFunction {
  executable = config("exe", default = "manwe", namespace = "manwe")

  var manweConfig: File = createManweConfig(None)

  @Output(doc = "the output file")
  var output: File = _

  var manweHelp: Boolean = false

  def subCommand: String

  final def cmdLine: String = {
    manweConfig = createManweConfig(Option(output).map(_.getParentFile))
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
  final def cmd: String = {
    val a = cmdLine
    a.replace("'", "").replace("  ", " ").trim
  }

  /**
    * Create Manwe config from biopet config
    * @return Manwe config file
    */
  def createManweConfig(directory: Option[File]): File = {
    val url: String = config("varda_root")
    val token: String = config("varda_token")
    val sslSettings: Option[String] = config("varda_verify_certificate")
    val collectionCacheSize: Option[Int] = config("varda_cache_size", default = 20)
    val dataBufferSize: Option[Int] = config("varda_buffer_size", default = 1024 * 1024)
    val taskPollWait: Option[Int] = config("varda_task_poll_wait", default = 2)

    val settingsMap: Map[String, Any] = Map(
      "API_ROOT" -> s"'$url'",
      "TOKEN" -> s"'$token'",
      "VERIFY_CERTIFICATE" -> (sslSettings match {
        case Some("true") => "True"
        case Some("false") => "False"
        case Some(x) => s"'$x'"
        case _ => "True"
      }),
      "COLLECTION_CACHE_SIZE" -> collectionCacheSize.getOrElse(20),
      "DATA_BUFFER_SIZE" -> dataBufferSize.getOrElse(1048576),
      "TASK_POLL_WAIT" -> taskPollWait.getOrElse(2)
    )

    val file = directory match {
      case Some(dir) => File.createTempFile("manwe_config", ".py", dir)
      case None => File.createTempFile("manwe_config", ".py")
    }

    file.deleteOnExit()
    val writer = new PrintWriter(file)
    settingsMap.foreach { case (key, value) => writer.println(s"$key = $value") }
    writer.close()
    file
  }
}
