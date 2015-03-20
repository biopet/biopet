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

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.queue.QException
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }
import scala.sys.process.{ Process, ProcessLogger }
import scala.util.matching.Regex
import java.io.FileInputStream
import java.security.MessageDigest

/** Biopet command line trait to auto check executable and cluster values */
trait BiopetCommandLineFunctionTrait extends CommandLineFunction with Configurable {
  analysisName = configName

  @Input(doc = "deps", required = false)
  var deps: List[File] = Nil

  var threads = 0
  val defaultThreads = 1

  var vmem: Option[String] = config("vmem")
  var defaultCoreMemory: Double = 1.0
  var vmemFactor: Double = config("vmem_factor", default =
    this match {
      case _: BiopetJavaCommandLineFunction => 2.5
      case _                                => 1.5
    })
  var executable: String = _

  /**
   * Can override this method. This is executed just before the job is ready to run.
   * Can check on run time files from pipeline here
   */
  protected[core] def beforeCmd {}

  /** Can override this method. This is executed after the script is done en queue starts to generate the graph */
  protected[core] def beforeGraph {}

  /** Set default output file, threads and vmem for current job */
  override def freezeFieldValues() {
    preProcesExecutable
    beforeGraph
    if (jobOutputFile == null) jobOutputFile = new File(firstOutput.getAbsoluteFile.getParent, "." + firstOutput.getName + "." + configName + ".out")

    if (threads == 0) threads = getThreads(defaultThreads)
    if (threads > 1) nCoresRequest = Option(threads)

    val coreMemory: Double = config("core_memory", default = defaultCoreMemory)
    if (memoryLimit.isEmpty) memoryLimit = Some(coreMemory * threads)

    if (vmem.isEmpty) {
      val vmemTemp = defaultCoreMemory * vmemFactor

      vmem = Some(vmemTemp + "G")
    }
    if (vmem.isDefined) jobResourceRequests :+= "h_vmem=" + vmem.get
    jobName = configName + ":" + (if (firstOutput != null) firstOutput.getName else jobOutputFile)

    super.freezeFieldValues()
  }

  /** can override this value is executable may not be converted to CanonicalPath */
  val executableToCanonicalPath = true

  /**
   * Checks executable. Follow full CanonicalPath, checks if it is existing and do a md5sum on it to store in job report
   */
  protected[core] def preProcesExecutable {
    if (!BiopetCommandLineFunctionTrait.executableMd5Cache.contains(executable)) {
      try if (executable != null) {
        if (!BiopetCommandLineFunctionTrait.executableCache.contains(executable)) {
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
          BiopetCommandLineFunctionTrait.executableCache += oldExecutable -> executable
          BiopetCommandLineFunctionTrait.executableCache += executable -> executable
        } else {
          executable = BiopetCommandLineFunctionTrait.executableCache(executable)
        }

        if (!BiopetCommandLineFunctionTrait.executableMd5Cache.contains(executable)) {
          val is = new FileInputStream(executable)
          val cnt = is.available
          val bytes = Array.ofDim[Byte](cnt)
          is.read(bytes)
          is.close()
          val temp = MessageDigest.getInstance("MD5").digest(bytes).map("%02X".format(_)).mkString.toLowerCase
          BiopetCommandLineFunctionTrait.executableMd5Cache += executable -> temp
        }
      } catch {
        case ioe: java.io.IOException => logger.warn("Could not use 'which', check on executable skipped: " + ioe)
      }
    }
    val md5 = BiopetCommandLineFunctionTrait.executableMd5Cache.get(executable)
    addJobReportBinding("md5sum_exe", md5.getOrElse("None"))
  }

  /** executes checkExecutable method and fill job report */
  final protected def preCmdInternal {
    preProcesExecutable

    beforeCmd

    addJobReportBinding("cores", nCoresRequest match {
      case Some(n) if n > 0 => n
      case _                => 1
    })
    addJobReportBinding("version", getVersion)
  }

  /** Command to get version of executable */
  protected def versionCommand: String = null

  /** Regex to get version from version command output */
  protected val versionRegex: Regex = null

  /** Allowed exit codes for the version command */
  protected val versionExitcode = List(0)

  /** Executes the version command */
  private def getVersionInternal: Option[String] = {
    if (versionCommand == null || versionRegex == null) return None
    val exe = new File(versionCommand.trim.split(" ")(0))
    if (!exe.exists()) return None
    val stdout = new StringBuffer()
    val stderr = new StringBuffer()
    def outputLog = "Version command: \n" + versionCommand +
      "\n output log: \n stdout: \n" + stdout.toString +
      "\n stderr: \n" + stderr.toString
    val process = Process(versionCommand).run(ProcessLogger(stdout append _ + "\n", stderr append _ + "\n"))
    if (!versionExitcode.contains(process.exitValue)) {
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
    return None
  }

  /** Get version from cache otherwise execute the version command  */
  def getVersion: Option[String] = {
    if (!BiopetCommandLineFunctionTrait.executableCache.contains(executable))
      preProcesExecutable
    if (!BiopetCommandLineFunctionTrait.versionCache.contains(versionCommand))
      getVersionInternal match {
        case Some(version) => BiopetCommandLineFunctionTrait.versionCache += versionCommand -> version
        case _             =>
      }
    BiopetCommandLineFunctionTrait.versionCache.get(versionCommand)
  }

  /**
   * Get threads from config
   * @param default default when not found in config
   * @return number of threads
   */
  def getThreads(default: Int): Int = {
    val maxThreads: Int = config("maxthreads", default = 8)
    val threads: Int = config("threads", default = default)
    if (maxThreads > threads) return threads
    else return maxThreads
  }

  /**
   * Get threads from config
   * @param default default when not found in config
   * @param module Module when this is difrent from default
   * @return number of threads
   */
  def getThreads(default: Int, module: String): Int = {
    val maxThreads: Int = config("maxthreads", default = 8, submodule = module)
    val threads: Int = config("threads", default = default, submodule = module)
    if (maxThreads > threads) return threads
    else return maxThreads
  }
}

/** stores global caches */
object BiopetCommandLineFunctionTrait {
  import scala.collection.mutable.Map
  private val versionCache: Map[String, String] = Map()
  private[core] val executableMd5Cache: Map[String, String] = Map()
  private val executableCache: Map[String, String] = Map()
}
