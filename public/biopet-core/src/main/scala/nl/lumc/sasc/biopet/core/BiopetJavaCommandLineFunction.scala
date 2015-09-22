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

import org.broadinstitute.gatk.queue.function.JavaCommandLineFunction

/** Biopet commandline class for java based programs */
trait BiopetJavaCommandLineFunction extends JavaCommandLineFunction with BiopetCommandLineFunction {
  executable = config("java", default = "java", submodule = "java", freeVar = false)

  javaGCThreads = config("java_gc_threads")
  javaGCHeapFreeLimit = config("java_gc_heap_freelimit")
  javaGCTimeLimit = config("java_gc_timelimit")

  override def defaultVmemFactor: Double = 2.0

  /** Constructs java opts, this adds scala threads */
  override def javaOpts = super.javaOpts +
    optional("-Dscala.concurrent.context.numThreads=", threads, spaceSeparated = false, escape = false)

  override def beforeGraph(): Unit = {
    if (javaMemoryLimit.isEmpty && memoryLimit.isDefined)
      javaMemoryLimit = memoryLimit

    if (javaMainClass != null && javaClasspath.isEmpty)
      javaClasspath = JavaCommandLineFunction.currentClasspath

    threads = getThreads(defaultThreads)
  }

  /** Creates command to execute extension */
  def cmdLine: String = {
    preCmdInternal()
    required(executable) +
      javaOpts +
      javaExecutable
  }

  def javaVersionCommand: String = executable + " -version"

  def getJavaVersion: Option[String] = {
    if (!BiopetCommandLineFunction.executableCache.contains(executable))
      preProcessExecutable()
    if (!BiopetCommandLineFunction.versionCache.contains(javaVersionCommand))
      getVersionInternal(javaVersionCommand, """java version "(.*)"""".r) match {
        case Some(version) => BiopetCommandLineFunction.versionCache += javaVersionCommand -> version
        case _             =>
      }
    BiopetCommandLineFunction.versionCache.get(javaVersionCommand)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    javaMemoryLimit = memoryLimit
  }
}
