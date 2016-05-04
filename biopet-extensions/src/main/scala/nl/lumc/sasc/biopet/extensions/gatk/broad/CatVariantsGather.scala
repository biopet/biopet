package nl.lumc.sasc.biopet.extensions.gatk.broad

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import org.broadinstitute.gatk.queue.extensions.gatk.TaggedFile
import org.broadinstitute.gatk.queue.function.scattergather.GatherFunction

/**
 *
 * Currently this is the default gather for VCFs.
 * One can set a specific gatherer to use by adding @Gather before any output argument.
 * For example (used to be part of UG):
 *           \@Gather(className = "org.broadinstitute.gatk.queue.extensions.gatk.CatVariantsGatherer")
 *           \@Output(doc="File to which variants should be written",required=true)
 *           protected VariantContextWriter writer = null;
 */
class CatVariantsGatherer extends CatVariants(null) with GatherFunction {
  this.assumeSorted = true

  analysisName = "Gather_CatVariants"

  override val root = originalFunction match {
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
