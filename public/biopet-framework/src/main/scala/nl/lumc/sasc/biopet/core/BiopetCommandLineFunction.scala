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

import java.io.{ File, FileInputStream }
import java.security.MessageDigest

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

import scala.collection.mutable
import scala.sys.process.{ Process, ProcessLogger }
import scala.util.matching.Regex

/** Biopet command line trait to auto check executable and cluster values */
trait BiopetCommandLineFunction extends CommandLineFunction with Configurable { biopetFunction =>
  analysisName = configName

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  var threads = 0
  def defaultThreads = 1

  var vmem: Option[String] = config("vmem")
  protected def defaultCoreMemory: Double = 1.0
  protected def defaultVmemFactor: Double = 1.4
  var vmemFactor: Double = config("vmem_factor", default = defaultVmemFactor)

  var residentFactor: Double = config("resident_factor", default = 1.2)

  private var coreMemory: Double = _

  var executable: String = _

  /**
   * Can override this method. This is executed just before the job is ready to run.
   * Can check on run time files from pipeline here
   */
  protected[core] def beforeCmd() {}

  /** Can override this method. This is executed after the script is done en queue starts to generate the graph */
  def beforeGraph() {}

  /** Set default output file, threads and vmem for current job */
  override def freezeFieldValues() {
    preProcessExecutable()
    beforeGraph()
    if (jobOutputFile == null) jobOutputFile = new File(firstOutput.getAbsoluteFile.getParent, "." + firstOutput.getName + "." + configName + ".out")

    if (threads == 0) threads = getThreads(defaultThreads)
    if (threads > 1) nCoresRequest = Option(threads)

    coreMemory = config("core_memory", default = defaultCoreMemory).asDouble + (0.5 * retry)

    if (config.contains("memory_limit")) memoryLimit = config("memory_limit")
    else memoryLimit = Some(coreMemory * threads)

    if (config.contains("resident_limit")) residentLimit = config("resident_limit")
    else residentLimit = Some((coreMemory + (0.5 * retry)) * residentFactor)

    if (!config.contains("vmem")) vmem = Some((coreMemory * (vmemFactor + (0.5 * retry))) + "G")
    if (vmem.isDefined) jobResourceRequests :+= "h_vmem=" + vmem.get
    jobName = configName + ":" + (if (firstOutput != null) firstOutput.getName else jobOutputFile)

    super.freezeFieldValues()
  }

  var retry = 0

  override def setupRetry(): Unit = {
    super.setupRetry()
    if (vmem.isDefined) jobResourceRequests = jobResourceRequests.filterNot(_.contains("h_vmem="))
    logger.info("Auto raise memory on retry")
    retry += 1
    this.freeze()
  }

  /** can override this value is executable may not be converted to CanonicalPath */
  val executableToCanonicalPath = true

  /**
   * Checks executable. Follow full CanonicalPath, checks if it is existing and do a md5sum on it to store in job report
   */
  protected[core] def preProcessExecutable() {
    if (!BiopetCommandLineFunction.executableMd5Cache.contains(executable)) {
      try if (executable != null) {
        if (!BiopetCommandLineFunction.executableCache.contains(executable)) {
          val oldExecutable = executable
          val buffer = new StringBuffer()
          val cmd = Seq("which", executable)
          val process = Process(cmd).run(ProcessLogger(buffer.append(_)))
          if (process.exitValue == 0) {
            executable = buffer.toString
            val file = new File(executable)
            if (executableToCanonicalPath) executable = file.getCanonicalPath
            else executable = file.getAbsolutePath
          } else {
            BiopetQScript.addError("executable: '" + executable + "' not found, please check config")
          }
          BiopetCommandLineFunction.executableCache += oldExecutable -> executable
          BiopetCommandLineFunction.executableCache += executable -> executable
        } else {
          executable = BiopetCommandLineFunction.executableCache(executable)
        }

        if (!BiopetCommandLineFunction.executableMd5Cache.contains(executable)) {
          val is = new FileInputStream(executable)
          val cnt = is.available
          val bytes = Array.ofDim[Byte](cnt)
          is.read(bytes)
          is.close()
          val temp = MessageDigest.getInstance("MD5").digest(bytes).map("%02X".format(_)).mkString.toLowerCase
          BiopetCommandLineFunction.executableMd5Cache += executable -> temp
        }
      } catch {
        case ioe: java.io.IOException => logger.warn("Could not use 'which', check on executable skipped: " + ioe)
      }
    }
    val md5 = BiopetCommandLineFunction.executableMd5Cache.get(executable)
    addJobReportBinding("md5sum_exe", md5.getOrElse("None"))
  }

