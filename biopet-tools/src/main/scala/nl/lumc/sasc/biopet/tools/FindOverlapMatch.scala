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

import java.io.{File, PrintStream}

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex

/**
  * This tool will find all pairs above a cutoff in a data table
  *
  * Created by pjvan_thof on 21-9-16.
  */
object FindOverlapMatch extends ToolCommand {

  case class Args(inputMetrics: File = null,
                  outputFile: Option[File] = None,
                  cutoff: Double = 0.0,
                  filterSameNames: Boolean = true,
                  rowSampleRegex: Option[Regex] = None,
                  columnSampleRegex: Option[Regex] = None)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('i', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputMetrics = x)
    } text "Input should be a table where the first row and column have the ID's, those can be different"
    opt[File]('o', "output") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "default to stdout"
    opt[Double]('c', "cutoff") required () unbounded () valueName "<value>" action { (x, c) =>
      c.copy(cutoff = x)
    } text "minimum value to report it as pair"
    opt[Unit]("use_same_names") unbounded () valueName "<value>" action { (_, c) =>
      c.copy(filterSameNames = false)
    } text "Do not compare samples with the same name"
    opt[String]("rowSampleRegex") unbounded () valueName "<regex>" action { (x, c) =>
      c.copy(rowSampleRegex = Some(x.r))
    } text "Samples in the row should match this regex"
    opt[String]("columnSampleRegex") unbounded () valueName "<regex>" action { (x, c) =>
      c.copy(columnSampleRegex = Some(x.r))
    } text "Samples in the column should match this regex"
  }

  /**
    * @param args the command line arguments
    */
  def main(args: Array[String]): Unit = {
    val argsParser = new OptParser
    val cmdArgs
      : Args = argsParser.parse(args, Args()) getOrElse (throw new IllegalArgumentException)

    val reader = Source.fromFile(cmdArgs.inputMetrics)

    val data = reader.getLines().map(_.split("\t")).toArray

    val samplesColumnHeader = data.head.zipWithIndex.tail
    val samplesRowHeader = data.map(_.head).zipWithIndex.tail

    var overlap = 0
    var multiOverlap = 0
    var noOverlap = 0

    val writer = cmdArgs.outputFile match {
      case Some(file) => new PrintStream(file)
      case _ => sys.process.stdout
    }

    for (columnSample <- samplesColumnHeader
         if cmdArgs.columnSampleRegex.forall(_.findFirstIn(columnSample._1).isDefined)) {
      val buffer = ListBuffer[(String, Double)]()
      for (rowSample <- samplesRowHeader
           if cmdArgs.rowSampleRegex.forall(_.findFirstIn(rowSample._1).isDefined)) {
        val value = data(columnSample._2)(rowSample._2).toDouble
        if (value >= cmdArgs.cutoff && (!cmdArgs.filterSameNames || columnSample._2 != rowSample._2)) {
          buffer.+=((rowSample._1, value))
        }
      }
      if (buffer.nonEmpty) overlap += 1
      else noOverlap += 1
      if (buffer.size > 1) multiOverlap += 1

      writer.println(s"${columnSample._1}\t${buffer.mkString("\t")}")
    }
    logger.info(s"$overlap found")
    logger.info(s"no $noOverlap found")
    logger.info(s"multi $multiOverlap found")
    writer.close()
  }
}
