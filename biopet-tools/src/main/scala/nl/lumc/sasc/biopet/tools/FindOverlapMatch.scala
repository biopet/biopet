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
                  shouldMatchRegexFile: Option[File] = None,
                  showBestMatch: Boolean = false,
                  filterSameNames: Boolean = true,
                  rowSampleRegex: Option[Regex] = None,
                  columnSampleRegex: Option[Regex] = None)

  class OptParser extends AbstractOptParser[Args](commandName) {
    opt[File]('i', "input") required () unbounded () valueName "<file>" action { (x, c) =>
      c.copy(inputMetrics = x)
    } text "Input should be a table where the first row and column have the ID's, those can be different"
    opt[File]("shouldMatchRegexFile") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(shouldMatchRegexFile = Some(x))
    } text "File with regexes what should be the correct matches.\n" +
      "first column is the row samples regex, second column the column regex.\n" +
      "When no second column given first column is used."
    opt[File]('o', "output") unbounded () valueName "<file>" action { (x, c) =>
      c.copy(outputFile = Some(x))
    } text "default to stdout"
    opt[Double]('c', "cutoff") required () unbounded () valueName "<value>" action { (x, c) =>
      c.copy(cutoff = x)
    } text "minimum value to report it as pair"
    opt[Unit]("use_same_names") unbounded () valueName "<value>" action { (_, c) =>
      c.copy(filterSameNames = false)
    } text "Do not compare samples with the same name"
    opt[Unit]("showBestMatch") unbounded () valueName "<value>" action { (_, c) =>
      c.copy(showBestMatch = true)
    } text "Show best match, even when it's below cutoff"
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

    logger.info("Start")

    val reader = Source.fromFile(cmdArgs.inputMetrics)

    val data = reader.getLines().map(_.split("\t")).toArray

    val samplesColumnHeader = data.head.zipWithIndex.tail
    val samplesRowHeader = data.map(_.head).zipWithIndex.tail

    var overlap = 0
    var multiOverlap = 0
    var noOverlap = 0
    var correctMatches = 0
    var incorrectMatches = 0

    val writer = cmdArgs.outputFile match {
      case Some(file) => new PrintStream(file)
      case _ => sys.process.stdout
    }

    val matchesRegexes = cmdArgs.shouldMatchRegexFile.map { file =>
      val reader = Source.fromFile(file)
      val regexes = reader
        .getLines()
        .map { line =>
          val values = line.split("\t").map(_.r)
          values.head -> values.lift(1).getOrElse(values.head)
        }
        .toList
      reader.close()
      regexes
    }

    for ((columnSampleName, columnSampleId) <- samplesColumnHeader
         if cmdArgs.columnSampleRegex.forall(_.findFirstIn(columnSampleName).isDefined)) {

      val buffer = ListBuffer[(String, Double)]()
      val usedRows = samplesRowHeader.filter {
        case (name, id) =>
          cmdArgs.rowSampleRegex.forall(_.findFirstIn(name).isDefined)
      }
      for (rowSample <- usedRows) {
        val value = data(columnSampleId)(rowSample._2).toDouble
        if (value >= cmdArgs.cutoff && (!cmdArgs.filterSameNames || columnSampleId != rowSample._2)) {
          buffer.+=((rowSample._1, value))
        }
      }

      if (buffer.nonEmpty) overlap += 1
      else noOverlap += 1
      if (buffer.size > 1) multiOverlap += 1

      if (buffer.isEmpty && cmdArgs.showBestMatch) {
        val max = usedRows.map(x => data(columnSampleId)(x._2).toDouble).max
        samplesRowHeader
          .filter(x => data(columnSampleId)(x._2).toDouble == max)
          .foreach {
            case (name, _) =>
              buffer.+=((name, max))
          }
      }

      matchesRegexes.foreach { regexes =>
        regexes.find(_._1.findFirstMatchIn(columnSampleName).isDefined).foreach {
          case (_, regex2) =>
            val max = buffer.map(_._2).max
            if (buffer.filter(_._2 == max).exists(x => regex2.findFirstMatchIn(x._1).isDefined)) {
              correctMatches += 1
            } else {
              logger.warn(s"Incorrect match found, sample: $columnSampleName")
              incorrectMatches += 1
              usedRows
                .filter(x => regex2.findFirstIn(x._1).isDefined)
                .foreach(x => buffer.+=((x._1, data(columnSampleId)(x._2).toDouble)))
            }
        }
      }

      writer.println(s"$columnSampleName\t${buffer.mkString("\t")}")
    }
    cmdArgs.outputFile.foreach(_ => writer.close())
    if (matchesRegexes.isDefined) {
      logger.info(s"$correctMatches correct matches found")
      logger.info(s"$incorrectMatches incorrect matches found")
    }
    logger.info(s"$overlap found")
    logger.info(s"no $noOverlap found")
    logger.info(s"multi $multiOverlap found")
    logger.info("Done")
  }
}
