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

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.{Cat, Ln}

class SeqtkSeq(root: Configurable) extends nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSeq(root) {
  var fastqc: Fastqc = _

  override def beforeCmd() {
    super.beforeCmd()
    if (fastqc != null && Q.isEmpty) {
      val encoding = fastqc.encoding
      Q = encoding match {
        case null => None
        case enc if enc.contains("Sanger / Illumina 1.9") => None
        case enc if enc.contains("Illumina <1.3") => Option(64)
        case enc if enc.contains("Illumina 1.3") => Option(64)
        case enc if enc.contains("Illumina 1.5") => Option(64)
        case _ => None
      }
      if (Q.isDefined) V = true
    }
  }

  override def beforeGraph() {
    if (fastqc != null) deps ::= fastqc.output
  }

  override def cmdLine = {
    if (Q.isDefined) {
      analysisName = getClass.getSimpleName
      super.cmdLine
    } else {
      analysisName = getClass.getSimpleName + "-skip"
      (inputAsStdin, outputAsStsout) match {
        case (true, true) => super.cmdLine
        case _ => Ln(this, input, output).cmd
      }
    }
  }
}

object SeqtkSeq {
  def apply(root: Configurable, fastqc: Fastqc = null): SeqtkSeq = {
    val seqtkSeq = new SeqtkSeq(root)
    seqtkSeq.fastqc = fastqc
    seqtkSeq
  }

  def apply(root: Configurable, input: File, output: File, fastqc: Fastqc = null): SeqtkSeq = {
    val seqtkSeq = new SeqtkSeq(root)
    seqtkSeq.input = input
    seqtkSeq.output = output
    seqtkSeq.fastqc = fastqc
    seqtkSeq
  }
}