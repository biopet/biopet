package nl.lumc.sasc.biopet.core.pipelinestatus

import java.io.File

import nl.lumc.sasc.biopet.utils.ConfigUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

/**
  * Created by pjvanthof on 24/06/2017.
  */
class Job(val name: String, map: Map[String, Any]) {

  def doneAtStart: Boolean = ConfigUtils.any2boolean(map("done_at_start"))

  def failFiles: List[File] = ConfigUtils.any2fileList(map("fail_files"))
  def doneFiles: List[File] = ConfigUtils.any2fileList(map("done_files"))
  def outputUsedByJobs: List[String] = ConfigUtils.any2stringList(map("output_used_by_jobs"))
  def dependsOnJobs: List[String] = ConfigUtils.any2stringList(map("depends_on_jobs"))
  def stdoutFile = new File(ConfigUtils.any2string(map("stdout_file")))

  def outputsFiles: List[File] = ConfigUtils.any2fileList(map("outputs"))
  def inputFiles: List[File] = ConfigUtils.any2fileList(map("inputs"))

  def mainJob: Boolean = ConfigUtils.any2boolean(map("main_job"))
  def intermediate: Boolean = ConfigUtils.any2boolean(map("intermediate"))

  def isDone: Future[Boolean] = Future { doneFiles.forall(_.exists()) }
  def isFailed: Future[Boolean] = Future { failFiles.exists(_.exists()) }

  def compressedName: (String, Int) = Job.compressedName(name)
}

object Job {
  val numberRegex: Regex = """(.*)_(\d*)$""".r

  def compressedName(jobName: String): (String, Int) = jobName match {
    case Job.numberRegex(name, number) => (name, number.toInt)
  }

}
