package nl.lumc.sasc.biopet.core

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.{ Logging, ConfigUtils }
import org.broadinstitute.gatk.queue.function.{ CommandLineFunction, QFunction }
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * @author Peter van 't Hof <p.j.van_t_hof@lumc.nl>
 */
object WriteDependencies extends Logging {
  val functionNames: mutable.Map[QFunction, String] = mutable.Map()

  def createFunctionNames(functions: Seq[QFunction]): Unit = {
    val cache: mutable.Map[String, Int] = mutable.Map()
    for (function <- functions) {
      val baseName = function match {
        case f: Configurable => f.configName
        case f               => f.getClass.getSimpleName
      }
      cache += baseName -> (cache.getOrElse(baseName, 0) + 1)
      functionNames += function -> s"$baseName-${cache(baseName)}"
    }
  }

  def writeDependencies(functions: Seq[QFunction], outputFile: File): Unit = {
    createFunctionNames(functions)

    case class QueueFile(file: File) {
      private val inputJobs: ListBuffer[QFunction] = ListBuffer()
      def addInputJob(function: QFunction) = inputJobs += function
      private val outputJobs: ListBuffer[QFunction] = ListBuffer()
      def addOutputJob(function: QFunction) = {
        if (outputJobs.nonEmpty) logger.warn(s"File '$file' is found as output of multiple jobs")
        outputJobs += function
      }

      def getMap = Map(
        "path" -> file.getAbsolutePath,
        "intermediate" -> isIntermediate,
        "output_jobs" -> outputJobs.toList.map(functionNames),
        "input_jobs" -> inputJobs.toList.map(functionNames),
        "exist_at_start" -> file.exists(),
        "pipeline_input" -> outputJobs.isEmpty
      )

      def isIntermediate = outputJobs.exists(_.isIntermediate)
    }

    val files: mutable.Map[File, QueueFile] = mutable.Map()

    for (function <- functions) {
      for (input <- function.inputs) {
        val file = files.getOrElse(input, QueueFile(input))
        file.addInputJob(function)
        files += input -> file
      }
      for (output <- function.outputs) {
        val file = files.getOrElse(output, QueueFile(output))
        file.addOutputJob(function)
        files += output -> file
      }
    }

    val jobs = functionNames.map {
      case (f, name) =>
        name -> Map("command" -> (f match {
          case cmd: CommandLineFunction => cmd.commandLine
          case _                        => None
        }), "intermediate" -> f.isIntermediate,
          "depens_on_intermediate" -> f.inputs.exists(files(_).isIntermediate),
          "outputs" -> f.outputs.toList,
          "inputs" -> f.inputs.toList)
    }

    val writer = new PrintWriter(outputFile)
    writer.println(ConfigUtils.mapToJson(Map(
      "jobs" -> jobs.toMap,
      "files" -> files.values.par.map(_.getMap).toList
    )).spaces2)
    writer.close()
  }

}
