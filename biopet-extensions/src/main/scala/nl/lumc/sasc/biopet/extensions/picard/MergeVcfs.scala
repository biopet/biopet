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
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class MergeVcfs(val parent: Configurable) extends Picard with Reference {

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: Seq[File] = Nil

  @Input(doc = "Dict file.", required = false)
  var dict: File = _

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Output(required = false)
  private var index: File = _

  override def dictRequired = true

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (createIndex) index = VcfUtils.getVcfIndexFile(output)
    if (dict == null) dict = referenceDictFile
  }

  override def cmdLine: String =
    super.cmdLine +
      repeat("INPUT=", input, spaceSeparated = false) +
      required("OUTPUT=", output, spaceSeparated = false) +
      optional("SEQUENCE_DICTIONARY=", dict, spaceSeparated = false)
}
