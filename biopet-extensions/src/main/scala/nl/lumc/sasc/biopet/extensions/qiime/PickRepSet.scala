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
package nl.lumc.sasc.biopet.extensions.qiime

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/**
  * Created by pjvan_thof on 12/4/15.
  */
class PickRepSet(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "pick_rep_set.py")

  @Input(required = true)
  var inputFile: File = _

  @Output
  var outputFasta: Option[File] = None

  @Output
  var logFile: Option[File] = None

  @Input(required = false)
  var referenceSeqsFp: Option[File] = config("reference_seqs_fp")

  @Input(required = false)
  var fastaInput: Option[File] = None

  var sortBy: Option[String] = config("sort_by")

  def versionCommand: String = executable + " --version"
  def versionRegex: List[Regex] = """Version: (.*)""".r :: Nil

  var repSetPickingMethod: Option[String] = config("rep_set_picking_method")

  def cmdLine: String =
    executable +
      required("-i", inputFile) +
      required("-o", outputFasta) +
      optional("-m", repSetPickingMethod) +
      optional("-f", fastaInput) +
      optional("-l", logFile) +
      optional("-s", sortBy) +
      optional("-r", referenceSeqsFp)
}
