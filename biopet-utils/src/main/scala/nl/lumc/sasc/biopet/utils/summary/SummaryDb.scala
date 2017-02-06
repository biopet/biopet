package nl.lumc.sasc.biopet.utils.summary

import java.sql.Blob

import slick.driver.H2Driver.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import nl.lumc.sasc.biopet.utils.summary.db.Schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

/**
  * Created by pjvanthof on 05/02/2017.
  */
class SummaryDb(db: Database) {
  /** This method will create all table */
  def createTables(): Unit = {
    try {
      val setup = DBIO.seq(
        (runs.schema ++ samples.schema ++
          libraries.schema ++ pipelineNames.schema ++
          moduleNames.schema ++ stats.schema ++ settings.schema ++
          files.schema ++ executables.schema).create
      )
      val setupFuture = db.run(setup)
      Await.result(setupFuture, Duration.Inf)
    }
  }

  def createRun(runName: String, outputDir: String): Future[Int] = {
    val id = Await.result(db.run(runs.size.result), Duration.Inf)
    db.run(runs.forceInsert(id, runName, outputDir)).map(_ => id)
  }

  def getRuns(runId: Option[Int] = None, runName: Option[String] = None, outputDir: Option[String] = None) = {
    val q = runs.filter { run =>
      List(
        runId.map(run.id === _),
        runName.map(run.runName === _),
        outputDir.map(run.outputDir === _) // not a condition as `criteriaRoast` evaluates to `None`
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  def createSample(runId: Int, name: String, tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(samples.size.result), Duration.Inf)
    db.run(samples.forceInsert(id, runId, name, tags)).map(_ => id)
  }

  def getSamples(sampleId: Option[Int] = None, runId: Option[Int] = None, name: Option[String] = None) = {
    val q = samples.filter { sample =>
      List(
        sampleId.map(sample.id === _),
        runId.map(sample.runId === _),
        name.map(sample.name === _) // not a condition as `criteriaRoast` evaluates to `None`
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.map(x => (x.id, x.runId, x.name)).result)
  }

  def sampleTags(sampleId: Int): Map[String, Any] = {
    samples.filter(_.id === sampleId).map(_.tags)
  }

}
