/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable

class ApplyRecalibration(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.ApplyRecalibration with GatkGeneral {
  scatterCount = config("scattercount", default = 0)

  override val defaultThreads = 3

  override def beforeGraph() {
    super.beforeGraph()

    nt = Option(getThreads)
    memoryLimit = Option(nt.getOrElse(1) * 2)

    import org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode
    if (mode == Mode.INDEL) ts_filter_level = config("ts_filter_level", default = 99.0)
    else if (mode == Mode.SNP) ts_filter_level = config("ts_filter_level", default = 99.5)
    ts_filter_level = config("ts_filter_level")
  }
}

object ApplyRecalibration {
  def apply(root: Configurable, input: File, output: File, recal_file: File, tranches_file: File, indel: Boolean = false): ApplyRecalibration = {
    val ar = if (indel) new ApplyRecalibration(root) {
      mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
    }
    else new ApplyRecalibration(root) {
      mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
    }
    ar.input :+= input
    ar.recal_file = recal_file
    ar.tranches_file = tranches_file
    ar.out = output
    ar
  }
}