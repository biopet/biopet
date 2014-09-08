package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class ApplyRecalibration(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.ApplyRecalibration with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
  
  nt = Option(getThreads(3))
  memoryLimit = Option(nt.getOrElse(1) * 2)
  
  override def afterGraph {
    super.afterGraph
    ts_filter_level = config("ts_filter_level")
  }
}
  
object ApplyRecalibration {
  def apply(root: Configurable, input:File, output:File, recal_file:File, tranches_file:File, indel: Boolean = false): ApplyRecalibration = {
    val ar = if (indel) new ApplyRecalibration(root) {
      mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.INDEL
      defaults ++= Map("ts_filter_level" -> 99.0)
    } else new ApplyRecalibration(root) {
      mode = org.broadinstitute.gatk.tools.walkers.variantrecalibration.VariantRecalibratorArgumentCollection.Mode.SNP
      defaults ++= Map("ts_filter_level" -> 99.5)
    }
    ar.input :+= input
    ar.recal_file = recal_file
    ar.tranches_file = tranches_file
    ar.out = output
    return ar
  }
}