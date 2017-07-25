package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.io.Source

/**
  * Created by pjvanthof on 30/05/2017.
  */
object NcbiReportToContigMap extends ToolCommand {

  case class Args(assemblyReport: File = null,
                  outputFile: File = null,
                  reportFile: Option[File] = None,
                  contigNameHeader: String = null,
                  names: List[String] =
                    List("Sequence-Name", "UCSC-style-name", "GenBank-Accn", "RefSeq-Accn"))

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('a', "assembly_report") required () unbounded () valueName "<file>" action {
      (x, c) =>
        c.copy(assemblyReport = x)
    } text "refseq ID from NCBI"
    opt[File]('o', "output") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    } text "output Fasta file"
    opt[String]("nameHeader") required () unbounded () valueName "<string>" action { (x, c) =>
      c.copy(contigNameHeader = x)
    } text
      """
        | What column to use from the NCBI report for the name of the contigs.
        | All columns in the report can be used but this are the most common field to choose from:
        | - 'Sequence-Name': Name of the contig within the assembly
        | - 'UCSC-style-name': Name of the contig used by UCSC ( like hg19 )
        | - 'RefSeq-Accn': Unique name of the contig at RefSeq (default for NCBI)""".stripMargin
    opt[String]("names") unbounded () action { (x, c) =>
      c.copy(names = x.split(",").toList)
    } text "output Fasta file"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info(s"Reading ${cmdargs.assemblyReport}")
    val reader = Source.fromFile(cmdargs.assemblyReport)
    val assamblyReport = reader.getLines().toList
    reader.close()
    cmdargs.reportFile.foreach { file =>
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

    val altNameIds = cmdargs.names.filter(_ != cmdargs.contigNameHeader).map(headers)
    val nameId = headers(cmdargs.contigNameHeader)

    val writer = new PrintWriter(cmdargs.outputFile)
    writer.println("#Name_in_fasta\tAlternative_names")
    for (line <- assamblyReport.filter(!_.startsWith("#"))) {
      val values = line.split("\t")
      val altNames = altNameIds.map(i => values(i)).filter(_ != "na").distinct
      writer.println(values(nameId) + "\t" + altNames.mkString(";"))
    }
    writer.close()
  }
}
