package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class PrintReads(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.PrintReads with GatkGeneral {
  memoryLimit = Option(4)
  
  override val defaultVmem = "8G"
  
  if (config.contains("scattercount")) scatterCount = config("scattercount")
}
  
object PrintReads {
  def apply(root: Configurable, input:File, output:File): PrintReads = {
    val br = new PrintReads(root)
    br.input_file :+= input
    br.out = output
    return br
  }
}