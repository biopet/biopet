package nl.lumc.sasc.biopet.utils.summary.db

import java.sql.Blob

import slick.driver.H2Driver.api._

/**
  * Created by pjvan_thof on 27-1-17.
  */
object Schema {

  class Runs(tag: Tag) extends Table[(Int, String)](tag, "Runs") {
    def runId = column[Int]("runId", O.PrimaryKey)
    def runName = column[String]("sampleName")

    def * = (runId, runName)
  }
  val runs = TableQuery[Runs]

  class Samples(tag: Tag) extends Table[(Int, String, Option[Blob])](tag, "Samples") {
    def sampleId = column[Int]("sampleId", O.PrimaryKey)
    def sampleName = column[String]("sampleName")
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
    def libraryId = column[Int]("libraryId", O.PrimaryKey)
    def libraryName = column[String]("libraryName")
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

  class Pipelines(tag: Tag) extends Table[(Int, String)](tag, "Pipelines") {
    def pipelineId = column[Int]("runId", O.PrimaryKey)
    def pipelineName = column[String]("sampleName")

    def * = (pipelineId, pipelineName)
  }
  val pipelines = TableQuery[Pipelines]
}
