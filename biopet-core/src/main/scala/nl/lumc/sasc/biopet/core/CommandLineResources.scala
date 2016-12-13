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

  val multiplyVmemThreads: Boolean = config("multiply_vmem_threads", default = true)
  val multiplyRssThreads: Boolean = config("multiply_rss_threads", default = true)

  var vmem: Option[String] = config("vmem")
  def defaultCoreMemory: Double = 2.0
  def defaultVmemFactor: Double = 1.4
  def defaultResidentFactor: Double = 1.2
  var vmemFactor: Double = config("vmem_factor", default = defaultVmemFactor)

  val useSge: Boolean = config("use_sge", default = true)

  var residentFactor: Double = config("resident_factor", default = defaultResidentFactor)

  private var _coreMemory: Double = 2.0
  def coreMemory = _coreMemory

  /** This value is for SGE and is defined in seconds */
  wallTime = config("max_walltime_limit")

  var retry = 0

  override def freezeFieldValues(): Unit = {
    setResources()
    if (useSge) {
      vmem.foreach(v => jobResourceRequests :+= s"h_vmem=$v")
      wallTime.foreach(t => jobResourceRequests :+= s"h_rt=$t")
    }
    super.freezeFieldValues()
  }

  def getThreads: Int = getThreads(defaultThreads)

  /**
   * Get threads from config
   * @param default default when not found in config
   * @return number of threads
   */
  private def getThreads(default: Int): Int = {
    val maxThreads: Option[Int] = config("maxthreads")
    val threads: Int = config("threads", default = default)
    maxThreads match {
      case Some(max) => if (max > threads) threads else max
      case _         => threads
    }
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
    else residentLimit = Some((_coreMemory + (0.5 * retryMultipler)) * residentFactor * (if (multiplyRssThreads) threads else 1))

    if (!config.contains("vmem"))
      vmem = Some((_coreMemory * (vmemFactor + (0.5 * retryMultipler)) * (if (multiplyVmemThreads) threads else 1)) + "G")
    jobName = configNamespace + ":" + (if (firstOutput != null) firstOutput.getName else jobOutputFile)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    if (vmem.isDefined) jobResourceRequests = jobResourceRequests.filterNot(_.contains("h_vmem="))
    if (retry > 0) logger.info("Auto raise memory on retry")
    retry += 1
    this.freezeFieldValues()
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
