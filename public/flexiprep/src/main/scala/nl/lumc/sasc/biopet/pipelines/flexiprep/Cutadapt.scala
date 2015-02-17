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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.extensions.Ln
import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class Cutadapt(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Cutadapt(root) {
  var fastqc: Fastqc = _

  override def beforeCmd() {
    super.beforeCmd

    val foundAdapters = fastqc.foundAdapters.map(_.seq)
    if (default_clip_mode == "3") opt_adapter ++= foundAdapters
    else if (default_clip_mode == "5") opt_front ++= foundAdapters
    else if (default_clip_mode == "both") opt_anywhere ++= foundAdapters
  }

  override def cmdLine = {
    if (opt_adapter.nonEmpty || opt_anywhere.nonEmpty || opt_front.nonEmpty) {
      analysisName = getClass.getSimpleName
      super.cmdLine
    } else {
      analysisName = getClass.getSimpleName + "-ln"
      Ln(this, fastq_input, fastq_output, relative = true).cmd
    }
  }
}

object Cutadapt {
  def apply(root: Configurable, input: File, output: File): Cutadapt = {
    val cutadapt = new Cutadapt(root)
    cutadapt.fastq_input = input
    cutadapt.fastq_output = output
    cutadapt.stats_output = new File(output.getAbsolutePath.substring(0, output.getAbsolutePath.lastIndexOf(".")) + ".stats")
    return cutadapt
  }
}