  /** executes checkExecutable method and fill job report */
  final protected def preCmdInternal() {
    preProcessExecutable()
    beforeCmd()

    addJobReportBinding("cores", nCoresRequest match {
      case Some(n) if n > 0 => n
      case _                => 1
    })
    addJobReportBinding("version", getVersion)
  }

  /** Command to get version of executable */
  protected def versionCommand: String = null

  /** Regex to get version from version command output */
  protected def versionRegex: Regex = null

  /** Allowed exit codes for the version command */
  protected def versionExitcode = List(0)

  /** Executes the version command */
  private[core] def getVersionInternal: Option[String] = {
    if (versionCommand == null || versionRegex == null) None
    else getVersionInternal(versionCommand, versionRegex)
  }

  /** Executes the version command */
  private[core] def getVersionInternal(versionCommand: String, versionRegex: Regex): Option[String] = {
    if (versionCommand == null || versionRegex == null) return None
    val exe = new File(versionCommand.trim.split(" ")(0))
    if (!exe.exists()) return None
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

  /** Get version from cache otherwise execute the version command  */
  def getVersion: Option[String] = {
    if (!BiopetCommandLineFunction.executableCache.contains(executable))
      preProcessExecutable()
    if (!BiopetCommandLineFunction.versionCache.contains(versionCommand))
      getVersionInternal match {
        case Some(version) => BiopetCommandLineFunction.versionCache += versionCommand -> version
        case _             =>
      }
    BiopetCommandLineFunction.versionCache.get(versionCommand)
  }

  def getThreads: Int = getThreads(defaultThreads)

  /**
   * Get threads from config
   * @param default default when not found in config
   * @return number of threads
   */
  def getThreads(default: Int): Int = {
    val maxThreads: Int = config("maxthreads", default = 24)
    val threads: Int = config("threads", default = default)
    if (maxThreads > threads) threads
    else maxThreads
  }

  /**
   * Get threads from config
   * @param default default when not found in config
   * @param module Module when this is difrent from default
   * @return number of threads
   */
  def getThreads(default: Int, module: String): Int = {
    val maxThreads: Int = config("maxthreads", default = 24, submodule = module)
    val threads: Int = config("threads", default = default, submodule = module)
    if (maxThreads > threads) threads
    else maxThreads
  }

  private[core] var _inputAsStdin = false
  def inputAsStdin = _inputAsStdin
  private[core] var _outputAsStdout = false
  def outputAsStsout = _outputAsStdout

  def |(that: BiopetCommandLineFunction): BiopetCommandLineFunction = {
    this._outputAsStdout = true
    that._inputAsStdin = true
    this match {
      case p: BiopetPipe => {
        p.commands.last._outputAsStdout = true
        new BiopetPipe(p.commands ::: that :: Nil)
      }
      case _ => new BiopetPipe(List(this, that))
    }
  }

  def :<:(file: File): BiopetCommandLineFunction = {
    this._inputAsStdin = true
    this.stdinFile = Some(file)
    this
  }

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
}

/** stores global caches */
object BiopetCommandLineFunction {
  private[core] val versionCache: mutable.Map[String, String] = mutable.Map()
  private[core] val executableMd5Cache: mutable.Map[String, String] = mutable.Map()
  private[core] val executableCache: mutable.Map[String, String] = mutable.Map()
}
