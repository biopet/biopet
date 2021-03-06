package nl.lumc.sasc.biopet.core.jobreport

import java.io.{File, PrintStream}

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}
import org.broadinstitute.gatk.utils.report.GATKReport

import scala.collection.JavaConversions._

/**
  * Created by pjvanthof on 25/07/2017.
  */
object MergeQueueJobReports extends ToolCommand {
  case class Args(inputFiles: List[File] = Nil, outputfile: File = null)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('I', "inputFile") unbounded () required () valueName "<file>" action { (x, c) =>
      c.copy(inputFiles = x :: c.inputFiles)
    } text "Output directory of the pipeline"
    opt[File]('o', "outputFile") unbounded () required () maxOccurs 1 valueName "<file>" action {
      (x, c) =>
        c.copy(outputfile = x)
    } text "Output directory of this tool"
  }

  def main(args: Array[String]): Unit = {
    logger.info("Start")

    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val newReport = new GATKReport
    val reports = cmdArgs.inputFiles.map(new GATKReport(_))
    val tableNames = reports.flatMap(_.getTables.map(_.getTableName)).distinct

    for (name <- tableNames; report <- reports if report.hasTable(name)) {
      val table = report.getTable(name)
      if (newReport.hasTable(name)) newReport.getTable(name).concat(table)
      else newReport.addTable(table)
    }

    val writer = new PrintStream(cmdArgs.outputfile)
    newReport.print(writer)
    writer.close()

    logger.info("Done")
  }

}
