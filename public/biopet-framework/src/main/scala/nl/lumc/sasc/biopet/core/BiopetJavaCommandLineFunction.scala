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

trait BiopetJavaCommandLineFunction extends JavaCommandLineFunction with BiopetCommandLineFunctionTrait {
  executable = "java"

  javaGCThreads = config("java_gc_threads")
  javaGCHeapFreeLimit = config("java_gc_heap_freelimit")
  javaGCTimeLimit = config("java_gc_timelimit")

  override def javaOpts = super.javaOpts + optional("-Dscala.concurrent.context.numThreads=", threads, spaceSeparated = false, escape = false)

  override def afterGraph {
    memoryLimit = config("memory_limit")
  }

  /**
   * Creates command to execute extension
   * @return
   */
  override def commandLine: String = {
    preCmdInternal
    val cmd = super.commandLine
    val finalCmd = executable + cmd.substring(cmd.indexOf(" "))
    //    addJobReportBinding("command", cmd)
    return cmd
  }
}
