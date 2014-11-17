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

  override def commandLine: String = {
    preCmdInternal
    val cmd = super.commandLine
    val finalCmd = executable + cmd.substring(cmd.indexOf(" "))
    //    addJobReportBinding("command", cmd)
    return cmd
  }
}
