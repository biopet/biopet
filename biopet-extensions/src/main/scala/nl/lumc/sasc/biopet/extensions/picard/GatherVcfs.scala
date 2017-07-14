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

import nl.lumc.sasc.biopet.extensions.Tabix
import nl.lumc.sasc.biopet.utils.VcfUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

class GatherVcfs(val parent: Configurable) extends Picard {

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: Seq[File] = Nil

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  //FIXME: This is a workaround for this issue: https://github.com/broadinstitute/picard/issues/789
  def tabix: Option[Tabix] = if (createIndex) Some(Tabix(this, output)) else None

  @Output(required = false)
  private var index: File = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (createIndex) index = VcfUtils.getVcfIndexFile(output)
  }

  override def cmdLine: String =
    super.cmdLine +
      repeat("INPUT=", input, spaceSeparated = false) +
      required("OUTPUT=", output, spaceSeparated = false) + (tabix match {
      case Some(t) => s" && ${t.cmdLine}"
      case _ => ""
    })
}
