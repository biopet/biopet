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
package nl.lumc.sasc.biopet.extensions.seqtk

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Wrapper for the seqtk sample subcommand.
  * Written based on seqtk version 1.0-r63-dirty.
  */
class SeqtkSample(val parent: Configurable) extends Seqtk {

  /** input file */
  @Input(doc = "Input file (FASTQ or FASTA)", required = true)
  var input: File = _

  /** output file */
  @Output(doc = "Output file", required = true)
  var output: File = _

  var s: Option[Int] = config("seed")

  var sample: Double = _

  def cmdLine =
    required(executable) +
      " sample " +
      optional("-s", s) +
      required(input) +
      (if (sample > 1) required(sample.toInt) else required(sample)) +
      (if (outputAsStdout) "" else " > " + required(output))

}
