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
package nl.lumc.sasc.biopet.extensions.gmap

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference }
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/**
 * Wrapper for the gsnap command line tool
 * Written based on gsnap version 2014-05-15
 */
class GmapBuild(val root: Configurable) extends BiopetCommandLineFunction with Reference {

  /** default executable */
  executable = config("exe", default = "gmap_build", freeVar = false)

  /** input file */
  @Input(doc = "Input fasta files", required = true) //var input: List[File] = _
  var fastaFiles: List[File] = Nil

  /** genome directory */
  var dir: File = _

  /** genome database */
  var db: String = _

  override def defaultCoreMemory = 4.0

  override def versionRegex = """.* version (.*)""".r
  override def versionCommand = executable
  override def versionExitcode = List(0, 1, 255)

  override def beforeGraph: Unit = {
    super.beforeGraph
    jobOutputFile = new File(dir, ".log.out")
  }

  def cmdLine = {
    required(executable) +
      required("--dir", dir) +
      optional("--db", db) +
      repeat(fastaFiles)
  }
}
