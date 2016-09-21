package nl.lumc.sasc.biopet.tools

import java.io.{File, PrintWriter}

import nl.lumc.sasc.biopet.utils.ToolCommand

import scala.io.Source

/**
  * Created by pjvan_thof on 21-9-16.
  */
object DownloadNcbiAssembly extends ToolCommand {

  case class Args(assemblyId: File = null,
                  outputFile: File = null,
                  contigNameHeader: Option[String] = None,
                  mustHaveOne: Map[String, String] = Map(),
                  mustNotHave: Map[String, String] = Map()) extends AbstractArgs

  class OptParser extends AbstractOptParser {
    opt[File]('a', "assembly id") required () valueName "<file>" action { (x, c) =>
      c.copy(assemblyId = x)
    }
    opt[File]('o', "output") required () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = x)
    }
    opt[String]("nameHeader") valueName "<string>" action { (x, c) =>
      c.copy(contigNameHeader = Some(x))
    }
    opt[(String, String)]("mustHaveOne") valueName "<string>" action { (x, c) =>
      c.copy(mustHaveOne = c.mustHaveOne + (x._1 -> x._2))
    }
    opt[(String, String)]("mustNotHave") valueName "<string>" action { (x, c) =>
      c.copy(mustNotHave = c.mustNotHave + (x._1 -> x._2))
    }
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdargs: Args = argsParser.parse (args, Args () ) getOrElse (throw new IllegalArgumentException)

    logger.info(s"Reading ${cmdargs.assemblyId} from NCBI")
    val reader = Source.fromURL(s"ftp://ftp.ncbi.nlm.nih.gov/genomes/ASSEMBLY_REPORTS/All/${cmdargs.assemblyId}.assembly.txt")
    val assamblyReport = reader.getLines().toList
    reader.close()

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
