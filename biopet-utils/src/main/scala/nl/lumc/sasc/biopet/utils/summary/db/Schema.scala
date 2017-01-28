package nl.lumc.sasc.biopet.utils.summary.db

import java.io.File
import java.sql.Blob

import nl.lumc.sasc.biopet.utils.Logging
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by pjvan_thof on 27-1-17.
  */
object Schema {

  class Runs(tag: Tag) extends Table[(Int, String)](tag, "Runs") {
    def runId = column[Int]("runId", O.PrimaryKey)
    def runName = column[String]("runName")

    def * = (runId, runName)
  }
  val runs = TableQuery[Runs]

  class Samples(tag: Tag) extends Table[(Int, String, Option[Blob])](tag, "Samples") {
    def sampleId = column[Int]("id", O.PrimaryKey)
    def sampleName = column[String]("name")
    def tags = column[Option[Blob]]("tags")

    def * = (sampleId, sampleName, tags)
  }
  val samples = TableQuery[Samples]

  class SamplesRuns(tag: Tag) extends Table[(Int, Int)](tag, "SamplesRuns") {
    def sampleId = column[Int]("sampleId")
    def runId = column[Int]("runId")

    def * = (sampleId, runId)

    def idx = index("idx_samples_runs", (sampleId, runId), unique = true)
  }
  val samplesRuns = TableQuery[SamplesRuns]

  class Libraries(tag: Tag) extends Table[(Int, String, Int, Option[Blob])](tag, "Libraries") {
    def libraryId = column[Int]("id", O.PrimaryKey)
    def libraryName = column[String]("name")
    def sampleId = column[Int]("sampleId")
    def tags = column[Option[Blob]]("tags")

    def * = (libraryId, libraryName, sampleId, tags)
  }
  val libraries = TableQuery[Libraries]

  class LibrariesRuns(tag: Tag) extends Table[(Int, Int)](tag, "LibrariesRuns") {
    def libraryId = column[Int]("libraryId")
    def runId = column[Int]("runId")

    def * = (libraryId, runId)

    def idx = index("idx_libraries_runs", (libraryId, runId), unique = true)
  }
  val librariesRuns = TableQuery[LibrariesRuns]

  class PipelineNames(tag: Tag) extends Table[(Int, String)](tag, "PipelineNames") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")

    def * = (id, name)
  }
  val pipelineNames = TableQuery[PipelineNames]

  class ModuleNames(tag: Tag) extends Table[(Int, String)](tag, "ModuleNames") {
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")

    def * = (id, name)
  }
  val moduleNames = TableQuery[ModuleNames]


  class Stats(tag: Tag) extends Table[(Int, Int, Option[Int], Option[Int], Option[Int], Blob, Option[String])](tag, "Stats") {
    def pipelineId = column[Int]("pipelineId")
    def runId = column[Int]("runId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def stats = column[Blob]("stats")
    def schema = column[Option[String]]("schema")

    def * = (pipelineId, runId, moduleId, sampleId, libraryId, stats, schema)

    def idx = index("idx_stats", (pipelineId, runId, moduleId, sampleId, libraryId), unique = true)
  }
  val stats = TableQuery[Stats]

  class Settings(tag: Tag) extends Table[(Int, Int, Option[Int], Option[Int], Option[Int], Blob, Option[String])](tag, "Settings") {
    def pipelineId = column[Int]("pipelineId")
    def runId = column[Int]("runId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def stats = column[Blob]("stats")
    def schema = column[Option[String]]("schema")

    def * = (pipelineId, runId, moduleId, sampleId, libraryId, stats, schema)

    def idx = index("idx_settings", (pipelineId, runId, moduleId, sampleId, libraryId), unique = true)
  }
  val settings = TableQuery[Settings]

  class Files(tag: Tag) extends Table[(Int, Int, Option[Int], Option[Int], Option[Int], String, String, Boolean, Long)](tag, "Files") {
    def pipelineId = column[Int]("pipelineId")
    def runId = column[Int]("runId")
    def moduleId = column[Option[Int]]("moduleId")
    def sampleId = column[Option[Int]]("sampleId")
    def libraryId = column[Option[Int]]("libraryId")
    def path = column[String]("path")
    def md5 = column[String]("md5")
    def link = column[Boolean]("link", O.Default(false))
    def size = column[Long]("size")

    def * = (pipelineId, runId, moduleId, sampleId, libraryId, path, md5, link, size)

    def idx = index("idx_files", (pipelineId, runId, sampleId, libraryId, path), unique = true)
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

  def createEmptySqlite(file: File): Unit = {
    val db = Database.forURL(s"jdbc:sqlite:${file.getAbsolutePath}", driver = "org.sqlite.JDBC")

    try {
      val setup = DBIO.seq(
        (runs.schema ++ samples.schema ++
          samplesRuns.schema ++ libraries.schema ++
          librariesRuns.schema ++ pipelineNames.schema ++
          moduleNames.schema ++ stats.schema ++ settings.schema ++
          files.schema ++ executables.schema).create
      )
      val setupFuture = db.run(setup)
      Await.result(setupFuture, Duration.Inf)
    } finally db.close
  }

}
