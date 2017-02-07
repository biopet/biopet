package nl.lumc.sasc.biopet.utils.summary

import nl.lumc.sasc.biopet.utils.ConfigUtils
import slick.driver.H2Driver.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import nl.lumc.sasc.biopet.utils.summary.db.Schema._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pjvanthof on 05/02/2017.
  */
class SummaryDb(db: Database) {
  /** This method will create all table */
  def createTables(): Unit = {
    try {
      val setup = DBIO.seq(
        (runs.schema ++ samples.schema ++
          libraries.schema ++ pipelines.schema ++
          modules.schema ++ stats.schema ++ settings.schema ++
          files.schema ++ executables.schema).create
      )
      val setupFuture = db.run(setup)
      Await.result(setupFuture, Duration.Inf)
    }
  }

  def createRun(runName: String, outputDir: String): Future[Int] = {
    val id = Await.result(db.run(runs.size.result), Duration.Inf)
    db.run(runs.forceInsert(Run(id, runName, outputDir))).map(_ => id)
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

  def createSample(name: String, runId: Int, tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(samples.size.result), Duration.Inf)
    db.run(samples.forceInsert(Sample(id, name, runId, tags))).map(_ => id)
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

  def getSampleTags(sampleId: Int): Future[Option[Map[String, Any]]] = {
    db.run(samples.filter(_.id === sampleId).map(_.tags).result)
      .map(_.headOption.flatten.map(ConfigUtils.jsonTextToMap))
  }

  def createLibrary(name: String, runId: Int, sampleId: Int, tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(libraries.size.result), Duration.Inf)
    db.run(libraries.forceInsert(Library(id, name, runId, sampleId, tags))).map(_ => id)
  }

  def getLibraries(libId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None, sampleId: Option[Int] = None) = {
    val q = libraries.filter { lib =>
      List(
        libId.map(lib.id === _),
        sampleId.map(lib.sampleId === _),
        runId.map(lib.runId === _),
        name.map(lib.name === _) // not a condition as `criteriaRoast` evaluates to `None`
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.map(x => (x.id, x.name, x.runId, x.sampleId)).result)
  }

  def getLibraryTags(libId: Int): Future[Option[Map[String, Any]]] = {
    db.run(libraries.filter(_.id === libId).map(_.tags).result)
      .map(_.headOption.flatten.map(ConfigUtils.jsonTextToMap))
  }

  def createPipeline(name: String, runId: Int): Future[Int] = {
    val id = Await.result(db.run(pipelines.size.result), Duration.Inf)
    db.run(pipelines.forceInsert(Pipeline(id, name, runId))).map(_ => id)
  }

  def getPipelines(pipelineId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None) = {
    val q = pipelines.filter { lib =>
      List(
        pipelineId.map(lib.id === _),
        runId.map(lib.runId === _),
        name.map(lib.name === _) // not a condition as `criteriaRoast` evaluates to `None`
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.map(x => (x.id, x.name, x.runId)).result)
  }

  def createModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    val id = Await.result(db.run(modules.size.result), Duration.Inf)
    db.run(modules.forceInsert(Module(id, name, runId, pipelineId))).map(_ => id)
  }

  def getModules(moduleId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None, pipelineId: Option[Int] = None) = {
    val q = modules.filter { lib =>
      List(
        moduleId.map(lib.id === _),
        runId.map(lib.runId === _),
        pipelineId.map(lib.pipelineId === _),
        name.map(lib.name === _) // not a condition as `criteriaRoast` evaluates to `None`
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.map(x => (x.id, x.name, x.runId)).result)
  }

}
