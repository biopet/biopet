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
package nl.lumc.sasc.biopet.utils.summary.db

import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.summary.db.Schema._
import slick.driver.H2Driver.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import java.io.{Closeable, File}
import java.sql.Date

import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb._
import nl.lumc.sasc.biopet.utils.summary.db.SummaryDb.Implicts._

import scala.language.implicitConversions

/**
  * This class interface wityh a summary database
  *
  * Created by pjvanthof on 05/02/2017.
  */
trait SummaryDb extends Closeable {

  implicit val ec: ExecutionContext

  def db: Database

  def close(): Unit = db.close()

  /** This will return all runs that match the critiria given */
  def getRuns(runId: Option[Int] = None,
              runName: Option[String] = None,
              outputDir: Option[String] = None): Future[Seq[Run]] = {
    val q = runs.filter { run =>
      List(
        runId.map(run.id === _),
        runName.map(run.runName === _),
        outputDir.map(run.outputDir === _)
      ).collect({ case Some(criteria) => criteria })
        .reduceLeftOption(_ && _)
        .getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** This will return all samples that match given criteria */
  def getSamples(sampleId: Option[Int] = None,
                 runId: Option[Int] = None,
                 name: Option[String] = None): Future[Seq[Sample]] = {
    val q = samples.filter { sample =>
      List(
        sampleId.map(sample.id === _),
        runId.map(sample.runId === _),
        name.map(sample.name === _)
      ).collect({ case Some(criteria) => criteria })
        .reduceLeftOption(_ && _)
        .getOrElse(true: Rep[Boolean])
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

  /** This returns all libraries that match the given criteria */
  def getLibraries(libId: Option[Int] = None,
                   name: Option[String] = None,
                   runId: Option[Int] = None,
                   sampleId: Option[Int] = None): Future[Seq[Library]] = {
    val q = libraries.filter { lib =>
      List(
        libId.map(lib.id === _),
        sampleId.map(lib.sampleId === _),
        runId.map(lib.runId === _),
        name.map(lib.name === _) // not a condition as `criteriaRoast` evaluates to `None`
      ).collect({ case Some(criteria) => criteria })
        .reduceLeftOption(_ && _)
        .getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** Return a libraryId for a specific combination */
  def getLibraryId(runId: Int, sampleId: Int, name: String): Future[Option[Int]] = {
    getLibraries(runId = Some(runId), sampleId = Some(sampleId), name = Some(name))
      .map(_.headOption.map(_.id))
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

  /** Get all pipelines that match given criteria */
  def getPipelines(pipelineId: Option[Int] = None,
                   name: Option[String] = None,
                   runId: Option[Int] = None): Future[Seq[Pipeline]] = {
    val q = pipelines.filter { lib =>
      List(
        pipelineId.map(lib.id === _),
        runId.map(lib.runId === _),
        name.map(lib.name === _)
      ).collect({ case Some(criteria) => criteria })
        .reduceLeftOption(_ && _)
        .getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** Return pipelineId of a specific pipelineName */
  def getPipelineId(runId: Int, pipelineName: String): Future[Option[Int]] = {
    getPipelines(runId = Some(runId), name = Some(pipelineName)).map(_.headOption.map(_.id))
  }

  /** Return name of a pipeline */
  def getPipelineName(pipelineId: Int): Future[Option[String]] = {
    getPipelines(pipelineId = Some(pipelineId)).map(_.headOption.map(_.name))
  }

  /** Return all module with given criteria */
  def getModules(moduleId: Option[Int] = None,
                 name: Option[String] = None,
                 runId: Option[Int] = None,
                 pipelineId: Option[Int] = None): Future[Seq[Module]] = {
    val q = modules.filter { lib =>
      List(
        moduleId.map(lib.id === _),
        runId.map(lib.runId === _),
        pipelineId.map(lib.pipelineId === _),
        name.map(lib.name === _)
      ).collect({ case Some(criteria) => criteria })
        .reduceLeftOption(_ && _)
        .getOrElse(true: Rep[Boolean])
    }
    db.run(q.result)
  }

  /** Return moduleId of a specific moduleName */
  def getmoduleId(runId: Int, moduleName: String, pipelineId: Int): Future[Option[Int]] = {
    getModules(runId = Some(runId), name = Some(moduleName), pipelineId = Some(pipelineId))
      .map(_.headOption.map(_.id))
  }

  /** Returns name of a module */
  def getModuleName(pipelineId: Int, moduleId: Int): Future[Option[String]] = {
    getModules(pipelineId = Some(pipelineId), moduleId = Some(moduleId))
      .map(_.headOption.map(_.name))
  }

  /** Return a Query for [[Stats]] */
  def statsFilter(
      runId: Option[Int] = None,
      pipeline: Option[PipelineQuery] = None,
      module: Option[ModuleQuery] = None,
      sample: Option[SampleQuery] = None,
      library: Option[LibraryQuery] = None,
      mustHaveSample: Boolean = false,
      mustHaveLibrary: Boolean = false): slick.driver.H2Driver.api.Query[Stats, Stat, Seq] = {
    var f: Query[Stats, Stats#TableElementType, Seq] = stats
    runId.foreach(r => f = f.filter(_.runId === r))
    f = pipeline match {
      case Some(p: PipelineId) => f.filter(_.pipelineId === p.id)
      case Some(p: PipelineName) =>
        f.join(pipelines).on(_.pipelineId === _.id).filter(_._2.name === p.name).map(_._1)
      case _ => f
    }
    f = module match {
      case Some(m: ModuleId) => f.filter(_.moduleId === m.id)
      case Some(m: ModuleName) =>
        f.join(modules).on(_.moduleId === _.id).filter(_._2.name === m.name).map(_._1)
      case Some(NoModule) => f.filter(_.moduleId.isEmpty)
      case _ => f
    }
    f = sample match {
      case Some(s: SampleId) => f.filter(_.sampleId === s.id)
      case Some(s: SampleName) =>
        f.join(samples).on(_.sampleId === _.id).filter(_._2.name === s.name).map(_._1)
      case Some(NoSample) => f.filter(_.sampleId.isEmpty)
      case _ => f
    }
    f = library match {
      case Some(l: LibraryId) => f.filter(_.libraryId === l.id)
      case Some(l: LibraryName) =>
        f.join(libraries).on(_.libraryId === _.id).filter(_._2.name === l.name).map(_._1)
      case Some(NoLibrary) => f.filter(_.libraryId.isEmpty)
      case _ => f
    }

    if (mustHaveSample) f = f.filter(_.sampleId.nonEmpty)
    if (mustHaveLibrary) f = f.filter(_.libraryId.nonEmpty)
    f
  }

  /** Return all stats that match given criteria */
  def getStats(runId: Option[Int] = None,
               pipeline: Option[PipelineQuery] = None,
               module: Option[ModuleQuery] = None,
               sample: Option[SampleQuery] = None,
               library: Option[LibraryQuery] = None,
               mustHaveSample: Boolean = false,
               mustHaveLibrary: Boolean = false): Future[Seq[Stat]] = {
    db.run(
      statsFilter(runId, pipeline, module, sample, library, mustHaveSample, mustHaveLibrary).result)
  }

  /** Return number of results */
  def getStatsSize(runId: Option[Int] = None,
                   pipeline: Option[PipelineQuery] = None,
                   module: Option[ModuleQuery] = None,
                   sample: Option[SampleQuery] = None,
                   library: Option[LibraryQuery] = None,
                   mustHaveSample: Boolean = false,
                   mustHaveLibrary: Boolean = false): Future[Int] = {
    db.run(
      statsFilter(runId, pipeline, module, sample, library, mustHaveSample, mustHaveLibrary).size.result)
  }

  /** Get a single stat as [[Map[String, Any]] */
  def getStat(runId: Int,
              pipeline: PipelineQuery,
              module: ModuleQuery = NoModule,
              sample: SampleQuery = NoSample,
              library: LibraryQuery = NoLibrary): Future[Option[Map[String, Any]]] = {
    getStats(Some(runId), pipeline, module, sample, library)
      .map(_.headOption.map(x => ConfigUtils.jsonTextToMap(x.content)))
  }

  def getStatKeys(runId: Int,
                  pipeline: PipelineQuery,
                  module: ModuleQuery = NoModule,
                  sample: SampleQuery = NoSample,
                  library: LibraryQuery = NoLibrary,
                  keyValues: Map[String, List[String]]): Map[String, Option[Any]] = {
    val stats = Await.result(getStat(runId, pipeline, module, sample, library), Duration.Inf)
    if (module == ModuleName("rna")) {
      ""
    }
    keyValues.map {
      case (key, path) =>
        stats match {
          case Some(map) => key -> ConfigUtils.getValueFromPath(map, path)
          case None => key -> None
        }
    }
  }

  def getStatsForSamples(
      runId: Int,
      pipeline: PipelineQuery,
      module: ModuleQuery = NoModule,
      sample: Option[SampleQuery] = None,
      keyValues: Map[String, List[String]]): Map[Int, Map[String, Option[Any]]] = {
    val samples = Await.result(getSamples(runId = Some(runId), sampleId = sample.collect {
      case s: SampleId => s.id
    }, name = sample.collect { case s: SampleName => s.name }), Duration.Inf)
    (for (s <- samples) yield {
      s.id -> getStatKeys(runId,
                          pipeline,
                          module,
                          SampleId(s.id),
                          NoLibrary,
                          keyValues = keyValues)
    }).toMap
  }

  def getStatsForLibraries(
      runId: Int,
      pipeline: PipelineQuery,
      module: ModuleQuery = NoModule,
      sampleId: Option[Int] = None,
      keyValues: Map[String, List[String]]): Map[(Int, Int), Map[String, Option[Any]]] = {
    val libraries =
      Await.result(getLibraries(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    (for (lib <- libraries) yield {
      (lib.sampleId, lib.id) -> getStatKeys(runId,
                                            pipeline,
                                            module,
                                            SampleId(lib.sampleId),
                                            LibraryId(lib.id),
                                            keyValues = keyValues)
    }).toMap
  }

  def settingsFilter(runId: Option[Int] = None,
                     pipeline: Option[PipelineQuery] = None,
                     module: Option[ModuleQuery] = None,
                     sample: Option[SampleQuery] = None,
                     library: Option[LibraryQuery] = None,
                     mustHaveSample: Boolean = false,
                     mustHaveLibrary: Boolean = false)
    : slick.driver.H2Driver.api.Query[Settings, Setting, Seq] = {
    var f: Query[Settings, Settings#TableElementType, Seq] = settings
    runId.foreach(r => f = f.filter(_.runId === r))
    f = pipeline match {
      case Some(p: PipelineId) => f.filter(_.pipelineId === p.id)
      case Some(p: PipelineName) =>
        f.join(pipelines).on(_.pipelineId === _.id).filter(_._2.name === p.name).map(_._1)
      case _ => f
    }
    f = module match {
      case Some(m: ModuleId) => f.filter(_.moduleId === m.id)
      case Some(m: ModuleName) =>
        f.join(modules).on(_.moduleId === _.id).filter(_._2.name === m.name).map(_._1)
      case Some(NoModule) => f.filter(_.moduleId.isEmpty)
      case _ => f
    }
    f = sample match {
      case Some(s: SampleId) => f.filter(_.sampleId === s.id)
      case Some(s: SampleName) =>
        f.join(samples).on(_.sampleId === _.id).filter(_._2.name === s.name).map(_._1)
      case Some(NoSample) => f.filter(_.sampleId.isEmpty)
      case _ => f
    }
    f = library match {
      case Some(l: LibraryId) => f.filter(_.libraryId === l.id)
      case Some(l: LibraryName) =>
        f.join(libraries).on(_.libraryId === _.id).filter(_._2.name === l.name).map(_._1)
      case Some(NoLibrary) => f.filter(_.libraryId.isEmpty)
      case _ => f
    }

    if (mustHaveSample) f = f.filter(_.sampleId.nonEmpty)
    if (mustHaveLibrary) f = f.filter(_.libraryId.nonEmpty)
    f
  }

  /** Return all settings that match the given criteria */
  def getSettings(runId: Option[Int] = None,
                  pipeline: Option[PipelineQuery] = None,
                  module: Option[ModuleQuery] = None,
                  sample: Option[SampleQuery] = None,
                  library: Option[LibraryQuery] = None): Future[Seq[Setting]] = {
    db.run(settingsFilter(runId, pipeline, module, sample, library).result)
  }

  /** Return a specific setting as [[Map[String, Any]] */
  def getSetting(runId: Int,
                 pipeline: PipelineQuery,
                 module: ModuleQuery = NoModule,
                 sample: SampleQuery = NoSample,
                 library: LibraryQuery = NoLibrary): Future[Option[Map[String, Any]]] = {
    getSettings(Some(runId), Some(pipeline), module, sample, library)
      .map(_.headOption.map(x => ConfigUtils.jsonTextToMap(x.content)))
  }

  def getSettingKeys(runId: Int,
                     pipeline: PipelineQuery,
                     module: ModuleQuery = NoModule,
                     sample: SampleQuery = NoSample,
                     library: LibraryQuery = NoLibrary,
                     keyValues: Map[String, List[String]]): Map[String, Option[Any]] = {
    val stats = Await.result(getSetting(runId, pipeline, module, sample, library), Duration.Inf)
    keyValues.map {
      case (key, path) =>
        stats match {
          case Some(map) => key -> ConfigUtils.getValueFromPath(map, path)
          case None => key -> None
        }
    }
  }

  def getSettingsForSamples(
      runId: Int,
      pipeline: PipelineQuery,
      module: ModuleQuery = NoModule,
      sampleId: Option[Int] = None,
      keyValues: Map[String, List[String]]): Map[Int, Map[String, Option[Any]]] = {
    val samples = Await.result(getSamples(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    (for (sample <- samples) yield {
      sample.id -> getSettingKeys(runId,
                                  pipeline,
                                  module,
                                  SampleId(sample.id),
                                  NoLibrary,
                                  keyValues = keyValues)
    }).toMap
  }

  def getSettingsForLibraries(
      runId: Int,
      pipeline: PipelineQuery,
      module: ModuleQuery = NoModule,
      sampleId: Option[Int] = None,
      keyValues: Map[String, List[String]]): Map[(Int, Int), Map[String, Option[Any]]] = {
    val libraries =
      Await.result(getLibraries(runId = Some(runId), sampleId = sampleId), Duration.Inf)
    (for (lib <- libraries) yield {
      (lib.sampleId, lib.id) -> getSettingKeys(runId,
                                               pipeline,
                                               module,
                                               SampleId(lib.sampleId),
                                               LibraryId(lib.id),
                                               keyValues = keyValues)
    }).toMap
  }

  /** Return a [[Query]] for [[Files]] */
  def filesFilter(runId: Option[Int] = None,
                  pipeline: Option[PipelineQuery] = None,
                  module: Option[ModuleQuery] = None,
                  sample: Option[SampleQuery] = None,
                  library: Option[LibraryQuery] = None,
                  key: Option[String] = None,
                  pipelineName: Option[String] = None,
                  moduleName: Option[Option[String]] = None,
                  sampleName: Option[Option[String]] = None,
                  libraryName: Option[Option[String]] = None)
    : slick.driver.H2Driver.api.Query[Files, Files#TableElementType, Seq] = {
    var f: Query[Files, Files#TableElementType, Seq] = files
    runId.foreach(r => f = f.filter(_.runId === r))
    key.foreach(r => f = f.filter(_.key === r))

    f = pipeline match {
      case Some(p: PipelineId) => f.filter(_.pipelineId === p.id)
      case Some(p: PipelineName) =>
        f.join(pipelines).on(_.pipelineId === _.id).filter(_._2.name === p.name).map(_._1)
      case _ => f
    }
    f = module match {
      case Some(m: ModuleId) => f.filter(_.moduleId === m.id)
      case Some(m: ModuleName) =>
        f.join(modules).on(_.moduleId === _.id).filter(_._2.name === m.name).map(_._1)
      case Some(NoModule) => f.filter(_.moduleId.isEmpty)
      case _ => f
    }
    f = sample match {
      case Some(s: SampleId) => f.filter(_.sampleId === s.id)
      case Some(s: SampleName) =>
        f.join(samples).on(_.sampleId === _.id).filter(_._2.name === s.name).map(_._1)
      case Some(NoSample) => f.filter(_.sampleId.isEmpty)
      case _ => f
    }
    f = library match {
      case Some(l: LibraryId) => f.filter(_.libraryId === l.id)
      case Some(l: LibraryName) =>
        f.join(libraries).on(_.libraryId === _.id).filter(_._2.name === l.name).map(_._1)
      case Some(NoLibrary) => f.filter(_.libraryId.isEmpty)
      case _ => f
    }
    f
  }

  /** Returns all [[Files]] with the given criteria */
  def getFiles(runId: Option[Int] = None,
               pipeline: Option[PipelineQuery] = None,
               module: Option[ModuleQuery] = None,
               sample: Option[SampleQuery] = None,
               library: Option[LibraryQuery] = None,
               key: Option[String] = None): Future[Seq[Schema.File]] = {
    db.run(filesFilter(runId, pipeline, module, sample, library, key).result)
  }

  def getFile(runId: Int,
              pipeline: PipelineQuery,
              module: ModuleQuery = NoModule,
              sample: SampleQuery = NoSample,
              library: LibraryQuery = NoLibrary,
              key: String): Future[Option[Schema.File]] = {
    db.run(
        filesFilter(Some(runId),
                    Some(pipeline),
                    Some(module),
                    Some(sample),
                    Some(library),
                    Some(key)).result)
      .map(_.headOption)
  }

  /** Returns a [[Query]] for [[Executables]] */
  def executablesFilter(
      runId: Option[Int],
      toolName: Option[String]): slick.driver.H2Driver.api.Query[Executables, Executable, Seq] = {
    var q: Query[Executables, Executables#TableElementType, Seq] = executables
    runId.foreach(r => q = q.filter(_.runId === r))
    toolName.foreach(r => q = q.filter(_.toolName === r))
    q
  }

  /** Return all executables with given criteria */
  def getExecutables(runId: Option[Int] = None,
                     toolName: Option[String] = None): Future[Seq[Executable]] = {
    db.run(executablesFilter(runId, toolName).result)
  }

}

class SummaryDbReadOnly(val db: Database)(implicit val ec: ExecutionContext) extends SummaryDb

class SummaryDbWrite(val db: Database)(implicit val ec: ExecutionContext) extends SummaryDb {

  /** This method will create all tables */
  def createTables(): Unit = {
    val setup = DBIO.seq(
      (runs.schema ++ samples.schema ++
        libraries.schema ++ pipelines.schema ++
        modules.schema ++ stats.schema ++ settings.schema ++
        files.schema ++ executables.schema).create
    )
    val setupFuture = db.run(setup)
    Await.result(setupFuture, Duration.Inf)
  }

  /** This method will create a new run and return the runId */
  def createRun(runName: String,
                outputDir: String,
                version: String,
                commitHash: String,
                creationDate: Date): Future[Int] = {
    val id = Await.result(db.run(runs.size.result), Duration.Inf)
    db.run(runs.forceInsert(Run(id, runName, outputDir, version, commitHash, creationDate)))
      .map(_ => id)
  }

  /** This creates a new sample and return the sampleId */
  def createSample(name: String, runId: Int, tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(samples.size.result), Duration.Inf)
    db.run(samples.forceInsert(Sample(id, name, runId, tags))).map(_ => id)
  }

  def createOrUpdateSample(name: String, runId: Int, tags: Option[String] = None): Future[Int] = {
    getSampleId(runId, name).flatMap {
      case Some(id: Int) =>
        db.run(samples.filter(_.name === name).filter(_.id === id).map(_.tags).update(tags))
          .map(_ => id)
      case _ => createSample(name, runId, tags)
    }
  }

  /** This will create a new library */
  def createLibrary(name: String,
                    runId: Int,
                    sampleId: Int,
                    tags: Option[String] = None): Future[Int] = {
    val id = Await.result(db.run(libraries.size.result), Duration.Inf)
    db.run(libraries.forceInsert(Library(id, name, runId, sampleId, tags))).map(_ => id)
  }

  def createOrUpdateLibrary(name: String,
                            runId: Int,
                            sampleId: Int,
                            tags: Option[String] = None): Future[Int] = {
    getLibraryId(runId, sampleId, name).flatMap {
      case Some(id: Int) =>
        db.run(
            libraries
              .filter(_.name === name)
              .filter(_.id === id)
              .filter(_.sampleId === sampleId)
              .map(_.tags)
              .update(tags))
          .map(_ => id)
      case _ => createLibrary(name, runId, sampleId, tags)
    }
  }

  /** Creates a new pipeline, even if it already exist. This may give a database exeption */
  def forceCreatePipeline(name: String, runId: Int): Future[Int] = {
    val id = Await.result(db.run(pipelines.size.result), Duration.Inf)
    db.run(pipelines.forceInsert(Pipeline(id, name, runId))).map(_ => id)
  }

  /** Creates a new pipeline if it does not yet exist */
  def createPipeline(name: String, runId: Int): Future[Int] = {
    getPipelines(name = Some(name), runId = Some(runId))
      .flatMap { m =>
        if (m.isEmpty) forceCreatePipeline(name, runId)
        else Future(m.head.id)
      }
  }

  /** Creates a new module, even if it already exist. This may give a database exeption */
  def forceCreateModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    val id = Await.result(db.run(modules.size.result), Duration.Inf)
    db.run(modules.forceInsert(Module(id, name, runId, pipelineId))).map(_ => id)
  }

  /** Creates a new module if it does not yet exist */
  def createModule(name: String, runId: Int, pipelineId: Int): Future[Int] = {
    getModules(name = Some(name), runId = Some(runId), pipelineId = Some(pipelineId))
      .flatMap { m =>
        if (m.isEmpty) forceCreateModule(name, runId, pipelineId)
        else Future(m.head.id)
      }
  }

  /** Create a new stat in the database, This method is need checking before */
  def createStat(runId: Int,
                 pipelineId: Int,
                 moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None,
                 libId: Option[Int] = None,
                 content: String): Future[Int] = {
    db.run(stats.forceInsert(Stat(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  /** This create or update a stat */
  def createOrUpdateStat(runId: Int,
                         pipelineId: Int,
                         moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None,
                         libId: Option[Int] = None,
                         content: String): Future[Int] = {
    val filter = statsFilter(
      Some(runId),
      pipelineId,
      Some(moduleId.map(ModuleId).getOrElse(NoModule)),
      Some(sampleId.map(SampleId).getOrElse(NoSample)),
      Some(libId.map(LibraryId).getOrElse(NoLibrary))
    )
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createStat(runId, pipelineId, moduleId, sampleId, libId, content)
    else db.run(filter.map(_.content).update(content))
  }

  /** This method creates a new setting. This method need checking before */
  def createSetting(runId: Int,
                    pipelineId: Int,
                    moduleId: Option[Int] = None,
                    sampleId: Option[Int] = None,
                    libId: Option[Int] = None,
                    content: String): Future[Int] = {
    db.run(settings.forceInsert(Setting(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  /** This method creates or update a setting. */
  def createOrUpdateSetting(runId: Int,
                            pipelineId: Int,
                            moduleId: Option[Int] = None,
                            sampleId: Option[Int] = None,
                            libId: Option[Int] = None,
                            content: String): Future[Int] = {
    val filter = settingsFilter(Some(runId),
                                PipelineId(pipelineId),
                                moduleId.map(ModuleId),
                                sampleId.map(SampleId),
                                libId.map(LibraryId))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createSetting(runId, pipelineId, moduleId, sampleId, libId, content)
    else db.run(filter.update(Setting(runId, pipelineId, moduleId, sampleId, libId, content)))
  }

  /** Creates a file. This method will raise expection if it already exist */
  def createFile(runId: Int,
                 pipelineId: Int,
                 moduleId: Option[Int] = None,
                 sampleId: Option[Int] = None,
                 libId: Option[Int] = None,
                 key: String,
                 path: String,
                 md5: String,
                 link: Boolean = false,
                 size: Long): Future[Int] = {
    db.run(
      files.forceInsert(
        Schema.File(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)))
  }

  /** Create or update a File */
  def createOrUpdateFile(runId: Int,
                         pipelineId: Int,
                         moduleId: Option[Int] = None,
                         sampleId: Option[Int] = None,
                         libId: Option[Int] = None,
                         key: String,
                         path: String,
                         md5: String,
                         link: Boolean = false,
                         size: Long): Future[Int] = {
    val filter = filesFilter(Some(runId),
                             PipelineId(pipelineId),
                             moduleId.map(ModuleId),
                             sampleId.map(SampleId),
                             libId.map(LibraryId),
                             Some(key))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0)
      createFile(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)
    else
      db.run(
        filter.update(
          Schema.File(runId, pipelineId, moduleId, sampleId, libId, key, path, md5, link, size)))
  }

  /** Creates a exeutable. This method will raise expection if it already exist */
  def createExecutable(runId: Int,
                       toolName: String,
                       version: Option[String] = None,
                       path: Option[String] = None,
                       javaVersion: Option[String] = None,
                       exeMd5: Option[String] = None,
                       javaMd5: Option[String] = None,
                       jarPath: Option[String] = None): Future[Int] = {
    db.run(
      executables.forceInsert(
        Executable(runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath)))
  }

  /** Create or update a [[Executable]] */
  def createOrUpdateExecutable(runId: Int,
                               toolName: String,
                               version: Option[String] = None,
                               path: Option[String] = None,
                               javaVersion: Option[String] = None,
                               exeMd5: Option[String] = None,
                               javaMd5: Option[String] = None,
                               jarPath: Option[String] = None): Future[Int] = {
    val filter = executablesFilter(Some(runId), Some(toolName))
    val r = Await.result(db.run(filter.size.result), Duration.Inf)
    if (r == 0) createExecutable(runId, toolName, version, javaVersion, exeMd5, javaMd5)
    else
      db.run(
        filter.update(
          Executable(runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath)))
  }

}

object SummaryDb {

  trait PipelineQuery
  case class PipelineId(id: Int) extends PipelineQuery
  case class PipelineName(name: String) extends PipelineQuery

  trait SampleQuery
  case object NoSample extends SampleQuery
  case class SampleId(id: Int) extends SampleQuery
  case class SampleName(name: String) extends SampleQuery

  trait LibraryQuery
  case object NoLibrary extends LibraryQuery
  case class LibraryId(id: Int) extends LibraryQuery
  case class LibraryName(name: String) extends LibraryQuery

  trait ModuleQuery
  case object NoModule extends ModuleQuery
  case class ModuleId(id: Int) extends ModuleQuery
  case class ModuleName(name: String) extends ModuleQuery

  object Implicts {

    implicit def intToPipelineQuery(x: Int): PipelineQuery = PipelineId(x)
    implicit def stringToPipelineQuery(x: String): PipelineQuery = PipelineName(x)
    implicit def intToOptionPipelineQuery(x: Int): Option[PipelineQuery] = Some(PipelineId(x))
    implicit def stringToOptionPipelineQuery(x: String): Option[PipelineQuery] =
      Some(PipelineName(x))
    implicit def sampleQueryToOptionPipelineQuery(x: PipelineQuery): Option[PipelineQuery] =
      Some(x)

    implicit def intToModuleQuery(x: Int): ModuleQuery = ModuleId(x)
    implicit def stringToModuleQuery(x: String): ModuleQuery = ModuleName(x)
    implicit def intToOptionModuleQuery(x: Int): Option[ModuleQuery] = Some(ModuleId(x))
    implicit def intToOptionModuleQuery(x: String): Option[ModuleQuery] = Some(ModuleName(x))
    implicit def moduleQueryToOptionModuleQuery(x: ModuleQuery): Option[ModuleQuery] = Some(x)

    implicit def intToSampleQuery(x: Int): SampleQuery = SampleId(x)
    implicit def stringToSampleQuery(x: String): SampleQuery = SampleName(x)
    implicit def intToOptionSampleQuery(x: Int): Option[SampleQuery] = Some(SampleId(x))
    implicit def stringToOptionSampleQuery(x: String): Option[SampleQuery] = Some(SampleName(x))
    implicit def sampleQueryToOptionSampleQuery(x: SampleQuery): Option[SampleQuery] = Some(x)

    implicit def intToLibraryQuery(x: Int): LibraryQuery = LibraryId(x)
    implicit def stringToLibraryQuery(x: String): LibraryQuery = LibraryName(x)
    implicit def intToOptionLibraryQuery(x: Int): Option[LibraryQuery] = Some(LibraryId(x))
    implicit def stringToOptionLibraryQuery(x: String): Option[LibraryQuery] = Some(LibraryName(x))
    implicit def libraryQueryToOptionLibraryQuery(x: LibraryQuery): Option[LibraryQuery] = Some(x)
  }

  private var summaryConnections = Map[File, SummaryDbWrite]()

  /** This closing all summary that are still in the cache */
  def closeAll(): Unit = {
    summaryConnections.foreach(_._2.close())
    summaryConnections = summaryConnections.empty
  }

  /** This will open a sqlite database and create tables when the database did not exist yet */
  def openSqliteSummary(file: File)(implicit ec: ExecutionContext): SummaryDbWrite = {
    if (!summaryConnections.contains(file)) {
      val config: org.sqlite.SQLiteConfig = new org.sqlite.SQLiteConfig()
      config.enforceForeignKeys(true)
      config.setBusyTimeout("10000")
      config.setSynchronous(org.sqlite.SQLiteConfig.SynchronousMode.FULL)
      val exist = file.exists()
      val db = Database.forURL(s"jdbc:sqlite:${file.getAbsolutePath}",
                               driver = "org.sqlite.JDBC",
                               prop = config.toProperties,
                               executor = AsyncExecutor("single_thread", 1, 1000))
      val s = new SummaryDbWrite(db)
      if (!exist) s.createTables()
      summaryConnections += file -> s
    }
    summaryConnections(file)
  }

  def openReadOnlySqliteSummary(file: File)(implicit ec: ExecutionContext): SummaryDbReadOnly = {
    require(file.exists(), s"File does not exist: $file")
    val config: org.sqlite.SQLiteConfig = new org.sqlite.SQLiteConfig()
    config.enforceForeignKeys(true)
    config.setBusyTimeout("10000")
    config.setSynchronous(org.sqlite.SQLiteConfig.SynchronousMode.FULL)
    config.setReadOnly(true)

    val asyncExecutor = new AsyncExecutor {
      override def executionContext: ExecutionContext = ec
      override def close(): Unit = {}
    }

    val db = Database.forURL(s"jdbc:sqlite:${file.getAbsolutePath}",
                             driver = "org.sqlite.JDBC",
                             prop = config.toProperties,
                             executor = asyncExecutor)
    new SummaryDbReadOnly(db)
  }
}
