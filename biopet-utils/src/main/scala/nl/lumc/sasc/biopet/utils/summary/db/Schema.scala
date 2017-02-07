package nl.lumc.sasc.biopet.utils.summary.db

import slick.driver.H2Driver.api._

/**
  * Created by pjvan_thof on 27-1-17.
  */
object Schema {

  class Runs(tag: Tag) extends Table[(Int, String, String)](tag, "Runs") {
    def id = column[Int]("id", O.PrimaryKey)
    def runName = column[String]("runName")
    def outputDir = column[String]("outputDir")

    def * = (id, runName, outputDir)
  }
  val runs = TableQuery[Runs]

  class Samples(tag: Tag) extends Table[(Int, String, Int, Option[String])](tag, "Samples") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")
    def tags = column[Option[String]]("tags")

    def * = (id, name, runId, tags)

    def idx = index("idx_samples", (runId, name), unique = true)
  }
  val samples = TableQuery[Samples]

  class Libraries(tag: Tag) extends Table[(Int, String, Int, Int, Option[String])](tag, "Libraries") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")
    def sampleId = column[Int]("sampleId")
    def tags = column[Option[String]]("tags")

    def * = (id, name, runId, sampleId, tags)

    def idx = index("idx_libraries", (runId, sampleId, name), unique = true)
  }
  val libraries = TableQuery[Libraries]

  class PipelineNames(tag: Tag) extends Table[(Int, String, Int)](tag, "PipelineNames") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")

    def * = (id, name, runId)

    def idx = index("idx_pipeline_names", (name, runId), unique = true)
  }
  val pipelineNames = TableQuery[PipelineNames]

  class ModuleNames(tag: Tag) extends Table[(Int, String, Int, Int)](tag, "ModuleNames") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")

    def * = (id, name, runId, pipelineId)

    def idx = index("idx_module_names", (name, runId, pipelineId), unique = true)
  }
  val moduleNames = TableQuery[ModuleNames]


  class Stats(tag: Tag) extends Table[(Int, Int, Option[Int], Option[Int], Option[Int], String, Option[String])](tag, "Stats") {
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def stats = column[String]("stats")
    def schema = column[Option[String]]("schema")

    def * = (runId, pipelineId, moduleId, sampleId, libraryId, stats, schema)

    def idx = index("idx_stats", (runId, pipelineId, moduleId, sampleId, libraryId), unique = true)
  }
  val stats = TableQuery[Stats]

  class Settings(tag: Tag) extends Table[(Int, Int, Option[Int], Option[Int], Option[Int], String, Option[String])](tag, "Settings") {
    def runId = column[Int]("runId")
    def pipelineId = column[Int]("pipelineId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def stats = column[String]("stats")
    def schema = column[Option[String]]("schema")

    def * = (runId, pipelineId, moduleId, sampleId, libraryId, stats, schema)

    def idx = index("idx_settings", (runId, pipelineId, moduleId, sampleId, libraryId), unique = true)
  }
  val settings = TableQuery[Settings]

  class Files(tag: Tag) extends Table[(Int, Int, Option[Int], Option[Int], Option[Int], String, String, String, Boolean, Long)](tag, "Files") {
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

    def * = (runId, pipelineId, moduleId, sampleId, libraryId, name, path, md5, link, size)

    def idx = index("idx_files", (runId, pipelineId, sampleId, libraryId, name), unique = true)
  }
  val files = TableQuery[Files]

  class Executables(tag: Tag) extends Table[(Int, String, Option[String], Option[String], Option[String], Option[String])](tag, "Executables") {
    def runId = column[Int]("runId")
    def toolName = column[String]("toolName")
    def version = column[Option[String]]("version")
    def javaVersion = column[Option[String]]("javaVersion")
    def exeMd5 = column[Option[String]]("exeMd5")
    def javaMd5 = column[Option[String]]("javaMd5")

    def * = (runId, toolName, version, javaVersion, exeMd5, javaMd5)

    def idx = index("idx_executables", (runId, toolName), unique = true)
  }
  val executables = TableQuery[Executables]

}
