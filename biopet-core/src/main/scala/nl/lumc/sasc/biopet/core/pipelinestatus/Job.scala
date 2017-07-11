package nl.lumc.sasc.biopet.core.pipelinestatus

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

/**
  * This class can store a single job from a deps.json
  *
  * Created by pjvanthof on 24/06/2017.
  */
class Job(val name: String, map: Map[String, Any]) {

  /** When true this job was done at the moment of the deps.json creation */
  def doneAtStart: Boolean = ConfigUtils.any2boolean(map("done_at_start"))

  /** If one of this files exist the job is marked as failed */
  def failFiles: List[File] = ConfigUtils.any2fileList(map("fail_files"))

  /** If all of this files exist the job is marked as done */
  def doneFiles: List[File] = ConfigUtils.any2fileList(map("done_files"))

  /** Returns a list of jobs that depends on this job */
  def outputUsedByJobs: List[String] = ConfigUtils.any2stringList(map("output_used_by_jobs"))

  /** Returns a list of job where this job depends on */
  def dependsOnJobs: List[String] = ConfigUtils.any2stringList(map("depends_on_jobs"))

  /** Location of the stdout file of this job */
  def stdoutFile = new File(ConfigUtils.any2string(map("stdout_file")))

  /** All output files of this job */
  def outputsFiles: List[File] = ConfigUtils.any2fileList(map("outputs"))

  /** All input files of this job */
  def inputFiles: List[File] = ConfigUtils.any2fileList(map("inputs"))

  /** When true this job is marked as a main job in the graph */
  def mainJob: Boolean = ConfigUtils.any2boolean(map("main_job"))

  /** When true this job is marked as a intermediate job */
  def intermediate: Boolean = ConfigUtils.any2boolean(map("intermediate"))

  /** Return a [[Future[Boolean]] to check if the job is done */
  def isDone: Future[Boolean] = Future { doneFiles.forall(_.exists()) }

  /** Return a [[Future[Boolean]] to check if the job is failed */
  def isFailed: Future[Boolean] = Future { failFiles.exists(_.exists()) }

  /** Returns the compressed name of this job */
  def compressedName: (String, Int) = Job.compressedName(name)
}

object Job {
  val numberRegex: Regex = """(.*)_(\d*)$""".r

  /** This splits a job name from it's id */
  def compressedName(jobName: String): (String, Int) = jobName match {
    case Job.numberRegex(name, number) => (name, number.toInt)
  }

}
