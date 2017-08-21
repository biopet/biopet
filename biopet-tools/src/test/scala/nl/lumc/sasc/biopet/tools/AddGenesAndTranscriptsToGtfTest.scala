package nl.lumc.sasc.biopet.tools

import java.io.File

import nl.lumc.sasc.biopet.utils.IoUtils
import nl.lumc.sasc.biopet.utils.annotation.Feature
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source

class AddGenesAndTranscriptsToGtfTest extends TestNGSuite with Matchers {
  @Test
  def testMain(): Unit = {
    val inputFile = File.createTempFile("test.", ".gtf")
    inputFile.deleteOnExit()
    val gene1 = Feature("chrQ", "test", "gene", 11, 40, None, Some(true), None, Map("gene_id" -> "gene_1"))
    val transcript1 = Feature("chrQ", "test", "transcript", 11, 40, None, Some(true), None, Map("gene_id" -> "gene_1", "transcript_id" -> "transcript_1_1"))
    val exon1 = Feature("chrQ", "test", "exon", 11, 20, None, Some(true), None, Map("gene_id" -> "gene_1", "transcript_id" -> "transcript_1_1"))
    val exon2 = Feature("chrQ", "test", "exon", 31, 40, None, Some(true), None, Map("gene_id" -> "gene_1", "transcript_id" -> "transcript_1_1"))
    IoUtils.writeLinesToFile(inputFile, List(exon1.asGtfLine, exon2.asGtfLine))

    val outputFile = File.createTempFile("test.", ".gtf")
    outputFile.deleteOnExit()

    AddGenesAndTranscriptsToGtf.main(Array("-I", inputFile.getAbsolutePath, "-o", outputFile.getAbsolutePath))

    val reader = Source.fromFile(outputFile)
    val lines = reader.getLines().toList
    reader.close()

    val features = lines.map(Feature.fromLine)

    features shouldBe List(
      gene1,
      transcript1,
      exon1,
      exon2
    )
  }
}
