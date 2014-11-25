package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class CombineVariants(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.CombineVariants with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
}

object CombineVariants {
  def apply(root: Configurable, input: List[File], output: File): CombineVariants = {
    val cv = new CombineVariants(root)
    cv.variant = input
    cv.out = output
    return cv
  }
}