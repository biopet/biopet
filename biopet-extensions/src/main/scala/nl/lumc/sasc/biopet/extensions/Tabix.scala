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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Wrapper for the tabix command
 *
 * Note that tabix can either index a file (no stdout stream) or retrieve regions from an indexed file (stdout stream)
 *
 */
class Tabix(val root: Configurable) extends BiopetCommandLineFunction with Version {

  @Input(doc = "Input bgzipped file", required = true)
  var input: File = null

  @Output(doc = "Output (for region query)", required = false)
  var outputQuery: File = null

  def outputIndex: File = {
    require(input != null, "Input should be defined")
    new File(input.getAbsolutePath + ".tbi")
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

  executable = config("exe", default = "tabix", freeVar = false)

  def versionCommand = executable
  def versionRegex = """Version: (.*)""".r
  override def versionExitcode = List(0, 1)

  /** Formats that tabix can handle */
  private val validFormats: Set[String] = Set("gff", "bed", "sam", "vcf", "psltbl")

  @Output
  var outputFiles: List[File] = Nil

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    p match {
      case Some(fmt) =>
        require(validFormats.contains(fmt), "-p flag must be one of " + validFormats.mkString(", "))
        outputFiles :+= outputIndex
      case None =>
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

object Tabix {
  def apply(root: Configurable, input: File) = {
    val tabix = new Tabix(root)
    tabix.input = input
    tabix.p = tabix.input.getName match {
      case s if s.endsWith(".vcf.gz")    => Some("vcf")
      case s if s.endsWith(".bed.gz")    => Some("bed")
      case s if s.endsWith(".sam.gz")    => Some("sam")
      case s if s.endsWith(".gff.gz")    => Some("gff")
      case s if s.endsWith(".psltbl.gz") => Some("psltbl")
      case _                             => throw new IllegalArgumentException("Unknown file type")
    }
    tabix
  }
}
