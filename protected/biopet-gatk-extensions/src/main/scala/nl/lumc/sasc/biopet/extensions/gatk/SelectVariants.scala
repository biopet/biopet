package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class SelectVariants(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.SelectVariants with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
}

object SelectVariants {
  def apply(root: Configurable, input: File, output: File): SelectVariants = {
    val sv = new SelectVariants(root)
    sv.variant = input
    sv.out = output
    return sv
  }
}