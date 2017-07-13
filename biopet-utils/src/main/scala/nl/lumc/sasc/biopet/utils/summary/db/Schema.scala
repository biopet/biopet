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

import java.sql.Date

import slick.driver.H2Driver.api._
import slick.lifted.{Index, MappedProjection}

/**
  * Created by pjvan_thof on 27-1-17.
  */
object Schema {

  case class Run(id: Int,
                 name: String,
                 outputDir: String,
                 version: String,
                 commitHash: String,
                 creationDate: Date)
  class Runs(tag: Tag) extends Table[Run](tag, "Runs") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    def runName: Rep[String] = column[String]("runName")
    def outputDir: Rep[String] = column[String]("outputDir")
    def version: Rep[String] = column[String]("version")
    def commitHash: Rep[String] = column[String]("commitHash")
    def creationDate: Rep[Date] = column[Date]("creationDate")

    def * : MappedProjection[Run, (Int, String, String, String, String, Date)] =
      (id, runName, outputDir, version, commitHash, creationDate) <> (Run.tupled, Run.unapply)
  }
  val runs: TableQuery[Runs] = TableQuery[Runs]

  case class Sample(id: Int, name: String, runId: Int, tags: Option[String])
  class Samples(tag: Tag) extends Table[Sample](tag, "Samples") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    def name: Rep[String] = column[String]("name")
    def runId: Rep[Int] = column[Int]("runId")
    def tags: Rep[Option[String]] = column[Option[String]]("tags")

    def * : MappedProjection[Sample, (Int, String, Int, Option[String])] =
      (id, name, runId, tags) <> (Sample.tupled, Sample.unapply)

    def idx: Index = index("idx_samples", (runId, name), unique = true)
  }
  val samples: TableQuery[Samples] = TableQuery[Samples]

  case class Library(id: Int, name: String, runId: Int, sampleId: Int, tags: Option[String])
  class Libraries(tag: Tag) extends Table[Library](tag, "Libraries") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    def name: Rep[String] = column[String]("name")
    def runId: Rep[Int] = column[Int]("runId")
    def sampleId: Rep[Int] = column[Int]("sampleId")
    def tags: Rep[Option[String]] = column[Option[String]]("tags")

    def * : MappedProjection[Library, (Int, String, Int, Int, Option[String])] =
      (id, name, runId, sampleId, tags) <> (Library.tupled, Library.unapply)

    def idx: Index = index("idx_libraries", (runId, sampleId, name), unique = true)
  }
  val libraries: TableQuery[Libraries] = TableQuery[Libraries]

  case class Pipeline(id: Int, name: String, runId: Int)
  class Pipelines(tag: Tag) extends Table[Pipeline](tag, "PipelineNames") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    def name: Rep[String] = column[String]("name")
    def runId: Rep[Int] = column[Int]("runId")

    def * : MappedProjection[Pipeline, (Int, String, Int)] =
      (id, name, runId) <> (Pipeline.tupled, Pipeline.unapply)

    def idx: Index = index("idx_pipeline_names", (name, runId), unique = true)
  }
  val pipelines: TableQuery[Pipelines] = TableQuery[Pipelines]

  case class Module(id: Int, name: String, runId: Int, pipelineId: Int)
  class Modules(tag: Tag) extends Table[Module](tag, "ModuleNames") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    def name: Rep[String] = column[String]("name")
    def runId: Rep[Int] = column[Int]("runId")
    def pipelineId: Rep[Int] = column[Int]("pipelineId")

    def * : MappedProjection[Module, (Int, String, Int, Int)] =
      (id, name, runId, pipelineId) <> (Module.tupled, Module.unapply)

    def idx: Index = index("idx_module_names", (name, runId, pipelineId), unique = true)
  }
  val modules: TableQuery[Modules] = TableQuery[Modules]

  case class Stat(runId: Int,
                  pipelineId: Int,
                  moduleId: Option[Int],
                  sampleId: Option[Int],
                  library: Option[Int],
                  content: String)
  class Stats(tag: Tag) extends Table[Stat](tag, "Stats") {
    def runId: Rep[Int] = column[Int]("runId")
    def pipelineId: Rep[Int] = column[Int]("pipelineId")
    def moduleId: Rep[Option[Int]] = column[Option[Int]]("moduleId")
    def sampleId: Rep[Option[Int]] = column[Option[Int]]("sampleId")
    def libraryId: Rep[Option[Int]] = column[Option[Int]]("libraryId")
    def content: Rep[String] = column[String]("content")

    def * : MappedProjection[Stat, (Int, Int, Option[Int], Option[Int], Option[Int], String)] =
      (runId, pipelineId, moduleId, sampleId, libraryId, content) <> (Stat.tupled, Stat.unapply)

    def idx: Index =
      index("idx_stats", (runId, pipelineId, moduleId, sampleId, libraryId), unique = true)
  }
  val stats: TableQuery[Stats] = TableQuery[Stats]

  case class Setting(runId: Int,
                     pipelineId: Int,
                     moduleId: Option[Int],
                     sampleId: Option[Int],
                     library: Option[Int],
                     content: String)
  class Settings(tag: Tag) extends Table[Setting](tag, "Settings") {
    def runId: Rep[Int] = column[Int]("runId")
    def pipelineId: Rep[Int] = column[Int]("pipelineId")
    def moduleId: Rep[Option[Int]] = column[Option[Int]]("moduleId")
    def sampleId: Rep[Option[Int]] = column[Option[Int]]("sampleId")
    def libraryId: Rep[Option[Int]] = column[Option[Int]]("libraryId")
    def content: Rep[String] = column[String]("content")

    def * : MappedProjection[Setting, (Int, Int, Option[Int], Option[Int], Option[Int], String)] =
      (runId, pipelineId, moduleId, sampleId, libraryId, content) <> (Setting.tupled, Setting.unapply)

    def idx: Index =
      index("idx_settings", (runId, pipelineId, moduleId, sampleId, libraryId), unique = true)
  }
  val settings: TableQuery[Settings] = TableQuery[Settings]

  case class File(runId: Int,
                  pipelineId: Int,
                  moduleId: Option[Int],
                  sampleId: Option[Int],
                  libraryId: Option[Int],
                  key: String,
                  path: String,
                  md5: String,
                  link: Boolean,
                  size: Long)
  class Files(tag: Tag) extends Table[File](tag, "Files") {
    def runId: Rep[Int] = column[Int]("runId")
    def pipelineId: Rep[Int] = column[Int]("pipelineId")
    def moduleId: Rep[Option[Int]] = column[Option[Int]]("moduleId")
    def sampleId: Rep[Option[Int]] = column[Option[Int]]("sampleId")
    def libraryId: Rep[Option[Int]] = column[Option[Int]]("libraryId")
    def key: Rep[String] = column[String]("key")
    def path: Rep[String] = column[String]("path") // This should be relative to the outputDir
    def md5: Rep[String] = column[String]("md5")
    def link: Rep[Boolean] = column[Boolean]("link", O.Default(false))
    def size: Rep[Long] = column[Long]("size")

    def * : MappedProjection[
      File,
      (Int, Int, Option[Int], Option[Int], Option[Int], String, String, String, Boolean, Long)] =
      (runId, pipelineId, moduleId, sampleId, libraryId, key, path, md5, link, size) <> (File.tupled, File.unapply)

    def idx: Index =
      index("idx_files", (runId, pipelineId, moduleId, sampleId, libraryId, key), unique = true)
  }
  val files: TableQuery[Files] = TableQuery[Files]

  case class Executable(runId: Int,
                        toolName: String,
                        version: Option[String] = None,
                        path: Option[String] = None,
                        javaVersion: Option[String] = None,
                        exeMd5: Option[String] = None,
                        javaMd5: Option[String] = None,
                        jarPath: Option[String] = None)
  class Executables(tag: Tag) extends Table[Executable](tag, "Executables") {
    def runId: Rep[Int] = column[Int]("runId")
    def toolName: Rep[String] = column[String]("toolName")
    def version: Rep[Option[String]] = column[Option[String]]("version")
    def path: Rep[Option[String]] = column[Option[String]]("path")
    def javaVersion: Rep[Option[String]] = column[Option[String]]("javaVersion")
    def exeMd5: Rep[Option[String]] = column[Option[String]]("exeMd5")
    def javaMd5: Rep[Option[String]] = column[Option[String]]("javaMd5")
    def jarPath: Rep[Option[String]] = column[Option[String]]("jarPath")

    def * : MappedProjection[Executable,
                             (Int,
                              String,
                              Option[String],
                              Option[String],
                              Option[String],
                              Option[String],
                              Option[String],
                              Option[String])] =
      (runId, toolName, version, path, javaVersion, exeMd5, javaMd5, jarPath) <> (Executable.tupled, Executable.unapply)

    def idx: Index = index("idx_executables", (runId, toolName), unique = true)
  }
  val executables: TableQuery[Executables] = TableQuery[Executables]

}
