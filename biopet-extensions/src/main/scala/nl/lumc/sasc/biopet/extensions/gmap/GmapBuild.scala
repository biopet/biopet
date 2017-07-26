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
package nl.lumc.sasc.biopet.extensions.gmap

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

import scala.util.matching.Regex

/**
  * Wrapper for the gsnap command line tool
  * Written based on gsnap version 2014-05-15
  */
class GmapBuild(val parent: Configurable)
    extends BiopetCommandLineFunction
    with Reference
    with Version {

  /** default executable */
  executable = config("exe", default = "gmap_build", freeVar = false)

  /** input file */
  @Input(doc = "Input fasta files", required = true) //var input: List[File] = _
  var fastaFiles: List[File] = Nil

  /** genome directory */
  var dir: File = _

  /** genome database */
  var db: String = _

  override def defaultCoreMemory = 25.0

  def versionRegex: Regex = """.* version (.*)""".r
  def versionCommand: String = executable
  override def versionExitcode = List(0, 1, 255)

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    jobOutputFile = new File(dir, ".log.out")
  }

  def cmdLine: String = {
    required(executable) +
      required("--dir", dir) +
      optional("--db", db) +
      repeat(fastaFiles)
  }
}
