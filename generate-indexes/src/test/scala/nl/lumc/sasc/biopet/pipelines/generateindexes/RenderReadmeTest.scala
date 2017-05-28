package nl.lumc.sasc.biopet.pipelines.generateindexes

import java.io.File
import java.nio.file.Paths

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

/**
  * Created by pjvanthof on 27/05/2017.
  */
class RenderReadmeTest extends TestNGSuite with Matchers {

  private def resourcePath(p: String): String = {
    Paths.get(getClass.getResource(p).toURI).toString
  }

  @Test
  def test: Unit = {
    val render = new RenderReadme
    render.outputFile = File.createTempFile("Readme.", ".md")
    render.outputFile.deleteOnExit()
    render.fastaFile = new File(resourcePath("/fake_chrQ.fa"))
    render.species = "test"
    render.genomeName = "test"
    render.extraSections = Map("tite" -> "content")
    render.indexes = Map("indexName" -> new File("test"))
    render.run()

    render.outputFile should exist
    val reader = Source.fromFile(render.outputFile)
    val lines = reader.getLines().toList
    reader.close()
  }
}
