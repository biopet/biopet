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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.core

import java.io.{ PrintWriter, File, FileInputStream }
import java.security.MessageDigest

import nl.lumc.sasc.biopet.utils.Logging
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }
import org.broadinstitute.gatk.utils.runtime.ProcessSettings
import org.ggf.drmaa.JobTemplate

import scala.collection.mutable
import scala.io.Source
import scala.sys.process.{ Process, ProcessLogger }
import scala.collection.JavaConversions._

/** Biopet command line trait to auto check executable and cluster values */
trait BiopetCommandLineFunction extends CommandLineResources { biopetFunction =>
  analysisName = configName

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  @Output
  var outputFiles: List[File] = Nil

  var executable: String = _

  /** This is the default shell for drmaa jobs */
  def defaultRemoteCommand = "bash"
  private val remoteCommand: String = config("remote_command", default = defaultRemoteCommand)

  private def changeScript(file: File): Unit = {
    val lines = Source.fromFile(file).getLines().toList
    val writer = new PrintWriter(file)
    writer.println("set -eubf")
    writer.println("set -o pipefail")
    lines.foreach(writer.println)
    writer.close()
  }

  // This overrides the default "sh" from queue. For Biopet the default is "bash"
  updateJobRun = {
    case jt: JobTemplate =>
      changeScript(new File(jt.getArgs.head.toString))
      jt.setRemoteCommand(remoteCommand)
    case ps: ProcessSettings =>
      changeScript(new File(ps.getCommand.tail.head))
      ps.setCommand(Array(remoteCommand) ++ ps.getCommand.tail)
  }

  /**
   * Can override this method. This is executed just before the job is ready to run.
   * Can check on run time files from pipeline here
   */
  def beforeCmd() {}

  /** Can override this method. This is executed after the script is done en queue starts to generate the graph */
  def beforeGraph() {}

  override def freezeFieldValues() {
    preProcessExecutable()
    beforeGraph()
    internalBeforeGraph()

    super.freezeFieldValues()
  }

  /** Set default output file, threads and vmem for current job */
  final def internalBeforeGraph(): Unit = {

    pipesJobs.foreach(_.beforeGraph())
    pipesJobs.foreach(_.internalBeforeGraph())

  }

  /**
   * Can override this value is executable may not be converted to CanonicalPath
   * @deprecated
   */
  val executableToCanonicalPath = true

  /**
   * Checks executable. Follow full CanonicalPath, checks if it is existing and do a md5sum on it to store in job report
   */
  protected[core] def preProcessExecutable() {
    val exe = BiopetCommandLineFunction.preProcessExecutable(executable)
    exe.path.foreach(executable = _)
    addJobReportBinding("md5sum_exe", exe.md5.getOrElse("N/A"))
  }

  /** executes checkExecutable method and fill job report */
  final protected def preCmdInternal() {
    preProcessExecutable()
    beforeCmd()

    addJobReportBinding("cores", nCoresRequest match {
      case Some(n) if n > 0 => n
      case _                => 1
    })
  }

  private[core] var _inputAsStdin = false
  def inputAsStdin = _inputAsStdin
  private[core] var _outputAsStdout = false
  def outputAsStsout = _outputAsStdout

  /**
   * This operator sends stdout to `that` and combine this into 1 command line function
   * @param that Function that will read from stdin
   * @return BiopetPipe function
   */
  def |(that: BiopetCommandLineFunction): BiopetCommandLineFunction = {
    this._outputAsStdout = true
    that._inputAsStdin = true
    this.beforeGraph()
    this.internalBeforeGraph()
    that.beforeGraph()
    that.internalBeforeGraph()
    this match {
      case p: BiopetPipe =>
        p.commands.last._outputAsStdout = true
        new BiopetPipe(p.commands ::: that :: Nil)
      case _ => new BiopetPipe(List(this, that))
    }
  }

  /**
   * This operator can be used to give a program a file as stdin
   * @param file File that will become stdin for this program
   * @return It's own class
   */
  def :<:(file: File): BiopetCommandLineFunction = {
    this._inputAsStdin = true
    this.stdinFile = Some(file)
    this
  }

  /**
   * This operator can be used to give a program a file write it's atdout
   * @param file File that will become stdout for this program
   * @return It's own class
   */
  def >(file: File): BiopetCommandLineFunction = {
    this._outputAsStdout = true
    this.stdoutFile = Some(file)
    this
  }

  @Output(required = false)
  private[core] var stdoutFile: Option[File] = None

  @Input(required = false)
  private[core] var stdinFile: Option[File] = None

  /**
   * This function needs to be implemented to define the command that is executed
   * @return Command to run
   */
  protected[core] def cmdLine: String

  /**
   * implementing a final version of the commandLine from org.broadinstitute.gatk.queue.function.CommandLineFunction
   * User needs to implement cmdLine instead
   * @return Command to run
   */
  override final def commandLine: String = {
    preCmdInternal()
    val cmd = cmdLine +
      stdinFile.map(file => " < " + required(file.getAbsoluteFile)).getOrElse("") +
      stdoutFile.map(file => " > " + required(file.getAbsoluteFile)).getOrElse("")
    addJobReportBinding("command", cmd)
    cmd
  }

  private[core] var pipesJobs: List[BiopetCommandLineFunction] = Nil
  def addPipeJob(job: BiopetCommandLineFunction) {
    pipesJobs :+= job
    pipesJobs = pipesJobs.distinct
  }
}

/** stores global caches */
object BiopetCommandLineFunction extends Logging {
  private[core] val executableMd5Cache: mutable.Map[String, String] = mutable.Map()
  private[core] val executableCache: mutable.Map[String, String] = mutable.Map()

  case class Executable(path: Option[String], md5: Option[String])
  def preProcessExecutable(executable: String): Executable = {
    if (!BiopetCommandLineFunction.executableMd5Cache.contains(executable)) {
      if (executable != null) {
        if (!BiopetCommandLineFunction.executableCache.contains(executable)) {
          try {
            val buffer = new StringBuffer()
            val cmd = Seq("which", executable)
            val process = Process(cmd).run(ProcessLogger(buffer.append(_)))
            if (process.exitValue == 0) {
              val file = new File(buffer.toString)
              BiopetCommandLineFunction.executableCache += executable -> file.getAbsolutePath
            } else {
              Logging.addError("executable: '" + executable + "' not found, please check config")
              BiopetCommandLineFunction.executableCache += executable -> executable
            }
          } catch {
            case ioe: java.io.IOException =>
              logger.warn(s"Could not use 'which' on '$executable', check on executable skipped: " + ioe)
              BiopetCommandLineFunction.executableCache += executable -> executable
          }
        }

        if (!BiopetCommandLineFunction.executableMd5Cache.contains(executable)) {
          val newExe = BiopetCommandLineFunction.executableCache(executable)
          if (new File(newExe).exists()) {
            val is = new FileInputStream(newExe)
            val cnt = is.available
            val bytes = Array.ofDim[Byte](cnt)
            is.read(bytes)
            is.close()
            val temp = MessageDigest.getInstance("MD5").digest(bytes).map("%02X".format(_)).mkString.toLowerCase
            BiopetCommandLineFunction.executableMd5Cache += newExe -> temp
          } else BiopetCommandLineFunction.executableMd5Cache += newExe -> "file_does_not_exist"
        }
      }
    }
    Executable(BiopetCommandLineFunction.executableCache.get(executable),
      BiopetCommandLineFunction.executableMd5Cache.get(executable))
  }
}