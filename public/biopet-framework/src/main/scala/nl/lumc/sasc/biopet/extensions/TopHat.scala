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
  @Input(doc = "FastQ file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "FastQ file R2", shortName = "R2", required = false)
  var R2: File = _

  @Input(doc = "Bowtie index", shortName = "bti")
  var bowtie_index: File = config("bowtie_index")

  @Argument(doc = "Output Directory")
  var outputDir: String = _

  @Output(doc = "Output file SAM", shortName = "output")
  var output: File = _

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

  override val defaultVmem = "4G"
  override val defaultThreads = 8

  override def versionCommand = executable + " --version"

  override def beforeGraph() {
    if (!outputDir.endsWith("/")) outputDir += "/"
    output = new File(outputDir + "accepted_hits.bam")
  }

  def cmdLine: String = {
    var cmd: String = required(executable) +
      optional("-p", nCoresRequest) +
      "--no-convert-bam" +
      required(bowtie_index) +
      required(R1) + optional(R2)
    return cmd
  }
}
