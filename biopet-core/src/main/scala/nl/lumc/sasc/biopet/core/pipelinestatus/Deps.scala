package nl.lumc.sasc.biopet.core.pipelinestatus

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils

/**
  * This class can store the deps.json from a pipeline that stores all jobs and files and the connections
  *
  * Created by pjvanthof on 24/06/2017.
  */
case class Deps(jobs: Map[String, Job], files: Array[Map[String, Any]]) {

  /**
    * This method will compress the graph by combining all common job names
    * @param main When set true the non main jobs will be skipped in the graph
    * @return List of dependencies
    */
  def compressOnType(main: Boolean = false): Map[String, List[String]] = {
    (for ((_, job) <- jobs.toSet if !main || job.mainJob) yield {
      job.name -> (if (main) getMainDependencies(job.name).map(Job.compressedName(_)._1)
                   else job.dependsOnJobs.map(Job.compressedName(_)._1))
    }).groupBy(x => Job.compressedName(x._1)._1)
      .map(x => x._1 -> x._2.flatMap(_._2).toList.distinct)
  }

  /** this will return all main dependencies */
  def getMainDeps: Map[String, List[String]] = {
    jobs.filter(_._2.mainJob).map(x => x._1 -> getMainDependencies(x._1))
  }

  /**
    * This will return for a single job the main dependencies.
    * When a job depend on a non main job it will take the dependencies from that job till it finds a main dependency
    */
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

  /** This will read a deps.json and returns it as a [[Deps]] class */
  def readDepsFile(depsFile: File): Deps = {
    val deps = ConfigUtils.fileToConfigMap(depsFile)

    val jobs =
      ConfigUtils.any2map(deps("jobs")).map(x => x._1 -> new Job(x._1, ConfigUtils.any2map(x._2)))
    val files = ConfigUtils.any2list(deps("files")).map(x => ConfigUtils.any2map(x)).toArray

    Deps(jobs, files)
  }
}
