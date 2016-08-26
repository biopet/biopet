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
package nl.lumc.sasc.biopet.extensions.bcftools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by sajvanderzeeuw on 16-10-15.
 */
class BcftoolsMerge(val root: Configurable) extends Bcftools {
  @Input(doc = "Input File", required = true)
  var input: List[File] = Nil

  @Output(doc = "output File", required = false)
  var output: File = _

  @Input(required = false)
  var R: Option[File] = config("R")

  @Input(required = false)
  var useheader: Option[File] = config("useheader")

  @Input(required = false)
  var l: Option[File] = config("l")

  var forcesamples: Boolean = config("forcesamples", default = false)
  var printheader: Boolean = config("printheader", default = false)
  var f: List[String] = config("f", default = Nil)
  var i: List[String] = config("i", default = Nil)
  var m: Option[String] = config("m")
  var O: Option[String] = config("O")
  var r: List[String] = config("r", default = Nil)

  def cmdLine = required(executable) +
    required("merge") +
    (if (outputAsStsout) "" else required("-o", output)) +
    conditional(forcesamples, "--force-samples") +
    conditional(printheader, "--print-header") +
    optional("--use-header", useheader) +
    optional("-f", f) +
    optional("-i", i) +
    optional("-l", l) +
    optional("-m", m) +
    optional("-O", O) +
    optional("-r", r) +
    optional("-R", R) +
    repeat(input)
}
