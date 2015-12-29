/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.pipelines.gatk

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.pipelines.gatk.variantcallers._
import nl.lumc.sasc.biopet.pipelines.shiva.ShivaVariantcallingTrait
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * ShivaVariantcalling with GATK variantcallers
 *
 * Created by pjvan_thof on 2/26/15.
 */
class ShivaVariantcalling(val root: Configurable) extends QScript with ShivaVariantcallingTrait {
  qscript =>
  def this() = this(null)

  /** Will generate all available variantcallers */
  override def callersList = {
    new HaplotypeCallerGvcf(this) ::
      new HaplotypeCallerAllele(this) ::
      new UnifiedGenotyperAllele(this) ::
      new UnifiedGenotyper(this) ::
      new HaplotypeCaller(this) ::
      super.callersList
  }
}

/** object to add default main method to pipeline */
object ShivaVariantcalling extends PipelineCommand