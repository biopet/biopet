package nl.lumc.sasc.biopet.core

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.function.CommandLineFunction

/**
 * Created by pjvanthof on 01/10/15.
 */
trait CommandLineResources extends CommandLineFunction with Configurable {

  def defaultThreads = 1
  final def threads = nCoresRequest match {
    case Some(i) => i
    case _ => {
      val t = getThreads
      nCoresRequest = Some(t)
      t
    }
  }

  var vmem: Option[String] = config("vmem")
  def defaultCoreMemory: Double = 1.0
  def defaultVmemFactor: Double = 1.4
  var vmemFactor: Double = config("vmem_factor", default = defaultVmemFactor)

  var residentFactor: Double = config("resident_factor", default = 1.2)

  private var _coreMemory: Double = 2.0
  def coreMemeory = _coreMemory

  var retry = 0

  override def freezeFieldValues(): Unit = {
    setResources()
    if (vmem.isDefined) jobResourceRequests :+= "h_vmem=" + vmem.get
    super.freezeFieldValues()
  }

  def getThreads: Int = getThreads(defaultThreads)

  /**
   * Get threads from config
   * @param default default when not found in config
   * @return number of threads
   */
  private def getThreads(default: Int): Int = {
    val maxThreads: Int = config("maxthreads", default = 24)
    val threads: Int = config("threads", default = default)
    if (maxThreads > threads) threads
    else maxThreads
  }

  def setResources(): Unit = {
    val firstOutput = try {
      this.firstOutput
    } catch {
      case e: NullPointerException => null
    }

    if (jobOutputFile == null && firstOutput != null)
      jobOutputFile = new File(firstOutput.getAbsoluteFile.getParent, "." + firstOutput.getName + "." + configName + ".out")

    nCoresRequest = Option(threads)

    _coreMemory = config("core_memory", default = defaultCoreMemory).asDouble +
      (0.5 * retry)

    if (config.contains("memory_limit")) memoryLimit = config("memory_limit")
    else memoryLimit = Some(_coreMemory * threads)

    if (config.contains("resident_limit")) residentLimit = config("resident_limit")
    else residentLimit = Some((_coreMemory + (0.5 * retry)) * residentFactor)

    if (!config.contains("vmem")) vmem = Some((_coreMemory * (vmemFactor + (0.5 * retry))) + "G")
    jobName = configName + ":" + (if (firstOutput != null) firstOutput.getName else jobOutputFile)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    if (vmem.isDefined) jobResourceRequests = jobResourceRequests.filterNot(_.contains("h_vmem="))
    logger.info("Auto raise memory on retry")
    retry += 1
    this.freeze()
  }

  var threadsCorrection = 0

  protected def combineResources(commands: List[CommandLineResources]): Unit = {
    commands.foreach(_.setResources())
    nCoresRequest = Some(commands.map(_.threads).sum + threadsCorrection)

    _coreMemory = commands.map(cmd => cmd.coreMemeory * (cmd.threads.toDouble / threads.toDouble)).sum
    memoryLimit = Some(_coreMemory * threads)
    residentLimit = Some((_coreMemory + (0.5 * retry)) * residentFactor)
    vmem = Some((_coreMemory * (vmemFactor + (0.5 * retry))) + "G")
  }

}
