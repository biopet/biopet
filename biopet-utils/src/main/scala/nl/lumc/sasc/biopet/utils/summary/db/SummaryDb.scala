package nl.lumc.sasc.biopet.utils.summary.db

import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.summary.db.Schema._
import slick.driver.H2Driver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import java.io.{ Closeable, File }
import java.sql.Date

import scalaz._
import Scalaz._
import SummaryDb.Implicts._

/**
 * This class interface wityh a summary database
 *
 * Created by pjvanthof on 05/02/2017.
 */
class SummaryDb(val db: Database) extends Closeable {

  def close(): Unit = db.close()

  /** This method will create all tables */
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

  /** This method will create a new run and return the runId */
  def createRun(runName: String, outputDir: String, version: String, commitHash: String,
                creationDate: Date): Future[Int] = {
    val id = Await.result(db.run(runs.size.result), Duration.Inf)
    db.run(runs.forceInsert(Run(id, runName, outputDir, version, commitHash, creationDate))).map(_ => id)
  }

  /** This will return all runs that match the critiria given */
  def getRuns(runId: Option[Int] = None, runName: Option[String] = None, outputDir: Option[String] = None): Future[Seq[Run]] = {
    val q = runs.filter { run =>
      List(
        runId.map(run.id === _),
        runName.map(run.runName === _),
        outputDir.map(run.outputDir === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** This creates a new sample and return the sampleId */
  def createSample(name: String, runId: Int, tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(samples.size.result), Duration.Inf)
    db.run(samples.forceInsert(Sample(id, name, runId, tags))).map(_ => id)
  }

  /** This will return all samples that match given criteria */
  def getSamples(sampleId: Option[Int] = None, runId: Option[Int] = None, name: Option[String] = None): Future[Seq[Sample]] = {
    val q = samples.filter { sample =>
      List(
        sampleId.map(sample.id === _),
        runId.map(sample.runId === _),
        name.map(sample.name === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** Return samplId of a specific runId + sampleName */
  def getSampleId(runId: Int, sampleName: String): Future[Option[Int]] = {
    getSamples(runId = Some(runId), name = Some(sampleName)).map(_.headOption.map(_.id))
  }

  /** Return sampleName of a specific sampleId */
  def getSampleName(sampleId: Int): Future[Option[String]] = {
    getSamples(sampleId = Some(sampleId)).map(_.headOption.map(_.name))
  }

  /** Return sample tags of a specific sample as a map */
  def getSampleTags(sampleId: Int): Future[Option[Map[String, Any]]] = {
    db.run(samples.filter(_.id === sampleId).map(_.tags).result)
      .map(_.headOption.flatten.map(ConfigUtils.jsonTextToMap))
  }

  /** This will create a new library */
  def createLibrary(name: String, runId: Int, sampleId: Int, tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(libraries.size.result), Duration.Inf)
    db.run(libraries.forceInsert(Library(id, name, runId, sampleId, tags))).map(_ => id)
  }

  /** This returns all libraries that match the given criteria */
  def getLibraries(libId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None, sampleId: Option[Int] = None): Future[Seq[Library]] = {
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

  /** Return a libraryId for a specific combination */
  def getLibraryId(runId: Int, sampleId: Int, name: String): Future[Option[Int]] = {
    getLibraries(runId = Some(runId), sampleId = Some(sampleId), name = Some(name)).map(_.headOption.map(_.id))
  }

  /** Return a libraryId for a specific combination */
  def getLibraryName(libraryId: Int): Future[Option[String]] = {
    getLibraries(libId = Some(libraryId)).map(_.headOption.map(_.name))
  }

  /** Return library tags as a map */
  def getLibraryTags(libId: Int): Future[Option[Map[String, Any]]] = {
    db.run(libraries.filter(_.id === libId).map(_.tags).result)
      .map(_.headOption.flatten.map(ConfigUtils.jsonTextToMap))
  }

  /** Creates a new pipeline, even if it already exist. This may give a database exeption */
  def forceCreatePipeline(name: String, runId: Int): Future[Int] = {
    val id = Await.result(db.run(pipelines.size.result), Duration.Inf)
    db.run(pipelines.forceInsert(Pipeline(id, name, runId))).map(_ => id)
  }

  /** Creates a new pipeline if it does not yet exist */
  def createPipeline(name: String, runId: Int): Future[Int] = {
    getPipelines(name = Some(name), runId = Some(runId))
      .flatMap {
        m =>
          if (m.isEmpty) forceCreatePipeline(name, runId)
          else Future(m.head.id)
      }
  }

  /** Get all pipelines that match given criteria */
  def getPipelines(pipelineId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None): Future[Seq[Pipeline]] = {
    val q = pipelines.filter { lib =>
      List(
        pipelineId.map(lib.id === _),
        runId.map(lib.runId === _),
        name.map(lib.name === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** Return pipelineId of a specific pipelineName */
  def getPipelineId(runId: Int, pipelineName: String): Future[Option[Int]] = {
    getPipelines(runId = Some(runId), name = Some(pipelineName)).map(_.headOption.map(_.id))
  }

  /** Creates a new module, even if it already exist. This may give a database exeption */
  def forceCreateModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    val id = Await.result(db.run(modules.size.result), Duration.Inf)
    db.run(modules.forceInsert(Module(id, name, runId, pipelineId))).map(_ => id)
  }

  /** Creates a new module if it does not yet exist */
  def createModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    getModules(name = Some(name), runId = Some(runId), pipelineId = Some(pipelineId))
      .flatMap {
        m =>
          if (m.isEmpty) forceCreateModule(name, runId, pipelineId)
          else Future(m.head.id)
      }
  }

  /** Return all module with given criteria */
  def getModules(moduleId: Option[Int] = None, name: Option[String] = None, runId: Option[Int] = None, pipelineId: Option[Int] = None): Future[Seq[Module]] = {
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

  /** Return moduleId of a specific moduleName */
  def getmoduleId(runId: Int, moduleName: String, pipelineId: Int): Future[Option[Int]] = {
    getModules(runId = Some(runId), name = Some(moduleName), pipelineId = Some(pipelineId)).map(_.headOption.map(_.id))
  }

  /** Create a new stat in the database, This method is need checking before */
  def createStat(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None, libId: Option[Int] = None, content: String): Future[Int] = {
    db.run(stats.forceInsert(Stat(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  /** This create or update a stat */
  def createOrUpdateStat(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None, libId: Option[Int] = None, content: String): Future[Int] = {
    val filter = statsFilter(Some(runId), Some(pipelineId.left), Some(moduleId.map(_.left)), Some(sampleId.map(_.left)), Some(libId.map(_.left)))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createStat(runId, pipelineId, moduleId, sampleId, libId, content)
    else db.run(filter.map(_.content).update(content))
  }

  /** Return a Query for [[Stats]] */
  def statsFilter(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
                  sample: Option[Option[\/[Int, String]]] = None, library: Option[Option[\/[Int, String]]] = None,
                  mustHaveSample: Boolean = false, mustHaveLibrary: Boolean = false) = {
    var f: Query[Stats, Stats#TableElementType, Seq] = stats
    runId.foreach(r => f = f.filter(_.runId === r))
    f = pipeline match {
      case Some(-\/(id))   => f.filter(_.pipelineId === id)
      case Some(\/-(name)) => f.join(pipelines).on(_.pipelineId === _.id).filter(_._2.name === name).map(_._1)
      case _               => f
    }
    f = module match {
      case Some(Some(-\/(id)))   => f.filter(_.moduleId === id)
      case Some(Some(\/-(name))) => f.join(modules).on(_.moduleId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.moduleId.isEmpty)
      case _                     => f
    }
    f = sample match {
      case Some(Some(-\/(id)))   => f.filter(_.sampleId === id)
      case Some(Some(\/-(name))) => f.join(samples).on(_.sampleId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.sampleId.isEmpty)
      case _                     => f
    }
    f = library match {
      case Some(Some(-\/(id)))   => f.filter(_.libraryId === id)
      case Some(Some(\/-(name))) => f.join(libraries).on(_.libraryId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.libraryId.isEmpty)
      case _                     => f
    }

    if (mustHaveSample) f = f.filter(_.sampleId.nonEmpty)
    if (mustHaveLibrary) f = f.filter(_.libraryId.nonEmpty)
    f
  }

  /** Return all stats that match given criteria */
  def getStats(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
               sample: Option[Option[\/[Int, String]]] = None, lib: Option[Option[\/[Int, String]]] = None,
               mustHaveSample: Boolean = false, mustHaveLibrary: Boolean = false): Future[Seq[Stat]] = {
    db.run(statsFilter(runId, pipeline, module, sample, lib, mustHaveSample, mustHaveLibrary).result)
  }

  /** Return number of results */
  def getStatsSize(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
                   sample: Option[Option[\/[Int, String]]] = None, library: Option[Option[\/[Int, String]]] = None,
                   mustHaveSample: Boolean = false, mustHaveLibrary: Boolean = false): Future[Int] = {
    db.run(statsFilter(runId, pipeline, module, sample, library, mustHaveSample, mustHaveLibrary).size.result)
  }

  /** Get a single stat as [[Map[String, Any]] */
  def getStat(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None,
              sample: Option[\/[Int, String]] = None, lib: Option[\/[Int, String]] = None): Future[Option[Map[String, Any]]] = {
    getStats(Some(runId), Some(pipeline), Some(module), Some(sample), Some(lib))
      .map(_.headOption.map(x => ConfigUtils.jsonTextToMap(x.content)))
  }

  def getStatKeys(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None,
                  sample: Option[\/[Int, String]] = None, library: Option[\/[Int, String]] = None,
                  keyValues: Map[String, List[String]]): Map[String, Option[Any]] = {
    val stats = Await.result(getStat(runId, pipeline, module, sample, library), Duration.Inf)
    keyValues.map {
      case (key, path) =>
        stats match {
          case Some(map) => key -> ConfigUtils.getValueFromPath(map, path)
          case None      => key -> None
        }
    }
  }

  def getStatsForSamples(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None,
                         sample: Option[\/[Int, String]] = None, keyValues: Map[String, List[String]]) = {
    val samples = Await.result(getSamples(runId = Some(runId), sampleId = sample.flatMap(_.left.toOption), name = sample.flatMap(_.toOption)), Duration.Inf)
    (for (s <- samples) yield {
      s.id -> getStatKeys(runId, pipeline, module, Option(s.id.left), None, keyValues = keyValues)
    }).toMap
  }

  def getStatsForLibraries(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None,
                           sampleId: Option[Int] = None, keyValues: Map[String, List[String]]) = {
    val libraries = Await.result(getLibraries(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    (for (lib <- libraries) yield {
      (lib.sampleId, lib.id) -> getStatKeys(runId, pipeline, module, Some(lib.sampleId.left), Some(lib.id.left), keyValues = keyValues)
    }).toMap
  }

  /** This method creates a new setting. This method need checking before */
  def createSetting(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                    sampleId: Option[Int] = None, libId: Option[Int] = None, content: String): Future[Int] = {
    db.run(settings.forceInsert(Setting(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  def settingsFilter(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
                     sample: Option[Option[\/[Int, String]]] = None, library: Option[Option[\/[Int, String]]] = None,
                     mustHaveSample: Boolean = false, mustHaveLibrary: Boolean = false) = {
    var f: Query[Settings, Settings#TableElementType, Seq] = settings
    runId.foreach(r => f = f.filter(_.runId === r))
    f = pipeline match {
      case Some(-\/(id))   => f.filter(_.pipelineId === id)
      case Some(\/-(name)) => f.join(pipelines).on(_.pipelineId === _.id).filter(_._2.name === name).map(_._1)
      case _               => f
    }
    f = module match {
      case Some(Some(-\/(id)))   => f.filter(_.moduleId === id)
      case Some(Some(\/-(name))) => f.join(modules).on(_.moduleId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.moduleId.isEmpty)
      case _                     => f
    }
    f = sample match {
      case Some(Some(-\/(id)))   => f.filter(_.sampleId === id)
      case Some(Some(\/-(name))) => f.join(samples).on(_.sampleId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.sampleId.isEmpty)
      case _                     => f
    }
    f = library match {
      case Some(Some(-\/(id)))   => f.filter(_.libraryId === id)
      case Some(Some(\/-(name))) => f.join(libraries).on(_.libraryId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.libraryId.isEmpty)
      case _                     => f
    }

    if (mustHaveSample) f = f.filter(_.sampleId.nonEmpty)
    if (mustHaveLibrary) f = f.filter(_.libraryId.nonEmpty)
    f
  }

  /** This method creates or update a setting. */
  def createOrUpdateSetting(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                            sampleId: Option[Int] = None, libId: Option[Int] = None, content: String): Future[Int] = {
    val filter = settingsFilter(Some(runId), Some(pipelineId.left), Some(moduleId.map(_.left)), Some(sampleId.map(_.left)), Some(libId.map(_.left)))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createSetting(runId, pipelineId, moduleId, sampleId, libId, content)
    else db.run(filter.update(Setting(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  /** Return all settings that match the given criteria */
  def getSettings(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
                  sample: Option[Option[\/[Int, String]]] = None, library: Option[Option[\/[Int, String]]] = None): Future[Seq[Setting]] = {
    db.run(settingsFilter(runId, pipeline, module, sample, library).result)
  }

  /** Return a specific setting as [[Map[String, Any]] */
  def getSetting(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None,
                 sample: Option[\/[Int, String]] = None, library: Option[\/[Int, String]] = None): Future[Option[Map[String, Any]]] = {
    getSettings(Some(runId), Some(pipeline), Some(module), Some(sample), Some(library))
      .map(_.headOption.map(x => ConfigUtils.jsonTextToMap(x.content)))
  }

  def getSettingKeys(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None,
                     sample: Option[\/[Int, String]] = None, library: Option[\/[Int, String]] = None,
                     keyValues: Map[String, List[String]]): Map[String, Option[Any]] = {
    val stats = Await.result(getSetting(runId, pipeline, module, sample, library), Duration.Inf)
    keyValues.map {
      case (key, path) =>
        stats match {
          case Some(map) => key -> ConfigUtils.getValueFromPath(map, path)
          case None      => key -> None
        }
    }
  }

  def getSettingsForSamples(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None, sampleId: Option[Int] = None, keyValues: Map[String, List[String]]) = {
    val samples = Await.result(getSamples(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    (for (sample <- samples) yield {
      sample.id -> getSettingKeys(runId, pipeline, module, Some(sample.id.left), None, keyValues = keyValues)
    }).toMap
  }

  def getSettingsForLibraries(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]] = None, sampleId: Option[Int] = None, keyValues: Map[String, List[String]]) = {
    val libraries = Await.result(getLibraries(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    (for (lib <- libraries) yield {
      (lib.sampleId, lib.id) -> getSettingKeys(runId, pipeline, module, Some(lib.sampleId.left), Some(lib.id.left), keyValues = keyValues)
    }).toMap
  }

  /** Return a [[Query]] for [[Files]] */
  def filesFilter(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
                  sample: Option[Option[\/[Int, String]]] = None, library: Option[Option[\/[Int, String]]] = None,
                  key: Option[String] = None, pipelineName: Option[String] = None, moduleName: Option[Option[String]] = None,
                  sampleName: Option[Option[String]] = None, libraryName: Option[Option[String]] = None) = {
    var f: Query[Files, Files#TableElementType, Seq] = files
    runId.foreach(r => f = f.filter(_.runId === r))
    key.foreach(r => f = f.filter(_.key === r))

    f = pipeline match {
      case Some(-\/(id))   => f.filter(_.pipelineId === id)
      case Some(\/-(name)) => f.join(pipelines).on(_.pipelineId === _.id).filter(_._2.name === name).map(_._1)
      case _               => f
    }
    f = module match {
      case Some(Some(-\/(id)))   => f.filter(_.moduleId === id)
      case Some(Some(\/-(name))) => f.join(modules).on(_.moduleId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.moduleId.isEmpty)
      case _                     => f
    }
    f = sample match {
      case Some(Some(-\/(id)))   => f.filter(_.sampleId === id)
      case Some(Some(\/-(name))) => f.join(samples).on(_.sampleId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.sampleId.isEmpty)
      case _                     => f
    }
    f = library match {
      case Some(Some(-\/(id)))   => f.filter(_.libraryId === id)
      case Some(Some(\/-(name))) => f.join(libraries).on(_.libraryId === _.id).filter(_._2.name === name).map(_._1)
      case Some(None)            => f.filter(_.libraryId.isEmpty)
      case _                     => f
    }
    f
  }

  /** Returns all [[Files]] with the given criteria */
  def getFiles(runId: Option[Int] = None, pipeline: Option[\/[Int, String]] = None, module: Option[Option[\/[Int, String]]] = None,
               sample: Option[Option[\/[Int, String]]] = None, library: Option[Option[\/[Int, String]]] = None,
               key: Option[String] = None): Future[Seq[Schema.File]] = {
    db.run(filesFilter(runId, pipeline, module, sample, library, key).result)
  }

  def getFile(runId: Int, pipeline: \/[Int, String], module: Option[\/[Int, String]], sample: Option[\/[Int, String]],
              library: Option[\/[Int, String]], key: String): Future[Option[Schema.File]] = {
    db.run(filesFilter(Some(runId), Some(pipeline), Some(module), Some(sample), Some(library), Some(key)).result).map(_.headOption)
  }

  /** Creates a file. This method will raise expection if it already exist */
  def createFile(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None, libId: Option[Int] = None,
                 key: String, path: String, md5: String, link: Boolean = false, size: Long): Future[Int] = {
    db.run(files.forceInsert(Schema.File(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)))
  }

  /** Create or update a File */
  def createOrUpdateFile(runId: Int, pipelineId: Int, moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None, libId: Option[Int] = None,
                         key: String, path: String, md5: String, link: Boolean = false, size: Long): Future[Int] = {
    val filter = filesFilter(Some(runId), Some(pipelineId.left), Some(moduleId.map(_.left)), Some(sampleId.map(_.left)), Some(libId.map(_.left)), Some(key))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createFile(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)
    else db.run(filter.update(Schema.File(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)))
  }

  /** Returns a [[Query]] for [[Executables]] */
  def executablesFilter(runId: Option[Int], toolName: Option[String]) = {
    var q: Query[Executables, Executables#TableElementType, Seq] = executables
    runId.foreach(r => q = q.filter(_.runId === r))
    toolName.foreach(r => q = q.filter(_.toolName === r))
    q
  }

  /** Return all executables with given criteria */
  def getExecutables(runId: Option[Int] = None, toolName: Option[String] = None): Future[Seq[Executable]] = {
    db.run(executablesFilter(runId, toolName).result)
  }

  /** Creates a exeutable. This method will raise expection if it already exist */
  def createExecutable(runId: Int, toolName: String, version: Option[String] = None, path: Option[String] = None,
                       javaVersion: Option[String] = None, exeMd5: Option[String] = None, javaMd5: Option[String] = None, jarPath: Option[String] = None): Future[Int] = {
    db.run(executables.forceInsert(Executable(runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath)))
  }

  /** Create or update a [[Executable]] */
  def createOrUpdateExecutable(runId: Int, toolName: String, version: Option[String] = None, path: Option[String] = None,
                               javaVersion: Option[String] = None, exeMd5: Option[String] = None, javaMd5: Option[String] = None, jarPath: Option[String] = None): Future[Int] = {
    val filter = executablesFilter(Some(runId), Some(toolName))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createExecutable(runId, toolName, version, javaVersion, exeMd5, javaMd5)
    else db.run(filter.update(Executable(runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath)))
  }
}

object SummaryDb {

  object Implicts {
    implicit def toRight(x: String): \/[Int, String] = x.right
    implicit def toLeft(x: Int): \/[Int, String] = x.left
  }

  private var summaryConnections = Map[File, SummaryDb]()

  /** This closing all summary that are still in the cache */
  def closeAll(): Unit = {
    summaryConnections.foreach(_._2.close())
    summaryConnections = summaryConnections.empty
  }

  /** This will open a sqlite database and create tables when the database did not exist yet */
  def openSqliteSummary(file: File): SummaryDb = {
    if (!summaryConnections.contains(file)) {
      val config: org.sqlite.SQLiteConfig = new org.sqlite.SQLiteConfig()
      config.enforceForeignKeys(true)
      config.setBusyTimeout("10000")
      config.setSynchronous(org.sqlite.SQLiteConfig.SynchronousMode.OFF)
      val exist = file.exists()
      val db = Database.forURL(s"jdbc:sqlite:${file.getAbsolutePath}", driver = "org.sqlite.JDBC", prop = config.toProperties, executor = AsyncExecutor("single_thread", 1, 1000))
      val s = new SummaryDb(db)
      if (!exist) s.createTables()
      summaryConnections += file -> s
    }
    summaryConnections(file)
  }
}