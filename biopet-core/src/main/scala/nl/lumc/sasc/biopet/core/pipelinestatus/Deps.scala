package nl.lumc.sasc.biopet.core.pipelinestatus

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
  * Created by pjvanthof on 24/06/2017.
  */
case class Deps(jobs: Map[String, Job], files: Array[Map[String, Any]]) {

  lazy val compressOnType: Map[String, List[String]] = {
    Deps.compressOnType(jobs.map(x => x._1 -> x._2.dependsOnJobs))
  }

  def getMainDependencies(jobName: String): List[String] = {
    val job = this.jobs(jobName)
    val dependencies = job.dependsOnJobs match {
      case l: List[_] => l.map(_.toString)
    }
    dependencies.flatMap { dep =>
      if (this.jobs(dep).mainJob) List(dep)
      else getMainDependencies(dep)

    }.distinct
  }

}

object Deps {
  def readDepsFile(depsFile: File): Deps = {
    val deps = ConfigUtils.fileToConfigMap(depsFile)

    val jobs =
      ConfigUtils.any2map(deps("jobs")).map(x => x._1 -> new Job(x._1, ConfigUtils.any2map(x._2)))
    val files = ConfigUtils.any2list(deps("files")).map(x => ConfigUtils.any2map(x)).toArray

    Deps(jobs, files)
  }

  def compressOnType(jobs: Map[String, List[String]]): Map[String, List[String]] = {
    val set = for ((job, deps) <- jobs.toSet; dep <- deps) yield {
      (Job.compressedName(job)._1, Job.compressedName(dep)._1)
    }
    // This will collapse a Set[(String, String)] to a Map[String, List[String]]
    set.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toList) ++ jobs
      .filter(_._2.isEmpty)
      .map(job => Job.compressedName(job._1)._1 -> Nil)
  }

}
