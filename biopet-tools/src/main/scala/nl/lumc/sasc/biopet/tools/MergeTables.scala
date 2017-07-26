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

import java.io.{BufferedWriter, File, FileWriter, OutputStreamWriter}

import nl.lumc.sasc.biopet.utils.{AbstractOptParser, ToolCommand}

import scala.collection.mutable.{Set => MutSet}
import scala.io.{BufferedSource, Source}

object MergeTables extends ToolCommand {

  /** Type alias for sample name */
  type Sample = String

  /** Type alias for feature name */
  type Feature = String

  /** Type alias for value string */
  type Value = String

  /** Case class for storing input data */
  case class InputTable(name: String, source: BufferedSource)

  /** Processes the current line into a pair of feature identifier and its value */
  def processLine(line: String,
                  idIdces: Seq[Int],
                  valIdx: Int,
                  delimiter: Char = '\t',
                  idSeparator: String = ","): (Feature, Value) = {

    // split on delimiter and remove empty strings
    val split = line
      .split(delimiter)
      .filter(_.nonEmpty)
    val colSize = split.length
    require(idIdces.forall(_ < colSize),
            "All feature ID indices must be smaller than number of columns")
    require(valIdx < colSize, "Value index must be smaller than number of columns")

    val featureId = idIdces.map { split }.mkString(idSeparator)
    (featureId, split(valIdx))
  }

  /** Merges multiple tables into a single map */
  def mergeTables(inputs: Seq[InputTable],
                  idIdces: Seq[Int],
                  valIdx: Int,
                  numHeaderLines: Int,
                  delimiter: Char = '\t'): Map[Sample, Map[Feature, Value]] = {

    require(numHeaderLines >= 0, "Number of input header lines less than zero")

    inputs
    // make a map of the base name and the file object
    .map {
      case InputTable(name, source) =>
        val featureValues: Seq[(Feature, Value)] = source
          .getLines()
          // drop header lines according to input
          .drop(numHeaderLines)
          // process the line into a feature, value pair
          .map(processLine(_, idIdces, valIdx))
          // turn into seq
          .toSeq
        // raise error if there are duplicate values (otherwise they will be silently ignored and we may lose values)
        require(featureValues.map(_._1).distinct.size == featureValues.size,
                s"Duplicate features exist in $name")
        name -> featureValues.toMap
    }.toMap
  }

  /** Writes results to stdout */
  def writeOutput(results: Map[Sample, Map[Feature, Value]],
                  output: BufferedWriter,
                  fallback: String,
                  featureName: String): Unit = {
    // sort samples alphabetically
    val samples: Seq[Sample] = results.keys.toSeq.sorted
    // create union of all feature IDs
    val features: Seq[Feature] = results
    // retrieve feature names from each sample
      .map { case (_, featureMap) => featureMap.keySet }
      // fold all of them into a single container
      .foldLeft(MutSet.empty[Feature]) { case (acc, x) => acc ++= x }
      // and create a sorted sequence
      .toSeq
      .sorted

    output.write((featureName +: samples).mkString("\t") + "\n")
    features.foreach { feature =>
      // get feature values for each sample (order == order of samples in header)
      val line = feature +: samples
        .map(results(_).getOrElse(feature, fallback))
      output.write(line.mkString("\t") + "\n")
    }
    output.flush()
  }

  /** Default arguments */
  case class Args(inputTables: Seq[File] = Seq.empty[File],
                  idColumnName: String = "feature",
                  idColumnIndices: Seq[Int] = Seq.empty[Int],
                  valueColumnIndex: Int = -1,
                  fileExtension: String = "",
                  columnNames: Option[Seq[String]] = None,
                  numHeaderLines: Int = 0,
                  fallbackString: String = "-",
                  delimiter: Char = '\t',
                  out: File = new File("-"))

  /** Command line argument parser */
  class OptParser extends AbstractOptParser[Args](commandName) {

    import scopt.Read

    // implicit conversion for argument parsing
    implicit val charRead: Read[Char] = Read.reads { _.toCharArray.head }

    head(s"""
         |$commandName - Tabular file merging based on feature ID equality.
      """.stripMargin)

