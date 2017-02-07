package nl.lumc.sasc.biopet.utils.summary.db

import slick.driver.H2Driver.api._

/**
  * Created by pjvan_thof on 27-1-17.
  */
object Schema {

  case class Run(id: Int, name: String, outputDir: String)
  class Runs(tag: Tag) extends Table[Run](tag, "Runs") {
    def id = column[Int]("id", O.PrimaryKey)
    def runName = column[String]("runName")
    def outputDir = column[String]("outputDir")

    def * = (id, runName, outputDir) <> (Run.tupled, Run.unapply)
  }
  val runs = TableQuery[Runs]

  case class Sample(id: Int, name: String, runId: Int, tags: Option[String])
  class Samples(tag: Tag) extends Table[Sample](tag, "Samples") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")
    def tags = column[Option[String]]("tags")

    def * = (id, name, runId, tags) <> (Sample.tupled, Sample.unapply)

    def idx = index("idx_samples", (runId, name), unique = true)
  }
  val samples = TableQuery[Samples]

  case class Library(id: Int, name: String, runId: Int, sampleId: Int, tags: Option[String])
  class Libraries(tag: Tag) extends Table[Library](tag, "Libraries") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")
    def sampleId = column[Int]("sampleId")
    def tags = column[Option[String]]("tags")

    def * = (id, name, runId, sampleId, tags) <> (Library.tupled, Library.unapply)

    def idx = index("idx_libraries", (runId, sampleId, name), unique = true)
  }
  val libraries = TableQuery[Libraries]

  case class Pipeline(id: Int, name: String, runId: Int)
  class Pipelines(tag: Tag) extends Table[Pipeline](tag, "PipelineNames") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")

    def * = (id, name, runId) <> (Pipeline.tupled, Pipeline.unapply)

    def idx = index("idx_pipeline_names", (name, runId), unique = true)
  }
  val pipelines = TableQuery[Pipelines]

  case class Module(id: Int, name: String, runId: Int, pipelineId: Int)
  class Modules(tag: Tag) extends Table[Module](tag, "ModuleNames") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")

    def * = (id, name, runId, pipelineId) <> (Module.tupled, Module.unapply)

    def idx = index("idx_module_names", (name, runId, pipelineId), unique = true)
  }
  val modules = TableQuery[Modules]

  case class Stat(runId: Int, pipelineId: Int, moduleId: Option[Int], sampleId: Option[Int], library: Option[Int], content: String)
  class Stats(tag: Tag) extends Table[Stat](tag, "Stats") {
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def content = column[String]("content")

    def * = (runId, pipelineId, moduleId, sampleId, libraryId, content) <> (Stat.tupled, Stat.unapply)

    def idx = index("idx_stats", (runId, pipelineId, moduleId, sampleId, libraryId), unique = true)
  }
  val stats = TableQuery[Stats]

  case class Setting(runId: Int, pipelineId: Int, moduleId: Option[Int], sampleId: Option[Int], library: Option[Int], content: String)
  class Settings(tag: Tag) extends Table[Setting](tag, "Settings") {
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def content = column[String]("content")

    def * = (runId, pipelineId, moduleId, sampleId, libraryId, content) <> (Setting.tupled, Setting.unapply)

    def idx = index("idx_settings", (runId, pipelineId, moduleId, sampleId, libraryId), unique = true)
  }
  val settings = TableQuery[Settings]

  case class File(runId: Int, pipelineId: Int, moduleId: Option[Int], sampleId: Option[Int], library: Option[Int], name: String, path: String, md5: String, link: Boolean, size: Long)
  class Files(tag: Tag) extends Table[File](tag, "Files") {
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def name = column[String]("name")
    def path = column[String]("path")
    def md5 = column[String]("md5")
    def link = column[Boolean]("link", O.Default(false))
    def size = column[Long]("size")

    def * = (runId, pipelineId, moduleId, sampleId, libraryId, name, path, md5, link, size) <> (File.tupled, File.unapply)

    def idx = index("idx_files", (runId, pipelineId, sampleId, libraryId, name), unique = true)
  }
  val files = TableQuery[Files]

  case class Executable(runId: Int, toolName: String, version : Option[String], javaVersion: Option[String], exeMd5: Option[String], javaMd5: Option[String])
  class Executables(tag: Tag) extends Table[Executable](tag, "Executables") {
    def runId = column[Int]("runId")
    def toolName = column[String]("toolName")
    def version = column[Option[String]]("version")
    def javaVersion = column[Option[String]]("javaVersion")
    def exeMd5 = column[Option[String]]("exeMd5")
    def javaMd5 = column[Option[String]]("javaMd5")

    def * = (runId, toolName, version, javaVersion, exeMd5, javaMd5) <> (Executable.tupled, Executable.unapply)

    def idx = index("idx_executables", (runId, toolName), unique = true)
  }
  val executables = TableQuery[Executables]

}
