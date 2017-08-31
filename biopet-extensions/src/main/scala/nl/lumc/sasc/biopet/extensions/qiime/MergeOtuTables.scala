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
  * Created by pjvan_thof on 12/10/15.
  */
class MergeOtuTables(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  executable = config("exe", default = "merge_otu_tables.py")

  def versionCommand: String = executable + " --version"
  def versionRegex: List[Regex] = """Version: (.*)""".r :: Nil

  @Input(required = true)
  var input: List[File] = Nil

  @Output(required = true)
  var outputFile: File = _

  override def defaultCoreMemory = 4.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    require(input.nonEmpty)
    require(outputFile != null)
  }

  def cmdLine: String =
    executable +
      (input match {
        case l: List[_] if l.nonEmpty => required("-i", l.mkString(","))
        case _ => ""
      }) +
      required("-o", outputFile)
}
