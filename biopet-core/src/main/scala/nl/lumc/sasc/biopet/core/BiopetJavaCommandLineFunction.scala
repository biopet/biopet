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

import org.broadinstitute.gatk.queue.function.JavaCommandLineFunction

/** Biopet commandline class for java based programs */
trait BiopetJavaCommandLineFunction
    extends JavaCommandLineFunction
    with BiopetCommandLineFunction {
  executable = config("java", default = "java", namespace = "java", freeVar = false)

  javaGCThreads = config("java_gc_threads", default = 4)
  javaGCHeapFreeLimit = config("java_gc_heap_freelimit", default = 10)
  javaGCTimeLimit = config("java_gc_timelimit", default = 50)

  override def defaultResidentFactor: Double = 1.5
  override def defaultVmemFactor: Double = 2.0

  /** Constructs java opts, this adds scala threads */
  override def javaOpts: String =
    super.javaOpts +
      optional("-Dscala.concurrent.context.numThreads=", threads, spaceSeparated = false) +
      optional("-Dscala.concurrent.context.maxThreads=", threads, spaceSeparated = false)

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    setResources()
    if (javaMemoryLimit.isEmpty && memoryLimit.isDefined)
      javaMemoryLimit = memoryLimit

    if (javaMainClass != null && javaClasspath.isEmpty)
      javaClasspath = JavaCommandLineFunction.currentClasspath
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
    if (!Version.versionCache.contains(javaVersionCommand))
      Version.getVersionInternal(javaVersionCommand, """java version "(.*)"""".r :: Nil) match {
        case Some(version) => Version.versionCache += javaVersionCommand -> version
        case _ =>
      }
    Version.versionCache.get(javaVersionCommand)
  }

  override def setupRetry(): Unit = {
    super.setupRetry()
    javaMemoryLimit = memoryLimit
  }
}
