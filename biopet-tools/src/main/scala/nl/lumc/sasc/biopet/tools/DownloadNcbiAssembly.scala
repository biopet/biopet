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

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.io.Source

/**
  * Created by pjvan_thof on 21-9-16.
  */
object DownloadNcbiAssembly extends ToolCommand {

  case class Args(assemblyReport: File = null,
                  outputFile: File = null,
                  reportFile: Option[File] = None,
                  contigNameHeader: Option[String] = None,
                  mustHaveOne: List[(String, String)] = List(),
                  mustNotHave: List[(String, String)] = List())

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('a', "assembly_report") required () unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(assemblyReport = x)
    } text "refseq ID from NCBI"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "output Fasta file"
    opt[File]("report") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(reportFile = Some(x))
    } text "where to write report from ncbi"
    opt[String]("nameHeader") unbounded () valueName "<string>" action { (x, c) =>
      c.copy(contigNameHeader = Some(x))
    } text
      """
        | What column to use from the NCBI report for the name of the contigs.
        | All columns in the report can be used but this are the most common field to choose from:
        | - 'Sequence-Name': Name of the contig within the assembly
        | - 'UCSC-style-name': Name of the contig used by UCSC ( like hg19 )
        | - 'RefSeq-Accn': Unique name of the contig at RefSeq (default for NCBI)""".stripMargin
    opt[(String, String)]("mustHaveOne") unbounded () valueName "<column_name=regex>" action {
      (x, c) =>
        c.copy(mustHaveOne = (x._1, x._2) :: c.mustHaveOne)
    } text "This can be used to filter based on the NCBI report, multiple conditions can be given, at least 1 should be true"
    opt[(String, String)]("mustNotHave") unbounded () valueName "<column_name=regex>" action {
      (x, c) =>
        c.copy(mustNotHave = (x._1, x._2) :: c.mustNotHave)
    } text "This can be used to filter based on the NCBI report, multiple conditions can be given, all should be false"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info(s"Reading ${cmdArgs.assemblyReport}")
    val reader = Source.fromFile(cmdArgs.assemblyReport)
    val assamblyReport = reader.getLines().toList
    reader.close()
    cmdArgs.reportFile.foreach { file =>
      val writer = new PrintWriter(file)
      assamblyReport.foreach(writer.println)
      writer.close()
    }

    val headers = assamblyReport
      .filter(_.startsWith("#"))
      .last
      .stripPrefix("# ")
      .split("\t")
      .zipWithIndex
      .toMap
    val nameId = cmdArgs.contigNameHeader.map(x => headers(x))
    val lengthId = headers.get("Sequence-Length")

    val baseUrlEutils = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"

    val fastaWriter = new PrintWriter(cmdArgs.outputFile)

    val allContigs = assamblyReport
      .filter(!_.startsWith("#"))
      .map(_.split("\t"))
    val totalLength = lengthId.map(id => allContigs.map(_.apply(id).toLong).sum)

    logger.info(s"${allContigs.size} contigs found")
    totalLength.foreach(l => logger.info(s"Total length: $l"))

    val filterContigs = allContigs
      .filter(values => cmdArgs.mustNotHave.forall(x => values(headers(x._1)) != x._2))
      .filter(values =>
        cmdArgs.mustHaveOne
          .exists(x => values(headers(x._1)) == x._2) || cmdArgs.mustHaveOne.isEmpty)
    val filterLength = lengthId.map(id => filterContigs.map(_.apply(id).toLong).sum)

    logger.info(s"${filterContigs.size} contigs left after filtering")
    filterLength.foreach(l => logger.info(s"Filtered length: $l"))

    filterContigs.foreach { values =>
      val id = if (values(6) == "na") values(4) else values(6)
      logger.info(s"Start download $id")
      val fastaReader =
        Source.fromURL(s"$baseUrlEutils/efetch.fcgi?db=nuccore&id=$id&retmode=text&rettype=fasta")
      fastaReader
        .getLines()
        .map(x => nameId.map(y => x.replace(">", s">${values(y)} ")).getOrElse(x))
        .foreach(fastaWriter.println)
      fastaReader.close()
    }

    logger.info("Downloading complete")

    fastaWriter.close()

  }
}
