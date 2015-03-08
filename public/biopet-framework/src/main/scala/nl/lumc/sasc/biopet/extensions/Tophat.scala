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

class TopHat(val root: Configurable) extends BiopetCommandLineFunction {

  @Input(doc = "FastQ file(s) R1", shortName = "R1")
  var R1: List[File] = List.empty[File]

  @Input(doc = "FastQ file(s) R2", shortName = "R2", required = false)
  var R2: List[File] = List.empty[File]

  /** output files, computed automatically from output directory */

  @Output(doc = "Output SAM/BAM file")
  lazy val outputGtf: File = {
    require(R1.nonEmpty && output_dir != null,
      "Read 1 input(s) are defined and output directory is defined")
    // cufflinks always outputs a transcripts.gtf file in the output directory
    new File(output_dir, "accepted_hits.bam")
  }

  @Argument(doc = "Bowtie index", shortName = "bti", required = true)
  var bowtie_index: String = config("bowtie_index")

  /** write all output files to this directory [./] */
  var output_dir: File = config("output_dir", default = new File("tophat_out"))

  // options set via API or config
  //  var numrecords: String = config("numrecords", default = "all")
  //  var solexa: Boolean = config("solexa", default = false)
  //  var solexaold: Boolean = config("solexaold", default = false)
  //  var sanger: Boolean = config("sanger", default = false)
  //
  //  var insertsize: Option[Int] = config("insertsize", default = 250)
  //  var insertsd: Option[Int] = config("insertsd", default = 60)
  //  var insertsize2: Option[Int] = config("insertsize2", default = -2000)
  //  var insertsd2: Option[Int] = config("insertsd2", default = -1)
  //
  //  var sensitive: Boolean = config("sensitive", default = false)
  //  var fast: Boolean = config("fast", default = false)
  //
  //  var readgroup: String = config("readgroup")
  //  var verbosity: Option[Int] = config("verbosity", default = 2)
  //  var logfile: String = config("logfile")

  executable = config("exe", default = "tophat", freeVar = false)

  override val versionRegex = """TopHat v(.*)""".r
  override val versionExitcode = List(0, 1)
  override def versionCommand = executable + " --version"

  override val defaultVmem = "4G"
  override val defaultThreads = 8

  //override def beforeGraph = {}

  def cmdLine: String = required(executable) +
    required(bowtie_index) +
    required(R1.mkString(",")) +
    optional(R2.mkString(","))
}
