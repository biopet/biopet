package nl.lumc.sasc.biopet.utils.summary

import java.io.{ Closeable, File }

import nl.lumc.sasc.biopet.utils.ConfigUtils
import slick.driver.H2Driver.api._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import nl.lumc.sasc.biopet.utils.summary.db.Schema._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pjvanthof on 05/02/2017.
 */
class SummaryDb(db: Database) extends Closeable {

  def close(): Unit = db.close()

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
        outputDir.map(run.outputDir === _)
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
        name.map(sample.name === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
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
    db.run(q.result)
  }

  def getLibraryTags(libId: Int): Future[Option[Map[String, Any]]] = {
    db.run(libraries.filter(_.id === libId).map(_.tags).result)
      .map(_.headOption.flatten.map(ConfigUtils.jsonTextToMap))
  }

  def forceCreatePipeline(name: String, runId: Int): Future[Int] = {
    val id = Await.result(db.run(pipelines.size.result), Duration.Inf)
    db.run(pipelines.forceInsert(Pipeline(id, name, runId))).map(_ => id)
  }

  def createPipeline(name: String, runId: Int): Future[Int] = {
    getPipelines(name = Some(name), runId = Some(runId))
      .flatMap {
        case m =>
          if (m.isEmpty) forceCreatePipeline(name, runId)
          else Future(m.head.id)
      }
  }

  def getPipelines(pipelineId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None) = {
    val q = pipelines.filter { lib =>
      List(
        pipelineId.map(lib.id === _),
        runId.map(lib.runId === _),
        name.map(lib.name === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  def forceCreateModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    val id = Await.result(db.run(modules.size.result), Duration.Inf)
    db.run(modules.forceInsert(Module(id, name, runId, pipelineId))).map(_ => id)
  }

  def createModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    getModules(name = Some(name), runId = Some(runId), pipelineId = Some(pipelineId))
      .flatMap {
        case m =>
          if (m.isEmpty) forceCreateModule(name, runId, pipelineId)
          else Future(m.head.id)
      }
  }

  def getModules(moduleId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None, pipelineId: Option[Int] = None) = {
    val q = modules.filter { lib =>
      List(
        moduleId.map(lib.id === _),
        runId.map(lib.runId === _),
        pipelineId.map(lib.pipelineId === _),
        name.map(lib.name === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  def createStat(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None, libId: Option[Int] = None, content: String) = {
    db.run(stats.forceInsert(Stat(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  def createOrUpdateStat(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None, libId: Option[Int] = None, content: String) = {
    val filter = statsFilter(Some(runId), Some(pipelineId), Some(moduleId), Some(sampleId), Some(libId))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createStat(runId, pipelineId, moduleId, sampleId, libId, content)
    else db.run(filter.map(_.content).update(content))
  }

  private def statsFilter(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]] = None,
                          sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None) = {
    val l: List[Option[Query[Stats, Stats#TableElementType, Seq] => Query[Stats, Stats#TableElementType, Seq]]] = List(
      runId.map(x => y => y.filter(_.runId === x)),
      pipelineId.map(x => y => y.filter(_.pipelineId === x)),
      moduleId.map(x => y => (if (x.isDefined) y.filter(_.moduleId === x) else y.filter(_.moduleId.isEmpty))),
      sampleId.map(x => y => (if (x.isDefined) y.filter(_.sampleId === x) else y.filter(_.sampleId.isEmpty))),
      libId.map(x => y => (if (x.isDefined) y.filter(_.libraryId === x) else y.filter(_.libraryId.isEmpty)))
    )
    l.flatten.foldLeft(stats.subquery)((a, b) => b(a))
  }

  def getStats(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]] = None,
               sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None) = {
    db.run(statsFilter(runId, pipelineId, moduleId, sampleId, libId).result)
  }

  def getStat(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
              sampleId: Option[Int] = None, libId: Option[Int] = None): Future[Option[Map[String, Any]]] = {
    val l: List[Query[Stats, Stats#TableElementType, Seq] => Query[Stats, Stats#TableElementType, Seq]] = List(
      y => y.filter(_.runId === runId),
      y => y.filter(_.pipelineId === pipelineId),
      y => (if (moduleId.isDefined) y.filter(_.moduleId === moduleId) else y.filter(_.moduleId.isEmpty)),
      y => (if (sampleId.isDefined) y.filter(_.sampleId === sampleId) else y.filter(_.sampleId.isEmpty)),
      y => (if (libId.isDefined) y.filter(_.libraryId === libId) else y.filter(_.libraryId.isEmpty))
    )
    val q = l.foldLeft(stats.subquery)((a, b) => b(a))

    db.run(q.map(_.content).result).map(_.headOption.map(ConfigUtils.jsonTextToMap))
  }

  def createSetting(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                    sampleId: Option[Int] = None, libId: Option[Int] = None, content: String) = {
    db.run(settings.forceInsert(Setting(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  def getSettings(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]] = None,
                  sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None) = {
    val l: List[Option[Query[Settings, Settings#TableElementType, Seq] => Query[Settings, Settings#TableElementType, Seq]]] = List(
      runId.map(x => y => y.filter(_.runId === x)),
      pipelineId.map(x => y => y.filter(_.pipelineId === x)),
      moduleId.map(x => y => (if (x.isDefined) y.filter(_.moduleId === x) else y.filter(_.moduleId.isEmpty))),
      sampleId.map(x => y => (if (x.isDefined) y.filter(_.sampleId === x) else y.filter(_.sampleId.isEmpty))),
      libId.map(x => y => (if (x.isDefined) y.filter(_.libraryId === x) else y.filter(_.libraryId.isEmpty)))
    )
    val q = l.flatten.foldLeft(settings.subquery)((a, b) => b(a))

    db.run(q.result)
  }

  def getSetting(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None, libId: Option[Int] = None): Future[Option[Map[String, Any]]] = {
    val l: List[Query[Settings, Settings#TableElementType, Seq] => Query[Settings, Settings#TableElementType, Seq]] = List(
      _.filter(_.runId === runId),
      _.filter(_.pipelineId === pipelineId),
      y => (if (moduleId.isDefined) y.filter(_.moduleId === moduleId) else y.filter(_.moduleId.isEmpty)),
      y => (if (sampleId.isDefined) y.filter(_.sampleId === sampleId) else y.filter(_.sampleId.isEmpty)),
      y => (if (libId.isDefined) y.filter(_.libraryId === libId) else y.filter(_.libraryId.isEmpty))
    )

    val q = l.foldLeft(settings.subquery)((a, b) => b(a))
    db.run(q.map(_.content).result).map(_.headOption.map(ConfigUtils.jsonTextToMap))
  }
}

object SummaryDb {
  def openSqliteSummary(file: File): SummaryDb = {
    val exist = file.exists()
    val db = Database.forURL(s"jdbc:sqlite:${file.getAbsolutePath}", driver = "org.sqlite.JDBC")
    val s = new SummaryDb(db)
    if (!exist) s.createTables()
    s
  }
}