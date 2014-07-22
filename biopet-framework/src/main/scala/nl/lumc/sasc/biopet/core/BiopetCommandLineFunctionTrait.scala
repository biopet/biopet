package nl.lumc.sasc.biopet.core

import java.io.File
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.gatk.queue.QException
import org.broadinstitute.gatk.queue.function.CommandLineFunction
import org.broadinstitute.gatk.utils.commandline._
import scala.sys.process._
import scala.util.matching.Regex

trait BiopetCommandLineFunctionTrait extends CommandLineFunction with Configurable {
  analysisName = getClass.getSimpleName
  
  @Input(doc="deps", required=false)
  var deps: List[File] = Nil
  
  @Argument(doc="Threads", required=false)
  var threads = 0
  val defaultThreads = 1
  
  @Argument(doc="Vmem", required=false)
  var vmem: String = _
  val defaultVmem: String = ""
  
  @Argument(doc="Executable")
  var executable: String = _
  
  protected def beforeCmd {
  }
  
  protected def afterGraph {
  }
  
  override def freezeFieldValues() {
    checkExecutable
    afterGraph
    jobOutputFile = new File(firstOutput.getParent + "/."  + firstOutput.getName + "." + analysisName + ".out")
    
    if (threads == 0) threads = getThreads(defaultThreads)
    if (threads > 1) nCoresRequest = Option(threads)
    
    if (vmem == null) {
      vmem = config("vmem")
      if (vmem == null && !defaultVmem.isEmpty) vmem = defaultVmem
    }
    if (vmem != null) jobResourceRequests :+= "h_vmem=" + vmem
    jobName = this.analysisName + ":" + firstOutput.getName
    
    super.freezeFieldValues()
  }
  
  protected def checkExecutable {
    try if (executable != null) {
      val buffer = new StringBuffer()
      val cmd = Seq("which", executable)
      val process = Process(cmd).run(ProcessLogger(buffer.append(_)))
      if (process.exitValue == 0) {
        executable = buffer.toString
        val file = new File(executable)
        executable = file.getCanonicalPath
      } else {
        logger.error("executable: '" + executable + "' not found, please check config")
        throw new QException("executable: '" + executable + "' not found, please check config")
      }
    } catch {
      case ioe: java.io.IOException => logger.warn("Could not use 'which', check on executable skipped: " + ioe)
    }
  }
  
  final protected def preCmdInternal {
    checkExecutable
    //for (input <- this.inputs) if (!input.exists) throw new IllegalStateException("Input: " + input + " for " + analysisName + " is missing")
    //logger.debug("Config for " + analysisName + ": " + localConfig)
    
    beforeCmd
    
    addJobReportBinding("cores", if (nCoresRequest.get.toInt > 0) nCoresRequest.get.toInt else 1)
    addJobReportBinding("version", getVersion)
  }
  
  protected def versionCommand: String = null
  protected val versionRegex: Regex = null
  protected val versionExitcode = List(0) // Can select multiple
  def getVersion : String = {
    if (versionCommand == null || versionRegex == null) return "N/A"
    val buffer = new StringBuffer()
    val process = Process(versionCommand).run(ProcessLogger(buffer append _))
    if (!versionExitcode.contains(process.exitValue)) {
      logger.warn("Version command: '" + versionCommand + "' give exit code " + process.exitValue + ", version not found")
      return "N/A"
    }
    val lines = versionCommand lines_! ProcessLogger(buffer append _)
    for (line <- lines) {
      line match { 
        case versionRegex(m) => return m
        case _ =>
      }
    }
    logger.warn("Version command: '" + versionCommand + "' give a exit code " + process.exitValue + " but no version was found, executable oke?")
    return "N/A"
  }
  
  def getThreads(default:Int) : Int = {
    val maxThreads: Int = config("maxthreads", default=8)
    val threads: Int = config("threads", default=default)
    if (maxThreads > threads) return threads
    else return maxThreads
  }
  
  def getThreads(default:Int, module:String) : Int = {
    val maxThreads: Int = config("maxthreads", default=8, submodule=module)
    val threads: Int = config("threads", default=default, submodule=module)
    if (maxThreads > threads) return threads
    else return maxThreads
  }
}
