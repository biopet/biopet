package nl.lumc.sasc.biopet.utils.summary.db

import slick.driver.H2Driver.api._

/**
  * Created by pjvan_thof on 26-1-17.
  */
class Samples(tag: Tag) extends Table[(Int, String)](tag, "SUPPLIERS") {
  def sampleId = column[Int]("sampleId", O.PrimaryKey) // This is the primary key column
  def sampleName = column[String]("sampleName")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (sampleId, sampleName)
}