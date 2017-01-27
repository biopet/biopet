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

  class Samples(tag: Tag) extends Table[(Int, String, Int, Blob)](tag, "Samples") {
    def sampleId = column[Int]("sampleId", O.PrimaryKey)
    def sampleName = column[String]("sampleName")
    def runId = column[Int]("runId")
    def tags = column[Blob]("tags")

    def * = (sampleId, sampleName, runId, tags)
  }
  val samples = TableQuery[Samples]


  class Libraries(tag: Tag) extends Table[(Int, String, Int, Blob)](tag, "Libraries") {
    def libraryId = column[Int]("libraryId", O.PrimaryKey)
    def libraryName = column[String]("libraryName")
    def sampleId = column[Int]("sampleId")
    def tags = column[Blob]("tags")

    def * = (libraryId, libraryName, sampleId, tags)
  }
  val libraries = TableQuery[Libraries]

  class Pipelines(tag: Tag) extends Table[(Int, String)](tag, "Pipelines") {
    def pipelineId = column[Int]("runId", O.PrimaryKey)
    def pipelineName = column[String]("sampleName")

    def * = (pipelineId, pipelineName)
  }
  val pipelines = TableQuery[Pipelines]

}
