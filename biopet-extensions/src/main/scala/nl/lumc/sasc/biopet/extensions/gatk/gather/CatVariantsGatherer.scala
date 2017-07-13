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
package nl.lumc.sasc.biopet.extensions.gatk.gather

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.extensions.gatk.{CatVariants, CommandLineGATK}
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.queue.function.scattergather.{GatherFunction, ScatterGatherableFunction}

/**
 *
 * Currently this is the default gather for VCFs.
 * One can set a specific gatherer to use by adding @Gather before any output argument.
 * For example (used to be part of UG):
 *           \@Gather(className = "org.broadinstitute.gatk.queue.extensions.gatk.CatVariantsGatherer")
 *           \@Output(doc="File to which variants should be written",required=true)
 *           protected VariantContextWriter writer = null;
 *
 *           @deprecated
 */
class CatVariantsGatherer extends CatVariants(null) with GatherFunction {
  this.assumeSorted = true

  analysisName = "Gather_CatVariants"

  override val parent: ScatterGatherableFunction with BiopetCommandLineFunction = originalFunction match {
    case b: BiopetCommandLineFunction => b
    case _                            => null
  }

  override def freezeFieldValues() {
    val originalGATK = this.originalFunction.asInstanceOf[CommandLineGATK]

    this.reference = originalGATK.reference_sequence
    this.variant = this.gatherParts.zipWithIndex map { case (input, index) => new TaggedFile(input, "input" + index) }
    this.outputFile = this.originalOutput
    this.assumeSorted = true
    this.variant_index_type = originalGATK.variant_index_type
    this.variant_index_parameter = originalGATK.variant_index_parameter

    super.freezeFieldValues()
  }
}
