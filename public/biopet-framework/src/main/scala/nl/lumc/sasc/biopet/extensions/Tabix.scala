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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Wrapper for the tabix command
 *
 * Note that tabix can either index a file (no stdout stream) or retrieve regions from an indexed file (stdout stream)
 *
 */
class Tabix(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "Input bgzipped file", required = true)
  var input: File = null

  @Output(doc = "Output (for region query)", required = false)
  var outputQuery: File = null

  @Output(doc = "Output (for indexing)", required = false) // NOTE: it's a def since we can't change the index name ~ it's always input_name + .tbi
  lazy val outputIndex: File = {
    require(input != null, "Input must be defined")
    new File(input.toString + ".tbi")
  }

  @Argument(doc = "Regions to query", required = false)
  var regions: List[String] = config("regions", default = List.empty[String])

  var p: Option[String] = config("p")
  var s: Option[Int] = config("s")
  var b: Option[Int] = config("b")
  var e: Option[Int] = config("e")
  var S: Option[Int] = config("S")
  var c: Option[String] = config("c")
  var r: Option[File] = config("r")
  var B: Boolean = config("B", default = false)
  var zero: Boolean = config("0", default = false)
  var h: Boolean = config("h", default = false)
  var l: Boolean = config("l", default = false)
  var f: Boolean = config("f", default = false)

  executable = config("exe", default = "tabix")

  override def versionCommand = executable
  override def versionRegex = """Version: (.*)""".r
  override def versionExitcode = List(0, 1)

  /** Formats that tabix can handle */
  private val validFormats: Set[String] = Set("gff", "bed", "sam", "vcf", "psltbl")

  override def beforeGraph: Unit = {
    p match {
      case Some(fmt) =>
        require(validFormats.contains(fmt), "-p flag must be one of " + validFormats.mkString(", "))
      case None => ;
    }
  }

  def cmdLine = {
    val baseCommand = required(executable) +
      optional("-p", p) +
      optional("-s", s) +
      optional("-b", b) +
      optional("-e", e) +
      optional("-S", S) +
      optional("-c", c) +
      optional("-r", r) +
      conditional(B, "-B") +
      conditional(zero, "-0") +
      conditional(h, "-h") +
      conditional(l, "-l") +
      conditional(f, "-f") +
      required(input)

    // query mode ~ we want to output to a file
    if (regions.nonEmpty) baseCommand + required("", repeat(regions), escape = false) + " > " + required(outputQuery)
    // indexing mode
    else baseCommand
  }
}
