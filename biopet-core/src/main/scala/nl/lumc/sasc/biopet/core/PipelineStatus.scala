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

    val depsFile = cmdArgs.depsFile.getOrElse(getDepsFileFromDir(cmdArgs.pipelineDir))
    val deps = readDepsFile(depsFile)
    writePipelineStatus(deps, cmdArgs.outputDir)
    logger.info("Done")
  }

  def getDepsFileFromDir(pipelineDir: File): File = {
    require(pipelineDir.exists(), s"pipelineDir does not exist: $pipelineDir")
    val logDir = new File(pipelineDir, ".log")
    require(logDir.exists(), s"No .log dir found in pipelineDir")
    val runLogDir = logDir.list().sorted.map(new File(logDir, _)).filter(_.isDirectory).last
    val graphDir = new File(runLogDir, "graph")
    require(graphDir.exists(), s"Graph dir is not found: $graphDir")
    new File(graphDir, "deps.json")
  }

  case class Deps(jobs: Map[String, Job], files: Array[Map[String, Any]])

  def readDepsFile(depsFile: File) = {
    val deps = ConfigUtils.fileToConfigMap(depsFile)

    val jobs = ConfigUtils.any2map(deps("jobs")).map(x => x._1 -> new Job(x._1, ConfigUtils.any2map(x._2)))
    val files = ConfigUtils.any2list(deps("files")).map(x => ConfigUtils.any2map(x)).toArray

    Deps(jobs, files)
  }

  def writePipelineStatus(deps: Deps,
                          outputDir: File,
                          alreadyDone: Set[String] = Set()): Unit = {

    val jobDone = jobsDone(deps)
    val jobFailed = jobsFailed(deps)

    val jobsDeps = deps.jobs.map(x => x._1 -> (x._2.dependsOnJobs match {
      case l: List[_] => l.map(_.toString)
      case _          => throw new IllegalStateException("Value 'depends_on_jobs' is not a list")
    }))
    val jobsWriter = new PrintWriter(new File(outputDir, s"jobs.json"))
    jobsWriter.println(ConfigUtils.mapToJson(jobsDeps).spaces2)
    jobsWriter.close()
    writeGraphvizFile(jobsDeps, new File(outputDir, s"jobs.gv"), jobDone, jobFailed, deps)
    writeGraphvizFile(compressOnType(jobsDeps), new File(outputDir, s"compress.jobs.gv"), jobDone, jobFailed, deps)

    val mainJobs = deps.jobs.filter(_._2.mainJob == true).map {
      case (name, job) =>
        name -> getMainDependencies(name, deps)
    }

    val mainJobsWriter = new PrintWriter(new File(outputDir, s"main_jobs.json"))
    mainJobsWriter.println(ConfigUtils.mapToJson(mainJobs).spaces2)
    mainJobsWriter.close()
    writeGraphvizFile(mainJobs, new File(outputDir, s"main_jobs.gv"), jobDone, jobFailed, deps)
    writeGraphvizFile(compressOnType(mainJobs), new File(outputDir, s"compress.main_jobs.gv"), jobDone, jobFailed, deps)

    //print(jobsDone(jobs).mkString("\n"))

  }

  def getMainDependencies(jobName: String, deps: Deps): List[String] = {
    val job = deps.jobs(jobName)
    val dependencies = job.dependsOnJobs match {
      case l: List[_] => l.map(_.toString)
    }
    dependencies.flatMap { dep =>
      deps.jobs(dep).mainJob match {
        case true  => List(dep)
        case false => getMainDependencies(dep, deps)
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
                        jobDone: Set[String],
                        jobFailed: Set[String],
                        deps: Deps): Unit = {
    val writer = new PrintWriter(outputFile)
    writer.println("digraph graphname {")
    jobDone
      .filter(x => jobsDeps.contains(x))
      .foreach(x => writer.println(s"  $x [color = green]"))
    jobFailed
      .filter(x => jobsDeps.contains(x))
      .foreach(x => writer.println(s"  $x [color = red]"))
    jobsReadyStart(deps.jobs, jobDone)
      .filter(x => jobsDeps.contains(x))
      .filterNot(jobDone.contains)
      .foreach(x => writer.println(s"  $x [color = orange]"))
    deps.jobs
      .filter(x => jobsDeps.contains(x._1))
      .filter(_._2.intermediate)
      .foreach(x => writer.println(s"  ${x._1} [style = dashed]"))
    jobsDeps.foreach { case (a, b) => b.foreach(c => writer.println(s"  $c -> $a;")) }
    writer.println("}")
    writer.close()
  }

  def jobsReadyStart(jobs: Map[String, Job], jobsDone: Set[String]): Set[String] = {
    jobs.filter(_._2.dependsOnJobs.forall(jobsDone)).map(_._1).toSet
  }

  def jobsDone(deps: Deps): Set[String] = {
    val f = deps.jobs.map(x => x._2 -> x._2.isDone)
    val dones = f.map(x => x._1 -> Await.result(x._2, Duration.Inf))
    val f2 = f.map(x => x._1 -> x._2.map{ d =>
      if (d || !x._1.intermediate) d
      else upstreamJobDone(x._1, dones, deps)
    })
    val d = f2.map(x => x._1 -> Await.result(x._2, Duration.Inf))
    d.filter(_._2).map(_._1.name).toSet
  }

  private def upstreamJobDone(job: Job, dones: Map[Job, Boolean], deps: Deps): Boolean = {
    job.outputUsedByJobs.map(deps.jobs)
      .exists(x => dones(x) || (x.intermediate && upstreamJobDone(x, dones, deps)))
  }

  def jobsFailed(deps: Deps): Set[String] = {
    val f = deps.jobs.map(x => x._1 -> x._2.isFailed)
    f.map(x => x._1 -> Await.result(x._2, Duration.Inf)).filter(_._2).map(_._1).toSet
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
