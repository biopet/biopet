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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Reference, ToolCommandFunction }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class BastyGenerateFasta(val root: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.BastyGenerateFasta

  @Input(doc = "Input vcf file", required = false)
  var inputVcf: File = _

  @Input(doc = "Bam File", required = false)
  var bamFile: File = _

  @Input(doc = "reference", required = false)
  var reference: File = _

  @Output(doc = "Output fasta, variants only", required = false)
  var outputVariants: File = _

  @Output(doc = "Output fasta, variants only", required = false)
  var outputConsensus: File = _

  @Output(doc = "Output fasta, variants only", required = false)
  var outputConsensusVariants: File = _

  var snpsOnly: Boolean = config("snps_only", default = false)
  var sampleName: String = _
  var minAD: Int = config("min_ad", default = 8)
  var minDepth: Int = config("min_depth", default = 8)
  var outputName: String = _

  override def defaultCoreMemory = 4.0

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    reference = referenceFasta()
  }

  override def cmdLine = super.cmdLine +
    optional("--inputVcf", inputVcf) +
    optional("--bamFile", bamFile) +
    optional("--outputVariants", outputVariants) +
    optional("--outputConsensus", outputConsensus) +
    optional("--outputConsensusVariants", outputConsensusVariants) +
    conditional(snpsOnly, "--snpsOnly") +
    optional("--sampleName", sampleName) +
    required("--outputName", outputName) +
    optional("--minAD", minAD) +
    optional("--minDepth", minDepth) +
    optional("--reference", reference)
}
