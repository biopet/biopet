package nl.lumc.sasc.biopet.core

import java.io.File
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.sting.queue.QException
import org.broadinstitute.sting.queue.function.CommandLineFunction
import org.broadinstitute.sting.commandline._
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
  
  @Argument(doc="Executeble")
  var executeble: String = _
  
  protected def beforeCmd {
  }
  
  protected def afterGraph {
  }
  
  override def freezeFieldValues() {
    checkExecuteble
    afterGraph
    jobOutputFile = new File(firstOutput.getParent + "/."  + firstOutput.getName + "." + analysisName + ".out")
    
    super.freezeFieldValues()
  }
  
  protected def checkExecuteble {
    try if (executeble != null) {
      val buffer = new StringBuffer()
      val cmd = Seq("which", executeble)
      val process = Process(cmd).run(ProcessLogger(buffer.append(_)))
      if (process.exitValue == 0) {
        executeble = buffer.toString
        val file = new File(executeble)
        executeble = file.getCanonicalPath
      } else {
        logger.error("executeble: '" + executeble + "' not found, please check config")
        throw new QException("executeble: '" + executeble + "' not found, please check config")
      }
    } catch {
      case ioe: java.io.IOException => logger.warn("Could not use 'which', check on executeble skipped: " + ioe)
    }
  }
  
  final protected def preCmdInternal {
    checkExecuteble
    //for (input <- this.inputs) if (!input.exists) throw new IllegalStateException("Input: " + input + " for " + analysisName + " is missing")
    //logger.debug("Config for " + analysisName + ": " + localConfig)
    
    beforeCmd
    
    addJobReportBinding("version", getVersion)
    
    if (threads == 0) threads = getThreads(defaultThreads)
    if (threads > 1) nCoresRequest = Option(threads)
    addJobReportBinding("cores", if (nCoresRequest.get.toInt > 0) nCoresRequest.get.toInt else 1)
    
    if (vmem == null) {
      if (configContains("vmem")) vmem = config("vmem")
      else if (!defaultVmem.isEmpty) vmem = defaultVmem
    }
    if (vmem != null) jobResourceRequests :+= "h_vmem=" + vmem
    jobName = this.analysisName + ":" + firstOutput.getName
  }
  
  protected var versionCommand: String = _
  protected val versionRegex: Regex = null
  def getVersion : String = {
    if (versionCommand == null || versionRegex == null) return "N/A"
    val buffer = new StringBuffer()
    val process = Process(versionCommand).run(ProcessLogger(buffer append _))
    if (process.exitValue != 0) {
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
    logger.warn("Version command: '" + versionCommand + "' give a exit code 0 but no version was found, executeble oke?")
    return "N/A"
  }
  
  def getThreads(default:Int) : Int = {
    val maxThreads: Int = config("maxthreads", 8)
    val threads: Int = config("threads", default)
    if (maxThreads > threads) return threads
    else return maxThreads
  }
  
  def getThreads(default:Int, module:String) : Int = {
    val maxThreads: Int = config("maxthreads", 8, module)
    val threads: Int = config("threads", default, module)
    if (maxThreads > threads) return threads
    else return maxThreads
  }
}
