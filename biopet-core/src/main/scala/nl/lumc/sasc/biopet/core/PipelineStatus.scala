package nl.lumc.sasc.biopet.core

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by pjvan_thof on 7-12-16.
  */
object PipelineStatus extends ToolCommand {
  case class Args(pipelineDir: File = null, depsFile: Option[File] = None, outputDir: File = null) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('d', "pipelineDir") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(pipelineDir = x)
    } text "Output directory of the pipeline"
    opt[File]('o', "outputDir") required () maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(outputDir = x)
    } text "Output directory of this tool"
    opt[File]("depsFile") maxOccurs 1 valueName "<file>" action { (x, c) =>
      c.copy(depsFile = Some(x))
    } text "Location of deps file, not required"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val depsFile = cmdArgs.depsFile.getOrElse {
      require(cmdArgs.pipelineDir.exists(), s"pipelineDir does not exist: ${cmdArgs.pipelineDir}")
      val logDir = new File(cmdArgs.pipelineDir, ".log")
      require(logDir.exists(), s"No .log dir found in pipelineDir")
      val runLogDir = logDir.list().sorted.map(new File(logDir, _)).filter(_.isDirectory).last
      val graphDir = new File(runLogDir, "graph")
      require(graphDir.exists(), s"Graph dir is not found: $graphDir")
      new File(graphDir, "deps.json")
    }
    writePipelineStatus(depsFile, cmdArgs.outputDir)
    logger.info("Done")
  }

  def writePipelineStatus(depsFile: File, outputDir: File): Unit = {
    val deps = ConfigUtils.fileToConfigMap(depsFile)

    val jobs = ConfigUtils.any2map(deps("jobs")).map(x => x._1 -> new Job(x._1, ConfigUtils.any2map(x._2)))

    val jobDone = jobsDone(jobs)
    val jobFailed = jobsFailed(jobs)
    val jobsRunning = jobs
      .filterNot(x => jobDone(x._1))
      .filterNot(x => jobFailed(x._1))
      .filter(_._2.stdoutFile.exists()).map(_._1).toList

    val jobsDeps = jobs.map(x => x._1 -> (x._2.dependsOnJobs match {
      case l: List[_] => l.map(_.toString)
      case _          => throw new IllegalStateException("Value 'depends_on_jobs' is not a list")
    }))
    val jobsWriter = new PrintWriter(new File(outputDir, s"jobs.json"))
    jobsWriter.println(ConfigUtils.mapToJson(jobsDeps).spaces2)
    jobsWriter.close()
    writeGraphvizFile(jobsDeps, new File(outputDir, s"jobs.gv"), jobDone, jobFailed, jobsRunning, jobs)
    writeGraphvizFile(compressOnType(jobsDeps), new File(outputDir, s"compress.jobs.gv"), jobDone, jobFailed, jobsRunning, jobs)

    val mainJobs = jobs.filter(_._2.mainJob == true).map {
      case (name, job) =>
        name -> getMainDependencies(name, jobs)
    }

    val mainJobsWriter = new PrintWriter(new File(outputDir, s"main_jobs.json"))
    mainJobsWriter.println(ConfigUtils.mapToJson(mainJobs).spaces2)
    mainJobsWriter.close()
    writeGraphvizFile(mainJobs, new File(outputDir, s"main_jobs.gv"), jobDone, jobFailed, jobsRunning, jobs)
    writeGraphvizFile(compressOnType(mainJobs), new File(outputDir, s"compress.main_jobs.gv"), jobDone, jobFailed, jobsRunning, jobs)

    //print(jobsDone(jobs).mkString("\n"))

  }

  def getMainDependencies(jobName: String, jobsMap: Map[String, Job]): List[String] = {
    val job = jobsMap(jobName)
    val dependencies = job.dependsOnJobs match {
      case l: List[_] => l.map(_.toString)
    }
    dependencies.flatMap { dep =>
      jobsMap(dep).mainJob match {
        case true  => List(dep)
        case false => getMainDependencies(dep, jobsMap)
      }
    }.distinct
  }

  val numberRegex = """(.*)_(\d*)$""".r
  def compressOnType(jobs: Map[String, List[String]]): Map[String, List[String]] = {
    val set = for ((job, deps) <- jobs.toSet; dep <- deps) yield {
      job match {
        case numberRegex(name, number) => (name, dep match {
          case numberRegex(name, number) => name
        })
      }
    }
    set.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toList)
  }

  def writeGraphvizFile(jobsDeps: Map[String, List[String]], outputFile: File,
                        jobDone: Map[String, Boolean],
                        jobFailed: Map[String, Boolean],
                        jobsRunning:  List[String],
                        jobs: Map[String, Job]): Unit = {
    val writer = new PrintWriter(outputFile)
    writer.println("digraph graphname {")
    jobDone
      .filter(x => jobsDeps.contains(x._1))
      .filter(_._2)
      .foreach(x => writer.println(s"  ${x._1} [color = green]"))
    jobFailed
      .filter(x => jobsDeps.contains(x._1))
      .filter(_._2)
      .foreach(x => writer.println(s"  ${x._1} [color = red]"))
    jobsReadyStart(jobs, jobDone)
      .filter(jobsDeps.contains)
      .filterNot(jobDone)
      .filterNot(jobsRunning.contains)
      .foreach(x => writer.println(s"  $x [color = orange]"))
    jobs
      .filter(x => jobsDeps.contains(x._1))
      .filter(_._2.intermediate)
      .foreach(x => writer.println(s"  ${x._1} [style = dashed]"))
    jobsDeps.foreach { case (a, b) => b.foreach(c => writer.println(s"  $c -> $a;")) }
    writer.println("}")
    writer.close()
  }

  def jobsReadyStart(jobs: Map[String, Job], jobsDone: Map[String, Boolean]): List[String] = {
    jobs.filter(_._2.dependsOnJobs.forall(jobsDone)).map(_._1).toList
  }

  def jobsDone(jobs: Map[String, Job]): Map[String, Boolean] = {
    val f = jobs.map(x => x._2 -> x._2.isDone)
    val dones = f.map(x => x._1 -> Await.result(x._2, Duration.Inf))
    val f2 = f.map(x => x._1 -> x._2.map{ d =>
      if (d || !x._1.intermediate) d
      else upstreamJobDone(x._1, dones, jobs)
    })
    f2.map(x => x._1.name -> Await.result(x._2, Duration.Inf))
  }

  private def upstreamJobDone(job: Job, dones: Map[Job, Boolean], allJobs: Map[String, Job]): Boolean = {
    job.outputUsedByJobs.map(allJobs)
      .exists(x => dones(x) || (x.intermediate && upstreamJobDone(x, dones, allJobs)))
  }

  def jobsFailed(jobs: Map[String, Job]): Map[String, Boolean] = {
    val f = jobs.map(x => x._1 -> x._2.isFailed)
    f.map(x => x._1 -> Await.result(x._2, Duration.Inf))
  }

  class Job(val name: String, map: Map[String, Any]) {

    def doneAtStart: Boolean = ConfigUtils.any2boolean(map("done_at_start"))

    def failFiles = ConfigUtils.any2fileList(map("fail_files"))
    def doneFiles = ConfigUtils.any2fileList(map("done_files"))
    def outputUsedByJobs = ConfigUtils.any2stringList(map("output_used_by_jobs"))
    def dependsOnJobs = ConfigUtils.any2stringList(map("depends_on_jobs"))
    def stdoutFile = new File(ConfigUtils.any2string(map("stdout_file")))

    def outputsFiles = ConfigUtils.any2fileList(map("outputs"))
    def inputFiles = ConfigUtils.any2fileList(map("inputs"))

    def mainJob = ConfigUtils.any2boolean(map("main_job"))
    def intermediate = ConfigUtils.any2boolean(map("intermediate"))

    def isDone: Future[Boolean] = Future { doneFiles.forall(_.exists()) }
    def isFailed: Future[Boolean] = Future { failFiles.exists(_.exists()) }
  }
}
