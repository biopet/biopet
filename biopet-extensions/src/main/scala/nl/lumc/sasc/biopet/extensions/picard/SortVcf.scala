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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/** Extension for picard SortVcf */
class SortVcf(val parent: Configurable) extends Picard with Reference {

  @Input(doc =
           "Input VCF(s) to be sorted. Multiple inputs must have the same sample names (in order)",
         required = true)
  var input: File = _

  @Output(doc = "Output VCF to be written.", required = true)
  var output: File = _

  @Input(doc = "Sequence dictionary to use", required = true)
  var sequenceDictionary: File = _

  @Output
  private var outputIndex: File = _

  override val dictRequired = true

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (output.getName.endsWith(".vcf.gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    if (output.getName.endsWith(".vcf")) outputIndex = new File(output.getAbsolutePath + ".idx")
    if (sequenceDictionary == null) sequenceDictionary = referenceDictFile
  }

  /** Returns command to execute */
  override def cmdLine =
    super.cmdLine +
      (if (inputAsStdin) required("INPUT=", new File("/dev/stdin"), spaceSeparated = false)
       else required("INPUT=", input, spaceSeparated = false)) +
      (if (outputAsStdout) required("OUTPUT=", new File("/dev/stdout"), spaceSeparated = false)
       else required("OUTPUT=", output, spaceSeparated = false)) +
      required("SEQUENCE_DICTIONARY=", sequenceDictionary, spaceSeparated = false)
}

object SortVcf {

  /** Returns default SortSam */
  def apply(root: Configurable, input: File, output: File): SortVcf = {
    val sortVcf = new SortVcf(root)
    sortVcf.input = input
    sortVcf.output = output
    sortVcf
  }
}
