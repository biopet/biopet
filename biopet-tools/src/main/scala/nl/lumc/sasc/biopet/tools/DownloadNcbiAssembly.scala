package nl.lumc.sasc.biopet.tools

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.io.Source

/**
 * Created by pjvan_thof on 21-9-16.
 */
object DownloadNcbiAssembly extends ToolCommand {

  case class Args(assemblyId: String = null,
                  outputFile: File = null,
                  reportFile: Option[File] = None,
                  contigNameHeader: Option[String] = None,
                  mustHaveOne: List[(String, String)] = List(),
                  mustNotHave: List[(String, String)] = List()) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[String]('a', "assembly id") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(assemblyId = x)
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
    opt[(String, String)]("mustHaveOne") unbounded () valueName "<column_name=regex>" action { (x, c) =>
      c.copy(mustHaveOne = (x._1, x._2) :: c.mustHaveOne)
    } text "This can be used to filter based on the NCBI report, multiple conditions can be given, at least 1 should be true"
    opt[(String, String)]("mustNotHave") unbounded () valueName "<column_name=regex>" action { (x, c) =>
      c.copy(mustNotHave = (x._1, x._2) :: c.mustNotHave)
    } text "This can be used to filter based on the NCBI report, multiple conditions can be given, all should be false"
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs: Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    logger.info(s"Reading ${cmdargs.assemblyId} from NCBI")
    val reader = Source.fromURL(s"ftp://ftp.ncbi.nlm.nih.gov/genomes/ASSEMBLY_REPORTS/All/${cmdargs.assemblyId}.assembly.txt")
    val assamblyReport = reader.getLines().toList
    reader.close()
    cmdargs.reportFile.foreach { file =>
      val writer = new PrintWriter(file)
      assamblyReport.foreach(writer.println)
      writer.close()
    }

    val headers = assamblyReport.filter(_.startsWith("#")).last.stripPrefix("# ").split("\t").zipWithIndex.toMap
    val nameId = cmdargs.contigNameHeader.map(x => headers(x))
    val lengthId = headers.get("Sequence-Length")

    val baseUrlEutils = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"

    val fastaWriter = new PrintWriter(cmdargs.outputFile)

    val allContigs = assamblyReport.filter(!_.startsWith("#"))
      .map(_.split("\t"))
    val totalLength = lengthId.map(id => allContigs.map(_.apply(id).toLong).sum)

    logger.info(s"${allContigs.size} contigs found")
    totalLength.foreach(l => logger.info(s"Total length: ${l}"))

    val filterContigs = allContigs
      .filter(values => cmdargs.mustNotHave.forall(x => values(headers(x._1)) != x._2))
      .filter(values => cmdargs.mustHaveOne.exists(x => values(headers(x._1)) == x._2) || cmdargs.mustHaveOne.isEmpty)
    val filterLength = lengthId.map(id => filterContigs.map(_.apply(id).toLong).sum)

    logger.info(s"${filterContigs.size} contigs left after filtering")
    filterLength.foreach(l => logger.info(s"Filtered length: ${l}"))

    filterContigs.foreach { values =>
      val id = if (values(6) == "na") values(4) else values(6)
      logger.info(s"Start download ${id}")
      val fastaReader = Source.fromURL(s"${baseUrlEutils}/efetch.fcgi?db=nuccore&id=${id}&retmode=text&rettype=fasta")
      fastaReader.getLines()
        .map(x => nameId.map(y => x.replace(">", s">${values(y)} ")).getOrElse(x))
        .foreach(fastaWriter.println)
      fastaReader.close()
    }

    logger.info("Downloading complete")

    fastaWriter.close()

  }
}
