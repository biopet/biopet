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
package nl.lumc.sasc.biopet.pipelines.gentrap

import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.extensions.picard._
import org.broadinstitute.gatk.queue.function._
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }
import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 */
class Gentrap(val root: Configurable) extends QScript with BiopetQScript {

  def this() = this(null)

  /** Read 1 input */
  @Input(doc = "FASTQ file input (single-end or pair 1)", fullName = "input_r1", shortName = "R1", required = true)
  var inputR1: File = _

  /** Read 2 input (optional) */
  @Input(doc = "FASTQ file input (pair 2)", fullName = "input_r2", shortName = "R2", required = false)
  var inputR2: File = _

  /** Split aligner to use */
  @Argument(doc = "Split aligner", fullName = "aligner", shortName = "aln", required = true, validation = "gsnap|tophat|star")
  var aligner: String = _

  /** Whether library is strand-specifc (dUTP protocol) or not */
  @Argument(doc = "Whether input data was made using the dUTP strand-specific protocol", fullName = "strand_specific", shortName = "strandSpec", required = true)
  var strandSpec: Boolean = _

  /** Variant calling */
  @Argument(doc = "Variant caller", fullName = "variant_caller", shortName = "varCaller", required = false, validation = "varscan|snvmix")
  var varcaller: String = _

  /** Cufflinks assembly type */
  @Argument(doc = "Cufflinks assembly type", fullName = "transcript_asm", shortName = "transAsm", required = false, validation = "none|strict|guided|blind")
  var asm: List[String] = List("none")

  /** FASTQ trimming */
  @Argument(doc = "Whether to skip trimming input files", fullName = "skip_trim_input", shortName = "skipTrim", required = false)
  var skipTrim: Boolean = false

  /** FASTQ clipping */
  @Argument(doc = "Whether to skip clipping input files", fullName = "skip_clip_input", shortName = "skipClip", required = false)
  var skipClip: Boolean = false

  /** Gene-wise read count table output */
  @Argument(doc = "Gene read count table output", fullName = "count_gene_read", shortName = "cGeneRead", required = false)
  var cGeneRead: Boolean = _

  /** Gene-wise base count table output */
  @Argument(doc = "Gene base count table output", fullName = "count_gene_base", shortName = "cGeneBase", required = false)
  var cGeneBase: Boolean = _

  /** Exon-wise base count table output */
  @Argument(doc = "Exon base count table output", fullName = "count_exon_base", shortName = "cExonBase", required = false)
  var cExonBase: Boolean = _

  def init() {

  }

  def biopetScript() {
  }
}

object Gentrap extends PipelineCommand
