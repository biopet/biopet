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

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.summary.WriteSummary
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.{ ConfigUtils, Logging }
import org.broadinstitute.gatk.queue.function.{ CommandLineFunction, QFunction }

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * This object will generate with [[WriteDependencies.writeDependencies]] a json file where information about job and file dependencies are stored
 *
 * @author Peter van 't Hof <p.j.van_t_hof@lumc.nl>
 */
object WriteDependencies extends Logging with Configurable {
  val root: Configurable = null
  private def createFunctionNames(functions: Seq[QFunction]): Map[QFunction, String] = {
    val cache: mutable.Map[String, Int] = mutable.Map()
    (for (function <- functions) yield {
      val baseName = function match {
        case f: Configurable => f.configNamespace
        case f               => f.getClass.getSimpleName
      }
      cache += baseName -> (cache.getOrElse(baseName, 0) + 1)
      function -> s"${baseName.replaceAll("-", "_")}_${cache(baseName)}"
    }).toMap
  }

  /**
   * This method will generate a json file where information about job and file dependencies are stored
   *
   * @param functions This should be all functions that are given to the graph of Queue
   * @param outputDir
   */
  def writeDependencies(functions: Seq[QFunction], outputDir: File): Unit = {
    outputDir.mkdirs()
    logger.info("Start calculating dependencies")

    val errorOnMissingInput: Boolean = config("error_on_missing_input", false)

    val functionNames = createFunctionNames(functions)

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

      def getMap = {
        val fileExist = file.exists()
        if (!fileExist && outputJobs.isEmpty) {
          if (errorOnMissingInput) Logging.addError(s"Input file does not exist: $file")
          else logger.warn(s"Input file does not exist: $file")
        }
        Map(
          "path" -> file.getAbsolutePath,
          "intermediate" -> isIntermediate,
          "output_jobs" -> outputJobNames,
          "input_jobs" -> inputJobNames,
          "exists_at_start" -> fileExist,
          "pipeline_input" -> outputJobs.isEmpty
        )
      }

      def isIntermediate = outputJobs.exists(_.isIntermediate)
    }

    val files: mutable.Map[File, QueueFile] = mutable.Map()

    for (function <- functions) {
      for (input <- BiopetQScript.safeInputs(function).getOrElse(Seq())) {
        val file = files.getOrElse(input, QueueFile(input))
        file.addInputJob(function)
        files += input -> file
      }
      for (output <- BiopetQScript.safeOutputs(function).getOrElse(Seq())) {
        val file = files.getOrElse(output, QueueFile(output))
        file.addOutputJob(function)
        files += output -> file
      }
      val file = files.getOrElse(function.jobOutputFile, QueueFile(function.jobOutputFile))
      file.addOutputJob(function)
      files += function.jobOutputFile -> file
    }

    val jobs = functionNames.par.map {
      case (f, name) =>
        name.toString -> Map("command" -> (f match {
          case cmd: CommandLineFunction => cmd.commandLine
          case _                        => None
        }), "main_job" -> (f match {
          case cmd: BiopetCommandLineFunction            => cmd.mainFunction
          case s: WriteSummary if s.qscript.root == null => true
          case _                                         => false
        }), "intermediate" -> f.isIntermediate,
          "depends_on_intermediate" -> BiopetQScript.safeOutputs(f).getOrElse(Seq()).exists(files(_).isIntermediate),
          "depends_on_jobs" -> BiopetQScript.safeInputs(f).getOrElse(Seq()).toList.flatMap(files(_).outputJobNames).distinct,
          "output_used_by_jobs" -> BiopetQScript.safeOutputs(f).getOrElse(Seq()).toList.flatMap(files(_).inputJobNames).distinct,
          "outputs" -> (f.jobOutputFile :: BiopetQScript.safeOutputs(f).getOrElse(Seq()).toList),
          "inputs" -> BiopetQScript.safeOutputs(f).getOrElse(Seq()).toList,
          "done_files" -> BiopetQScript.safeDoneFiles(f).getOrElse(Seq()).toList,
          "fail_files" -> BiopetQScript.safeFailFiles(f).getOrElse(Seq()).toList,
          "stdout_file" -> f.jobOutputFile,
          "done_at_start" -> BiopetQScript.safeIsDone(f),
          "fail_at_start" -> BiopetQScript.safeIsFail(f))
    }.toIterator.toMap

    val outputFile = new File(outputDir, s"deps.json")
    logger.info(s"Writing dependencies to: $outputFile")
    val writer = new PrintWriter(outputFile)
    writer.println(ConfigUtils.mapToJson(Map(
      "jobs" -> jobs,
      "files" -> files.values.par.map(_.getMap).toList
    )).spaces2)
    writer.close()

    PipelineStatus.writePipelineStatus(PipelineStatus.readDepsFile(outputFile), outputDir)
    logger.info("done calculating dependencies")
  }

}
