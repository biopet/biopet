package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class CombineGVCFs(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.CombineGVCFs with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
}

object CombineGVCFs {
  def apply(root: Configurable, input: List[File], output: File): CombineGVCFs = {
    val cg = new CombineGVCFs(root)
    cg.variant = input
    cg.o = output
    return cg
  }
}