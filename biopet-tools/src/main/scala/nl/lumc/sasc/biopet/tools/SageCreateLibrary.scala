/**
  * Biopet is built on top of GATK Queue for building bioinformatic
  * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
  * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
  * should also be able to execute Biopet tools and pipelines.
  *
  * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
  *
  * Contact us at: sasc@lumc.nl
  *
  * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
  * license; For commercial users or users who do not want to follow the AGPL
  * license, please contact us to obtain a separate license.
  */
package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand
import org.biojava3.core.sequence.DNASequence
import org.biojava3.core.sequence.io.FastaReaderHelper

import scala.collection.{SortedMap, mutable}
import scala.util.matching.Regex
import scala.collection.JavaConversions._

object SageCreateLibrary extends ToolCommand {
  case class Args(input: File = null,
                  tag: String = "CATG",
                  length: Int = 17,
                  output: File = null,
                  noTagsOutput: File = null,
                  noAntiTagsOutput: File = null,
                  allGenesOutput: File = null)
      extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('I', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(input = x)
    }
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(output = x)
    }
    opt[String]("tag") required () unbounded () action { (x, c) =>
      c.copy(tag = x)
    }
    opt[Int]("length") required () unbounded () action { (x, c) =>
      c.copy(length = x)
    }
    opt[File]("noTagsOutput") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(noTagsOutput = x)
    }
    opt[File]("noAntiTagsOutput") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(noAntiTagsOutput = x)
    }
    opt[File]("allGenesOutput") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(allGenesOutput = x)
    }
  }

  val geneRegex: Regex = """ENSG[0-9]{11}""".r

  val tagGenesMap: mutable.Map[String, TagGenes] = mutable.Map()

  val allGenes: mutable.Set[String] = mutable.Set()
  val tagGenes: mutable.Set[String] = mutable.Set()
  val antiTagGenes: mutable.Set[String] = mutable.Set()

  class TagGenes {
    val firstTag: mutable.Set[String] = mutable.Set()
    val allTags: mutable.Set[String] = mutable.Set()
    val firstAntiTag: mutable.Set[String] = mutable.Set()
    val allAntiTags: mutable.Set[String] = mutable.Set()
  }
  class TagResult(val firstTag: String,
                  val allTags: List[String],
                  val firstAntiTag: String,
                  val allAntiTags: List[String])

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val commandArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    if (!commandArgs.input.exists)
      throw new IllegalStateException("Input file not found, file: " + commandArgs.input)

    val tagRegex = (commandArgs.tag + "[CATG]{" + commandArgs.length + "}").r

    var count = 0
    logger.info("Reading fasta file")
    val reader = FastaReaderHelper.readFastaDNASequence(commandArgs.input)
    logger.info("Finding tags")
    for ((name, seq) <- reader) {
      val result = getTags(name, seq, tagRegex)
      addTagresultToTaglib(name, result)
      count += 1
      if (count % 10000 == 0) logger.info(count + " transcripts done")
    }
    logger.info(count + " transcripts done")

    logger.info("Start sorting tags")
    val tagGenesMapSorted: SortedMap[String, TagGenes] = SortedMap(tagGenesMap.toArray: _*)

    logger.info("Writting output files")
    val writer = new PrintWriter(commandArgs.output)
    writer.println("#tag\tfirstTag\tAllTags\tFirstAntiTag\tAllAntiTags")
    for ((tag, genes) <- tagGenesMapSorted) {
      val line = tag + "\t" + genes.firstTag.mkString(",") +
        "\t" + genes.allTags.mkString(",") +
        "\t" + genes.firstAntiTag.mkString(",") +
        "\t" + genes.allAntiTags.mkString(",")
      writer.println(line)
    }
    writer.close()

    if (commandArgs.noTagsOutput != null) {
      val writer = new PrintWriter(commandArgs.noTagsOutput)
      for (gene <- allGenes if !tagGenes.contains(gene)) {
        writer.println(gene)
      }
      writer.close()
    }

    if (commandArgs.noAntiTagsOutput != null) {
      val writer = new PrintWriter(commandArgs.noAntiTagsOutput)
      for (gene <- allGenes if !antiTagGenes.contains(gene)) {
        writer.println(gene)
      }
      writer.close()
    }

    if (commandArgs.allGenesOutput != null) {
      val writer = new PrintWriter(commandArgs.allGenesOutput)
      for (gene <- allGenes) {
        writer.println(gene)
      }
      writer.close()
    }
  }

  private def addTagresultToTaglib(name: String, tagResult: TagResult) {
    //.stripPrefix("hg19_ensGene_")
    val geneID = geneRegex.findFirstIn(name).getOrElse("unknown_gene")
    allGenes.add(geneID)

    if (tagResult.firstTag != null) {
      if (!tagGenesMap.contains(tagResult.firstTag))
        tagGenesMap += (tagResult.firstTag -> new TagGenes)
      tagGenesMap(tagResult.firstTag).firstTag.add(geneID)
      tagGenes.add(geneID)
    }

    for (tag <- tagResult.allTags) {
      if (!tagGenesMap.contains(tag)) tagGenesMap += (tag -> new TagGenes)
      tagGenesMap(tag).allTags.add(geneID)
    }

    if (tagResult.firstAntiTag != null) {
      if (!tagGenesMap.contains(tagResult.firstAntiTag))
        tagGenesMap += (tagResult.firstAntiTag -> new TagGenes)
      tagGenesMap(tagResult.firstAntiTag).firstAntiTag.add(geneID)
      antiTagGenes.add(geneID)
    }

    for (tag <- tagResult.allAntiTags) {
      if (!tagGenesMap.contains(tag)) tagGenesMap += (tag -> new TagGenes)
      tagGenesMap(tag).allAntiTags.add(geneID)
    }
  }

  def getTags(name: String, seq: DNASequence, tagRegex: Regex): TagResult = {
    val allTags: List[String] =
      for (tag <- tagRegex.findAllMatchIn(seq.getSequenceAsString).toList) yield tag.toString()
    val firstTag = if (allTags.isEmpty) null else allTags.last
    val allAntiTags: List[String] =
      for (tag <- tagRegex.findAllMatchIn(seq.getReverseComplement.getSequenceAsString).toList)
        yield tag.toString()
    val firstAntiTag = if (allAntiTags.isEmpty) null else allAntiTags.head
    val result = new TagResult(firstTag, allTags, firstAntiTag, allAntiTags)

    result
  }
}
