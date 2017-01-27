package nl.lumc.sasc.biopet.utils.summary.db

import slick.driver.H2Driver.api._

/**
  * Created by pjvanthof on 27/01/2017.
  */
class Runs(tag: Tag) extends Table[(Int, String)](tag, "Runs") {
  def runId = column[Int]("runId", O.PrimaryKey) // This is the primary key column
  def runName = column[String]("sampleName")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (runId, runName)
}