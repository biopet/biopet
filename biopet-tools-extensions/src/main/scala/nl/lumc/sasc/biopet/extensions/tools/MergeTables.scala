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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.mutable.{ Set => MutSet }

/**
 * Biopet wrapper for the [[MergeTables]] command line tool.
 *
 * @param root [[Configurable]] object
 */
class MergeTables(val root: Configurable) extends ToolCommandFunction {

  def toolObject = nl.lumc.sasc.biopet.tools.MergeTables

  override def defaultCoreMemory = 6.0

  /** List of input tabular files */
  @Input(doc = "Input table files", required = true)
  var inputTables: List[File] = List.empty[File]

  /** Output file */
  @Output(doc = "Output merged table", required = true)
  var output: File = null

  // TODO: should be List[Int] really
  /** List of column indices to combine to make a unique identifier per row */
  var idColumnIndices: List[String] = config("id_column_indices", default = List("1"))

  /** Index of column from each tabular file containing the values to be put in the final merged table */
  var valueColumnIndex: Int = config("value_column_index", default = 2)

  /** Name of the identifier column in the output file */
  var idColumnName: Option[String] = config("id_column_name")

  /** Common file extension of all input files */
  var fileExtension: Option[String] = config("file_extension")

  /** Number of header lines from each input file to ignore */
  var numHeaderLines: Option[Int] = config("num_header_lines")

  /** String to use when a value is missing from an input file */
  var fallbackString: Option[String] = config("fallback_string")

  /** Column delimiter of each input file (used for splitting into columns */
  var delimiter: Option[String] = config("delimiter")

  // executed command line
  override def cmdLine =
    super.cmdLine +
      required("-i", idColumnIndices.mkString(",")) +
      required("-a", valueColumnIndex) +
      optional("-n", idColumnName) +
      optional("-e", fileExtension) +
      optional("-m", numHeaderLines) +
      optional("-f", fallbackString) +
      optional("-d", delimiter) +
      required("-o", output) +
      required("", repeat(inputTables), escape = false)
}

object MergeTables {
  def apply(root: Configurable,
            tables: List[File],
            outputFile: File,
            idCols: List[Int],
            valCol: Int,
            numHeaderLines: Int = 0,
            fallback: String = "-",
            fileExtension: Option[String] = None): MergeTables = {
    val job = new MergeTables(root)
    job.inputTables = tables
    job.output = outputFile
    job.idColumnIndices = idCols.map(_.toString)
    job.valueColumnIndex = valCol
    job.fallbackString = Option(fallback)
    job.numHeaderLines = Option(numHeaderLines)
    job.fileExtension = fileExtension
    job
  }
}