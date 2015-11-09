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
object WriteDependencies extends Logging with Configurable {
  val root: Configurable = null
  private val functionNames: mutable.Map[QFunction, String] = mutable.Map()

  private def createFunctionNames(functions: Seq[QFunction]): Unit = {
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
    logger.info("Start calculating dependencies")
    createFunctionNames(functions)

    case class QueueFile(file: File) {
      private val inputJobs: ListBuffer[QFunction] = ListBuffer()
      def addInputJob(function: QFunction) = inputJobs += function
      def inputJobNames = inputJobs.toList.map(functionNames)

      private val outputJobs: ListBuffer[QFunction] = ListBuffer()
      def addOutputJob(function: QFunction) = {
        if (outputJobs.nonEmpty) logger.warn(s"File '$file' is found as output of multiple jobs")
        outputJobs += function
      }
      def outputJobNames = outputJobs.toList.map(functionNames)

      def getMap = Map(
        "path" -> file.getAbsolutePath,
        "intermediate" -> isIntermediate,
        "output_jobs" -> outputJobNames,
        "input_jobs" -> inputJobNames,
        "exist_at_start" -> file.exists(),
        "pipeline_input" -> outputJobs.isEmpty
      )

      def isIntermediate = outputJobs.exists(_.isIntermediate)
    }

    val files: mutable.Map[File, QueueFile] = mutable.Map()

    def outputFiles(function: QFunction) = {
      if (function.jobErrorFile == null) function.outputs :+ function.jobOutputFile
      else function.outputs :+ function.jobOutputFile :+ function.jobErrorFile
    }

    for (function <- functions) {
      for (input <- function.inputs) {
        val file = files.getOrElse(input, QueueFile(input))
        file.addInputJob(function)
        files += input -> file
      }
      for (output <- outputFiles(function)) {
        val file = files.getOrElse(output, QueueFile(output))
        file.addOutputJob(function)
        files += output -> file
      }
    }

    val jobs = functionNames.par.map {
      case (f, name) =>
        name -> Map("command" -> (f match {
          case cmd: CommandLineFunction => cmd.commandLine
          case _                        => None
        }), "intermediate" -> f.isIntermediate,
          "depens_on_intermediate" -> f.inputs.exists(files(_).isIntermediate),
          "depens_on_jobs" -> f.inputs.toList.flatMap(files(_).outputJobNames).distinct,
          "ouput_used_by_jobs" -> outputFiles(f).toList.flatMap(files(_).inputJobNames).distinct,
          "outputs" -> outputFiles(f).toList,
          "inputs" -> f.inputs.toList,
          "done_at_start" -> f.isDone,
          "fail_at_start" -> f.isFail)
    }.toIterator.toMap

    logger.info(s"Writing dependencies to: $outputFile")
    val writer = new PrintWriter(outputFile)
    writer.println(ConfigUtils.mapToJson(Map(
      "jobs" -> jobs.toMap,
      "files" -> files.values.par.map(_.getMap).toList
    )).spaces2)
    writer.close()

    logger.info("done calculating dependencies")
  }
}
