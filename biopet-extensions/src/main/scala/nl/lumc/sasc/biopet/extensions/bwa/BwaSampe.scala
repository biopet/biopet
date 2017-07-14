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
package nl.lumc.sasc.biopet.extensions.bwa

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * BWA sampe wrapper
  *
  * based on executable version 0.7.10-r789
  *
  */
class BwaSampe(val parent: Configurable) extends Bwa with Reference {
  @Input(doc = "Fastq file R1", required = true)
  var fastqR1: File = _

  @Input(doc = "Fastq file R2", required = true)
  var fastqR2: File = _

  @Input(doc = "Sai file R1", required = true)
  var saiR1: File = _

  @Input(doc = "Sai file R2", required = true)
  var saiR2: File = _

  @Input(doc = "The reference file for the bam files.", required = true)
  var reference: File = _

  @Output(doc = "Output file SAM", required = false)
  var output: File = _

  var a: Option[Int] = config("a")
  var o: Option[Int] = config("o")
  var n: Option[Int] = config("n")
  var N: Option[Int] = config("N")
  var c: Option[Float] = config("c")
  var P: Boolean = config("P", default = false)
  var s: Boolean = config("s", default = false)
  var A: Boolean = config("A", default = false)

  var r: String = _

  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  def cmdLine: String =
    required(executable) +
      required("sampe") +
      optional("-a", a) +
      optional("-o", o) +
      optional("-n", n) +
      optional("-N", N) +
      optional("-c", c) +
      optional("-f", output) +
      optional("-r", r) +
      conditional(P, "-P") +
      conditional(s, "-s") +
      conditional(A, "-A") +
      required(reference) +
      required(saiR1) +
      required(saiR2) +
      required(fastqR1) +
      required(fastqR2)
}
