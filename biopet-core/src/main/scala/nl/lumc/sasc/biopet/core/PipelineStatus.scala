package nl.lumc.sasc.biopet.core

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.{ ConfigUtils, ToolCommand }

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.sys.process.Process

/**
 * Created by pjvan_thof on 7-12-16.
 */
object PipelineStatus extends ToolCommand {
  case class Args(pipelineDir: File = null,
                  depsFile: Option[File] = None,
                  outputDir: File = null,
                  follow: Boolean = false,
                  refreshTime: Int = 30,
                  complatePlots: Boolean = false,
                  compressPlots: Boolean = true) extends AbstractArgs

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
    opt[Unit]('f', "follow") maxOccurs 1 action { (x, c) =>
      c.copy(follow = true)
    } text "This will follow a run"
    opt[Int]("refresh") maxOccurs 1 action { (x, c) =>
      c.copy(refreshTime = x)
    } text "Time to check again, default set on 30 seconds"
    opt[Unit]("completePlots") maxOccurs 1 action { (x, c) =>
      c.copy(complatePlots = true)
    } text "Add complete plots, this is disabled because of performance. " +
      "Complete plots does show each job separated, while compressed plots collapse all jobs of the same type together."
    opt[Unit]("skipCompressPlots") maxOccurs 1 action { (x, c) =>
      c.copy(compressPlots = false)
    } text "Disable compressed plots. By default compressed plots are enabled."
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val depsFile = cmdArgs.depsFile.getOrElse(getDepsFileFromDir(cmdArgs.pipelineDir))
    val deps = readDepsFile(depsFile)
    writePipelineStatus(deps, cmdArgs.outputDir, follow = cmdArgs.follow, refreshTime = cmdArgs.refreshTime,
      plots = cmdArgs.complatePlots, compressPlots = cmdArgs.compressPlots)
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
                          alreadyDone: Set[String] = Set(),
                          alreadyFailed: Set[String] = Set(),
                          follow: Boolean = false,
                          refreshTime: Int = 30,
                          plots: Boolean = false,
                          compressPlots: Boolean = true): Unit = {

    val jobDone = jobsDone(deps)
    val jobFailed = jobsFailed(deps, jobDone)
    val jobsStart = jobsReadyStart(deps, jobDone)

    var futures: List[Future[Any]] = Nil

    val jobsDeps = deps.jobs.map(x => x._1 -> (x._2.dependsOnJobs match {
      case l: List[_] => l.map(_.toString)
      case _          => throw new IllegalStateException("Value 'depends_on_jobs' is not a list")
    }))
    val jobsWriter = new PrintWriter(new File(outputDir, s"jobs.json"))
    jobsWriter.println(ConfigUtils.mapToJson(jobsDeps).spaces2)
    jobsWriter.close()
    futures :+= writeGraphvizFile(jobsDeps, new File(outputDir, s"jobs.gv"), jobDone, jobFailed, jobsStart, deps, plots, plots)
    futures :+= writeGraphvizFile(jobsDeps, new File(outputDir, s"compress.jobs.gv"), jobDone, jobFailed, jobsStart, deps, compressPlots, compressPlots, compress = true)

    val mainJobs = deps.jobs.filter(_._2.mainJob == true).map {
      case (name, job) =>
        name -> getMainDependencies(name, deps)
    }

    val mainJobsWriter = new PrintWriter(new File(outputDir, s"main_jobs.json"))
    mainJobsWriter.println(ConfigUtils.mapToJson(mainJobs).spaces2)
    mainJobsWriter.close()
    futures :+= writeGraphvizFile(mainJobs, new File(outputDir, s"main_jobs.gv"), jobDone, jobFailed, jobsStart, deps, plots, plots)
    futures :+= writeGraphvizFile(mainJobs, new File(outputDir, s"compress.main_jobs.gv"), jobDone, jobFailed, jobsStart, deps, compressPlots, compressPlots, compress = true)

    val totalJobs = deps.jobs.size
    val totalStart = jobsStart.size
    val totalDone = jobDone.size
    val totalFailed = jobFailed.size
    val totalPending = totalJobs - jobsStart.size - jobDone.size - jobFailed.size

    futures.foreach(x => Await.ready(x, Duration.Inf))

    logger.info(s"Total job: ${totalJobs}, Pending: ${totalPending}, Ready to run / running: ${totalStart}, Done: ${totalDone}, Failed ${totalFailed}")

    if (follow) {
      Thread.sleep(refreshTime * 1000)
      writePipelineStatus(deps, outputDir, jobDone, jobFailed, follow)
    }
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
      (compressedName(job)._1, compressedName(dep)._1)
    }
    // This will collapse a Set[(String, String)] to a Map[String, List[String]]
    set.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toList)
  }

  def compressedName(jobName: String) = jobName match {
      case numberRegex(name, number) =>  (name, number.toInt)
  }

  def writeGraphvizFile(jobsDeps: Map[String, List[String]],
                        outputFile: File,
                        jobDone: Set[String],
                        jobFailed: Set[String],
                        jobsStart: Set[String],
                        deps: Deps,
                        png: Boolean = true, svg: Boolean = true, compress: Boolean = false): Future[Unit] = Future {
    val graph = if (compress) compressOnType(jobsDeps) else jobsDeps
    val writer = new PrintWriter(outputFile)
    writer.println("digraph graphname {")

    graph.foreach { case (job, jobDeps) =>
      // Writing color of node
      val compressTotal = if (compress) Some(deps.jobs.keys.count(compressedName(_)._1 == job)) else None
      val compressDone = if (compress) Some(jobDone.count(compressedName(_)._1 == job)) else None
      val compressFailed = if (compress) Some(jobFailed.count(compressedName(_)._1 == job)) else None
      val compressStart = if (compress) Some(jobsStart.count(compressedName(_)._1 == job)) else None
      val compressIntermediate = if (compress) Some(deps.jobs.filter(x => x._2.intermediate)
        .count(x => compressedName(x._1)._1 == job)) else None

      if (jobDone.contains(job) || compress && compressTotal == compressDone) writer.println(s"  $job [color = green]")
      else if (jobFailed.contains(job) || compress && compressTotal == compressFailed) writer.println(s"  $job [color = red]")
      else if (jobsStart.contains(job) || compress && compressTotal == compressStart) writer.println(s"  $job [color = orange]")

      // Dashed lined for intermediate jobs
      if ((deps.jobs.contains(job) && deps.jobs(job).intermediate) || (compress && compressTotal == compressIntermediate))
        writer.println(s"  $job [style = dashed]")

      // Writing Node deps
      jobDeps.foreach(c => writer.println(s"  $c -> $job;"))
    }
    writer.println("}")
    writer.close()

    writeGvToPlot(outputFile, png, svg)
  }

  def writeGvToPlot(input: File, png: Boolean = true, svg: Boolean = true): Unit = {
    if (png) Process(Seq("dot", "-Tpng", "-O", input.getAbsolutePath)).run().exitValue()
    if (svg) Process(Seq("dot", "-Tsvg", "-O", input.getAbsolutePath)).run().exitValue()
  }

  def jobsReadyStart(deps: Deps, jobsDone: Set[String]): Set[String] = {
    deps.jobs.filterNot(x => jobsDone.contains(x._1)).filter(_._2.dependsOnJobs.forall(jobsDone)).keySet
  }

  def jobsDone(deps: Deps, alreadyDone: Set[String] = Set()): Set[String] = {
    val f = deps.jobs.filterNot(x => alreadyDone.contains(x._1)).map(x => x._2 -> x._2.isDone)
    val dones = f.map(x => x._1 -> Await.result(x._2, Duration.Inf)).filter(_._2).map(_._1.name).toSet ++ alreadyDone
    val f2 = f.map(x => x._1 -> x._2.map { d =>
      if (d || !x._1.intermediate) d
      else upstreamJobDone(x._1, dones, deps)
    })
    val d = f2.map(x => x._1 -> Await.result(x._2, Duration.Inf))
    d.filter(_._2).map(_._1.name).toSet
  }

  private def upstreamJobDone(job: Job, dones: Set[String], deps: Deps): Boolean = {
    job.outputUsedByJobs.map(deps.jobs)
      .exists(x => dones.contains(x.name) || (x.intermediate && upstreamJobDone(x, dones, deps)))
  }

  def jobsFailed(deps: Deps, dones: Set[String], alreadyFailed: Set[String] = Set()): Set[String] = {
    val f = deps.jobs.filterNot(x => dones.contains(x._1))
      .filterNot(x => alreadyFailed.contains(x._1)).map(x => x._1 -> x._2.isFailed)
    f.map(x => x._1 -> Await.result(x._2, Duration.Inf)).filter(_._2).keySet ++ alreadyFailed
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
