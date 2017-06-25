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
package nl.lumc.sasc.biopet.core.pipelinestatus

import java.io.{File, PrintWriter}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import nl.lumc.sasc.biopet.utils.pim._
import nl.lumc.sasc.biopet.utils.pim.{Job => PimJob}
import nl.lumc.sasc.biopet.utils.{ConfigUtils, ToolCommand}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
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
                  compressPlots: Boolean = true,
                  pimHost: Option[String] = None)
      extends AbstractArgs

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
    opt[Unit]('f', "follow") maxOccurs 1 action { (_, c) =>
      c.copy(follow = true)
    } text "This will follow a run"
    opt[Int]("refresh") maxOccurs 1 action { (x, c) =>
      c.copy(refreshTime = x)
    } text "Time to check again, default set on 30 seconds"
    opt[Unit]("completePlots") maxOccurs 1 action { (_, c) =>
      c.copy(complatePlots = true)
    } text "Add complete plots, this is disabled because of performance. " +
      "Complete plots does show each job separated, while compressed plots collapse all jobs of the same type together."
    opt[Unit]("skipCompressPlots") maxOccurs 1 action { (_, c) =>
      c.copy(compressPlots = false)
    } text "Disable compressed plots. By default compressed plots are enabled."
    opt[String]("pimHost") maxOccurs 1 action { (x, c) =>
      c.copy(pimHost = Some(x))
    } text "Pim host to publish status to"
  }

  implicit lazy val system = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()
  lazy val ws = AhcWSClient()

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val depsFile = cmdArgs.depsFile.getOrElse(getDepsFileFromDir(cmdArgs.pipelineDir))
    val deps = Deps.readDepsFile(depsFile)
    writePipelineStatus(deps,
                        cmdArgs.outputDir,
                        follow = cmdArgs.follow,
                        refreshTime = cmdArgs.refreshTime,
                        plots = cmdArgs.complatePlots,
                        compressPlots = cmdArgs.compressPlots,
                        pimHost = cmdArgs.pimHost)
    logger.info("Done")
    sys.exit()
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

  def writePipelineStatus(deps: Deps,
                          outputDir: File,
                          alreadyDone: Set[String] = Set(),
                          alreadyFailed: Set[String] = Set(),
                          follow: Boolean = false,
                          refreshTime: Int = 30,
                          plots: Boolean = false,
                          compressPlots: Boolean = true,
                          pimHost: Option[String] = None): Unit = {

    val jobDone = jobsDone(deps)
    val jobFailed = jobsFailed(deps, jobDone)
    val jobsStart = jobsReadyStart(deps, jobDone)

    var futures: List[Future[Any]] = Nil

    val jobsDeps = deps.jobs.map(x =>
      x._1 -> (x._2.dependsOnJobs match {
        case l: List[_] => l.map(_.toString)
      }))
    val jobsWriter = new PrintWriter(new File(outputDir, s"jobs.json"))
    jobsWriter.println(ConfigUtils.mapToJson(jobsDeps).spaces2)
    jobsWriter.close()
    futures :+= writeGraphvizFile(jobsDeps,
                                  new File(outputDir, s"jobs.gv"),
                                  jobDone,
                                  jobFailed,
                                  jobsStart,
                                  deps,
                                  plots,
                                  plots)
    futures :+= writeGraphvizFile(jobsDeps,
                                  new File(outputDir, s"compress.jobs.gv"),
                                  jobDone,
                                  jobFailed,
                                  jobsStart,
                                  deps,
                                  compressPlots,
                                  compressPlots,
                                  compress = true)

    val mainJobs = deps.jobs.filter(_._2.mainJob == true).map {
      case (name, _) => name -> deps.getMainDependencies(name)
    }

    val mainJobsWriter = new PrintWriter(new File(outputDir, s"main_jobs.json"))
    mainJobsWriter.println(ConfigUtils.mapToJson(mainJobs).spaces2)
    mainJobsWriter.close()
    futures :+= writeGraphvizFile(mainJobs,
                                  new File(outputDir, s"main_jobs.gv"),
                                  jobDone,
                                  jobFailed,
                                  jobsStart,
                                  deps,
                                  plots,
                                  plots)
    futures :+= writeGraphvizFile(mainJobs,
                                  new File(outputDir, s"compress.main_jobs.gv"),
                                  jobDone,
                                  jobFailed,
                                  jobsStart,
                                  deps,
                                  compressPlots,
                                  compressPlots,
                                  compress = true)

    val totalJobs = deps.jobs.size
    val totalStart = jobsStart.size
    val totalDone = jobDone.size
    val totalFailed = jobFailed.size
    val totalPending = totalJobs - jobsStart.size - jobDone.size - jobFailed.size

    futures.foreach(x => Await.ready(x, Duration.Inf))

    pimHost.foreach { host =>
      val links: List[Link] = deps.compressOnType.flatMap(x => x._2.map(y => Link("test", y, "output", x._1, "input", "test"))).toList
      val run = Run("test", Network(
        "test", Nil, deps.compressOnType.map(x => Node(x._1, "root", List(Port("input", "test")), List(Port("output", "test")), "test")).toList,
        links), "test", "biopet")
      println(run.toString)
      val request = ws.url(s"$host/api/runs/")
        .withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json")
        .put(run.toString)

      val content = Await.result(request, Duration.Inf)

      println(content.status)
      println(content.body)

      val futures = for (job <- deps.jobs) yield {
        val status = job._1 match {
          case n if jobsStart.contains(n) => JobStatus.running
          case n if jobFailed.contains(n) => JobStatus.failed
          case n if jobDone.contains(n) => JobStatus.success
          case _ => JobStatus.idle
        }
        ws.url(s"$host/api/runs/test/jobs/" + job._1)
          .withHeaders("Accept" -> "application/json", "Content-Type" -> "application/json")
          .put(PimJob(job._1, Job.compressedName(job._1)._1, "test", "test", status).toString)
      }
      Await.result(Future.sequence(futures), Duration.Inf).foreach(println)
    }
    logger.info(
      s"Total job: $totalJobs, Pending: $totalPending, Ready to run / running: $totalStart, Done: $totalDone, Failed $totalFailed")

    if (follow) {
      Thread.sleep(refreshTime * 1000)
      writePipelineStatus(deps, outputDir, jobDone, jobFailed, follow, refreshTime, plots, compressPlots, pimHost)
    }
  }

  def writeGraphvizFile(jobsDeps: Map[String, List[String]],
                        outputFile: File,
                        jobDone: Set[String],
                        jobFailed: Set[String],
                        jobsStart: Set[String],
                        deps: Deps,
                        png: Boolean = true,
                        svg: Boolean = true,
                        compress: Boolean = false): Future[Unit] = Future {
    val graph = if (compress) deps.compressOnType else jobsDeps
    val writer = new PrintWriter(outputFile)
    writer.println("digraph graphname {")

    graph.foreach {
      case (job, jobDeps) =>
        // Writing color of node
        val compressTotal =
          if (compress) Some(deps.jobs.keys.filter(Job.compressedName(_)._1 == job)) else None
        val compressDone =
          if (compress) Some(jobDone.filter(Job.compressedName(_)._1 == job)) else None
        val compressFailed =
          if (compress) Some(jobFailed.filter(Job.compressedName(_)._1 == job)) else None
        val compressStart =
          if (compress) Some(jobsStart.filter(Job.compressedName(_)._1 == job)) else None
        val compressIntermediate =
          if (compress)
            Some(deps.jobs.filter(x => Job.compressedName(x._1)._1 == job).forall(_._2.intermediate))
          else None

        if (compress) {
          val pend = compressTotal.get.size - compressFailed.get.diff(compressStart.get)
            .size - compressStart.get.size - compressDone.get.size
          writer.println(s"""  $job [label = "$job
        |Total: ${compressTotal.get.size}
        |Fail: ${compressFailed.get.size}
        |Pend:$pend
        |Run: ${compressStart.get.diff(compressFailed.get).size}
        |Done: ${compressDone.get.size}"]""".stripMargin)
        }

        if (jobDone.contains(job) || compress && compressTotal == compressDone)
          writer.println(s"  $job [color = green]")
        else if (jobFailed.contains(job) || compress && compressFailed.get.nonEmpty)
          writer.println(s"  $job [color = red]")
        else if (jobsStart.contains(job) || compress && compressTotal == compressStart)
          writer.println(s"  $job [color = orange]")

        // Dashed lined for intermediate jobs
        if ((deps.jobs.contains(job) && deps
              .jobs(job)
              .intermediate) || compressIntermediate.contains(true))
          writer.println(s"  $job [style = dashed]")

        // Writing Node deps
        jobDeps.foreach { dep =>
          if (compress) {
            val depsNames = deps.jobs
              .filter(x => Job.compressedName(x._1)._1 == dep)
              .filter(_._2.outputUsedByJobs.exists(x => Job.compressedName(x)._1 == job))
              .map(x => x._1 -> x._2.outputUsedByJobs.filter(x => Job.compressedName(x)._1 == job))
            val total = depsNames.size
            val done = depsNames
              .map(x => x._2.exists(y => jobDone.contains(x._1)))
              .count(_ == true)
              .toFloat / total
            val fail = depsNames
              .map(x => x._2.exists(y => jobFailed.contains(x._1)))
              .count(_ == true)
              .toFloat / total
            val start = (depsNames
              .map(x => x._2.exists(y => jobsStart.contains(x._1)))
              .count(_ == true)
              .toFloat / total) - fail
            if (total > 0)
              writer.println(
                s"""  $dep -> $job [color="red;%f:orange;%f:green;%f:black;%f"];"""
                  .format(fail, start, done, 1.0f - done - fail - start))
            else writer.println(s"  $dep -> $job;")
          } else writer.println(s"  $dep -> $job;")
        }
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
    deps.jobs
      .filterNot(x => jobsDone.contains(x._1))
      .filter(_._2.dependsOnJobs.forall(jobsDone))
      .keySet
  }

  def jobsDone(deps: Deps, alreadyDone: Set[String] = Set()): Set[String] = {
    val f = deps.jobs.filterNot(x => alreadyDone.contains(x._1)).map(x => x._2 -> x._2.isDone)
    val dones = f
      .map(x => x._1 -> Await.result(x._2, Duration.Inf))
      .filter(_._2)
      .map(_._1.name)
      .toSet ++ alreadyDone
    val f2 = f.map(x =>
      x._1 -> x._2.map { d =>
        if (d || !x._1.intermediate) d
        else upstreamJobDone(x._1, dones, deps)
    })
    val d = f2.map(x => x._1 -> Await.result(x._2, Duration.Inf))
    d.filter(_._2).map(_._1.name).toSet
  }

  private def upstreamJobDone(job: Job, dones: Set[String], deps: Deps): Boolean = {
    job.outputUsedByJobs
      .map(deps.jobs)
      .exists(x => dones.contains(x.name) || (x.intermediate && upstreamJobDone(x, dones, deps)))
  }

  def jobsFailed(deps: Deps, dones: Set[String], alreadyFailed: Set[String] = Set()): Set[String] = {
    val f = deps.jobs
      .filterNot(x => dones.contains(x._1))
      .filterNot(x => alreadyFailed.contains(x._1))
      .map(x => x._1 -> x._2.isFailed)
    f.map(x => x._1 -> Await.result(x._2, Duration.Inf)).filter(_._2).keySet ++ alreadyFailed
  }

}
