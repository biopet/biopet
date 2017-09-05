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
package nl.lumc.sasc.biopet.core.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

import scala.util.matching.Regex

/** Extension for md5sum */
class Md5sum(val parent: Configurable) extends BiopetCommandLineFunction with Version {
  mainFunction = false

  @Input(doc = "Input")
  var input: File = _

  @Output(doc = "Output")
  var output: File = _

  executable = config("exe", default = "md5sum")

  def versionRegex: List[Regex] = """md5sum \(GNU coreutils\) (.*)""".r :: Nil
  def versionCommand: String = executable + " --version"

  /** return commandline to execute */
  def cmdLine: String = required(executable) + required(input) + " > " + required(output)
}

/** Object for constructors for md5sum */
object Md5sum {

  /** Makes md5sum with md5 file in given dir */
  def apply(root: Configurable, fastqfile: File, outDir: File): Md5sum = {
    val md5sum = new Md5sum(root)
    md5sum.input = fastqfile
    md5sum.output = new File(outDir, fastqfile.getName + ".md5")
    md5sum
  }

  /** Makes md5sum with md5 file in same dir as input file */
  def apply(root: Configurable, file: File): Md5sum = {
    val md5sum = new Md5sum(root)
    md5sum.input = file
    md5sum.output = new File(file.getParentFile, file.getName + ".md5")
    md5sum
  }
}
