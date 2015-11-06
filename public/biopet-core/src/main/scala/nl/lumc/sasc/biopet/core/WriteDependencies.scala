package nl.lumc.sasc.biopet.core

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.queue.function.{CommandLineFunction, QFunction}
import scala.collection.mutable

/**
  * Created by pjvanthof on 06/11/15.
  */
object WriteDependencies {
  val functionNames: mutable.Map[QFunction, String] = mutable.Map()

  def createFunctionNames(functions: Seq[QFunction]): Unit = {
    val cache: mutable.Map[String, Int] = mutable.Map()
    for (function <- functions) {
      val baseName = function match {
        case f: BiopetCommandLineFunction => f.configName
        case f => f.getClass.getSimpleName
      }
      cache += baseName -> (cache.getOrElse(baseName, 0) + 1)
      functionNames += function -> (s"$baseName-${cache(baseName)}")
    }
  }

  def writeDependencies(functions: Seq[QFunction], outputFile: File): Unit = {
    createFunctionNames(functions)
    val writer = new PrintWriter(outputFile)
    val jobs = functionNames.map { case (f, name) => name -> Map("command" -> (f match {
      case cmd: CommandLineFunction => cmd.commandLine
      case _ => None
    }), "intermediate" -> f.isIntermediate) }

    writer.println(ConfigUtils.mapToJson(jobs.toMap).spaces2)
    writer.close()
  }

}
