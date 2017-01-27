package nl.lumc.sasc.biopet.utils.summary.db

import java.sql.Blob

import slick.driver.H2Driver.api._

/**
  * Created by pjvan_thof on 26-1-17.
  */
class Libraries(tag: Tag) extends Table[(Int, String, Int, Blob)](tag, "Libraries") {
  def libraryId = column[Int]("libraryId", O.PrimaryKey) // This is the primary key column
  def libraryName = column[String]("libraryName")
  def sampleId = column[Int]("sampleId")
  def tags = column[Blob]("tags")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (libraryId, libraryName, sampleId, tags)
}