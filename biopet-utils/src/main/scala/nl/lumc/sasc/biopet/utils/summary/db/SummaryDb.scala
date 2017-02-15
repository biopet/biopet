package nl.lumc.sasc.biopet.utils.summary.db

import java.io.{ Closeable, File }

import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.summary.db.Schema._
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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

  def getSampleId(runId: Int, sampleName: String) = {
    getSamples(runId = Some(runId), name = Some(sampleName)).map(_.headOption.map(_.id))
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

  def getLibraryId(runId: Int, sampleId: Int, name: String) = {
    getLibraries(runId = Some(runId), sampleId = Some(sampleId), name = Some(name)).map(_.headOption.map(_.id))
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
    var f: Query[Stats, Stats#TableElementType, Seq] = stats
    runId.foreach(r => f = f.filter(_.runId === r))
    pipelineId.foreach(r => f = f.filter(_.pipelineId === r))
    moduleId.foreach(r => f = (if (r.isDefined) f.filter(_.moduleId === r.get) else f.filter(_.moduleId.isEmpty)))
    sampleId.foreach(r => f = (if (r.isDefined) f.filter(_.sampleId === r.get) else f.filter(_.sampleId.isEmpty)))
    libId.foreach(r => f = (if (r.isDefined) f.filter(_.libraryId === r.get) else f.filter(_.libraryId.isEmpty)))
    f
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

  def settingsFilter(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]] = None,
                     sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None) = {
    var f: Query[Settings, Settings#TableElementType, Seq] = settings
    runId.foreach(r => f = f.filter(_.runId === r))
    pipelineId.foreach(r => f = f.filter(_.pipelineId === r))
    moduleId.foreach(r => f = (if (r.isDefined) f.filter(_.moduleId === r.get) else f.filter(_.moduleId.isEmpty)))
    sampleId.foreach(r => f = (if (r.isDefined) f.filter(_.sampleId === r.get) else f.filter(_.sampleId.isEmpty)))
    libId.foreach(r => f = (if (r.isDefined) f.filter(_.libraryId === r.get) else f.filter(_.libraryId.isEmpty)))
    f
  }

  def createOrUpdateSetting(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None, libId: Option[Int] = None, content: String) = {
    val filter = settingsFilter(Some(runId), Some(pipelineId), Some(moduleId), Some(sampleId), Some(libId))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createSetting(runId, pipelineId, moduleId, sampleId, libId, content)
    else db.run(filter.update(Setting(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  def getSettings(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]] = None,
                  sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None) = {
    db.run(settingsFilter(runId, pipelineId, moduleId, sampleId, libId).result)
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

  def filesFilter(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]],
                  sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None,
                  key: Option[String] = None) = {
    var f: Query[Files, Files#TableElementType, Seq] = files
    runId.foreach(r => f = f.filter(_.runId === r))
    pipelineId.foreach(r => f = f.filter(_.pipelineId === r))
    key.foreach(r => f = f.filter(_.key === r))
    moduleId.foreach(r => f = (if (r.isDefined) f.filter(_.moduleId === r.get) else f.filter(_.moduleId.isEmpty)))
    sampleId.foreach(r => f = (if (r.isDefined) f.filter(_.sampleId === r.get) else f.filter(_.sampleId.isEmpty)))
    libId.foreach(r => f = (if (r.isDefined) f.filter(_.libraryId === r.get) else f.filter(_.libraryId.isEmpty)))
    f
  }

  def getFiles(runId: Option[Int] = None, pipelineId: Option[Int] = None, moduleId: Option[Option[Int]],
               sampleId: Option[Option[Int]] = None, libId: Option[Option[Int]] = None,
               key: Option[String] = None) = {
    db.run(filesFilter(runId, pipelineId, moduleId, sampleId, libId, key).result)
  }

  def createFile(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None, libId: Option[Int] = None,
                 key:String, path: String, md5: String, link: Boolean = false, size: Long) = {
    db.run(files.forceInsert(Schema.File(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)))
  }

  def createOrUpdateFile(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None, libId: Option[Int] = None,
                         key:String, path: String, md5: String, link: Boolean = false, size: Long) = {
    val filter = filesFilter(Some(runId), Some(pipelineId), Some(moduleId), Some(sampleId), Some(libId), Some(key))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createFile(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)
    else db.run(filter.update(Schema.File(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)))
  }

  def executablesFilter(runId: Option[Int], toolName: Option[String]) = {
    var q: Query[Executables, Executables#TableElementType, Seq] = executables
    runId.foreach(r => q = q.filter(_.runId === r))
    toolName.foreach(r => q = q.filter(_.toolName === r))
    q
  }

  def getExecutables(runId: Option[Int] = None, toolName: Option[String] = None) = {
    db.run(executablesFilter(runId, toolName).result)
  }

  def createExecutable(runId: Int, toolName: String, version: Option[String] = None, path: Option[String] = None,
                       javaVersion: Option[String] = None, exeMd5: Option[String] = None, javaMd5: Option[String] = None, jarPath: Option[String] = None) = {
    db.run(executables.forceInsert(Executable(runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath)))
  }

  def createOrUpdateExecutable(runId: Int, toolName: String, version: Option[String] = None, path: Option[String] = None,
                               javaVersion: Option[String] = None, exeMd5: Option[String] = None, javaMd5: Option[String] = None, jarPath: Option[String] = None) = {
    val filter = executablesFilter(Some(runId), Some(toolName))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createExecutable(runId, toolName, version, javaVersion, exeMd5, javaMd5)
    else db.run(filter.update(Executable(runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath)))
  }
}

object SummaryDb {
  private var summaryConnections = Map[File, SummaryDb]()

  def closeAll(): Unit = {
    summaryConnections.foreach(_._2.close())
  }

  def openSqliteSummary(file: File): SummaryDb = {
    if (!summaryConnections.contains(file)) {
      val exist = file.exists()
      val db = Database.forURL(s"jdbc:sqlite:${file.getAbsolutePath}", driver = "org.sqlite.JDBC")
      val s = new SummaryDb(db)
      if (!exist) s.createTables()
      summaryConnections += file -> s
    }
    summaryConnections(file)
  }
}