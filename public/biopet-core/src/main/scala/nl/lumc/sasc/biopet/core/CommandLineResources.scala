package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.function.CommandLineFunction

/**
 * This trait will control resources given to a CommandlineFunction
 */
trait CommandLineResources extends CommandLineFunction with Configurable {

  def defaultThreads = 1
  final def threads = nCoresRequest match {
    case Some(i) => i
    case _ =>
      val t = getThreads
      nCoresRequest = Some(t)
      t
  }

  var vmem: Option[String] = config("vmem")
  def defaultCoreMemory: Double = 2.0
  def defaultVmemFactor: Double = 1.4
  var vmemFactor: Double = config("vmem_factor", default = defaultVmemFactor)

  var residentFactor: Double = config("resident_factor", default = 1.2)

  private var _coreMemory: Double = 2.0
  def coreMemory = _coreMemory

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

    nCoresRequest = Option(threads)

    /** The 1e retry does not yet upgrade the memory */
    val retryMultipler = if (retry > 1) retry - 1 else 0

    _coreMemory = config("core_memory", default = defaultCoreMemory).asDouble +
      (0.5 * retryMultipler)

    if (config.contains("memory_limit")) memoryLimit = config("memory_limit")
    else memoryLimit = Some(_coreMemory * threads)

    if (config.contains("resident_limit")) residentLimit = config("resident_limit")
    else residentLimit = Some((_coreMemory + (0.5 * retryMultipler)) * residentFactor)

    if (!config.contains("vmem")) vmem = Some((_coreMemory * (vmemFactor + (0.5 * retryMultipler))) + "G")
    jobName = configName + ":" + (if (firstOutput != null) firstOutput.getName else jobOutputFile)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    if (vmem.isDefined) jobResourceRequests = jobResourceRequests.filterNot(_.contains("h_vmem="))
    if (retry > 0) logger.info("Auto raise memory on retry")
    retry += 1
    this.freeze()
  }

  var threadsCorrection = 0

  protected def combineResources(commands: List[CommandLineResources]): Unit = {
    commands.foreach(_.setResources())
    nCoresRequest = Some(commands.map(_.threads).sum + threadsCorrection)

    _coreMemory = commands.map(cmd => cmd.coreMemory * (cmd.threads.toDouble / threads.toDouble)).sum
    memoryLimit = Some(_coreMemory * threads)
    residentLimit = Some((_coreMemory + (0.5 * retry)) * residentFactor)
    vmem = Some((_coreMemory * (vmemFactor + (0.5 * retry))) + "G")
  }

}