    opt[Seq[Int]]('i', "id_column_index") required () valueName "<idx1>,<idx2>, ..." action {
      (x, c) =>
        c.copy(idColumnIndices = x.map(_ - 1)) // -1 to convert to Scala-style 0-based indexing
    } validate { x =>
      if (x.forall(_ > 0)) success else failure("Index must be at least 1")
    } text "Index of feature ID column from each input file (1-based)"

    opt[Int]('a', "value_column_index") required () valueName "<idx>" action { (x, c) =>
      c.copy(valueColumnIndex = x - 1) // -1 to convert to Scala-style 0-based indexing
    } validate { x =>
      if (x > 0) success else failure("Index must be at least 1")
    } text "Index of column from each input file containing the value to merge (1-based)"

    opt[File]('o', "output") optional () valueName "<path>" action { (x, c) =>
      c.copy(out = x)
    } text "Path to output file (default: '-' <stdout>)"

    opt[String]('n', "id_column_name") optional () valueName "<name>" action { (x, c) =>
      c.copy(idColumnName = x)
    } text "Name of feature ID column in the output merged file (default: feature)"

    opt[String]('N', "column_names") optional () valueName "<name>" action { (x, c) =>
      c.copy(columnNames = Some(x.split(",")))
    } text "Name of feature ID column in the output merged file (default: feature)"

    opt[String]('e', "strip_extension") optional () valueName "<ext>" action { (x, c) =>
      c.copy(fileExtension = x)
    } text "Common extension of all input tables to strip (default: empty string)"

    opt[Int]('m', "num_header_lines") optional () action { (x, c) =>
      c.copy(numHeaderLines = x)
    } text "The number of header lines present in all input files (default: 0; no header)"

    opt[String]('f', "fallback") optional () action { (x, c) =>
      c.copy(fallbackString = x)
    } text "The string to use when a value for a feature is missing in one or more sample(s) (default: '-')"

    opt[Char]('d', "delimiter") optional () action { (x, c) =>
      c.copy(delimiter = x)
    } text "The character used for separating columns in the input files (default: '\\t')"

    arg[File]("<input_tables> ...") unbounded () optional () action { (x, c) =>
      c.copy(inputTables = c.inputTables :+ x)
    } validate { x =>
      if (x.exists) success else failure(s"File '$x' does not exist")
    } text "Input tables to merge"

    note("""
        |This tool merges multiple tab-delimited files and outputs a single
        |tab delimited file whose columns are the feature IDs and a single
        |column from each input files.
        |
        |Note that in each input file there must not be any duplicate features.
        |If there are, the tool will only keep one and discard the rest.
      """.stripMargin)

  }

  /** Parses the command line argument */
  def parseArgs(args: Array[String]): Args =
    new OptParser()
      .parse(args, Args())
      .getOrElse(throw new IllegalArgumentException)

  /** Transforms the input file seq into a seq of [[InputTable]] objects */
  def prepInput(inFiles: Seq[File],
                ext: String,
                columnNames: Option[Seq[String]]): Seq[InputTable] = {
    (ext, columnNames) match {
      case (_, Some(names)) =>
        require(names.size == inFiles.size, "columnNames are not the same number as input Files")
        names.zip(inFiles).map {
          case (name, tableFile) => InputTable(name, Source.fromFile(tableFile))
        }
      case _ =>
        require(inFiles.map(_.getName.stripSuffix(ext)).distinct.size == inFiles.size,
                "Duplicate samples exist in inputs")
        inFiles
          .map(tableFile =>
            InputTable(tableFile.getName.stripSuffix(ext), Source.fromFile(tableFile)))
    }
  }

  /** Creates the output writer object */
  def prepOutput(outFile: File): BufferedWriter = outFile match {
    case f if f.toString == "-" => new BufferedWriter(new OutputStreamWriter(System.out))
    case otherwise => new BufferedWriter(new FileWriter(otherwise))
  }

  /** Main entry point */
  def main(args: Array[String]): Unit = {
    val commandArgs: Args = parseArgs(args)

    import commandArgs._

    val outStream = prepOutput(out)
    val merged = mergeTables(prepInput(inputTables, fileExtension, columnNames),
                             idColumnIndices,
                             valueColumnIndex,
                             numHeaderLines,
                             delimiter)
    writeOutput(merged, outStream, fallbackString, idColumnName)
    outStream.close()
  }
}
