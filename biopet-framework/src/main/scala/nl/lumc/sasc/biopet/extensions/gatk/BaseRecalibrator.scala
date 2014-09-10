package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class BaseRecalibrator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.BaseRecalibrator with GatkGeneral {
  override def afterGraph {
    super.afterGraph
    
    if (config.contains("scattercount")) scatterCount = config("scattercount")
    if (config.contains("dbsnp")) knownSites +:= new File(config("dbsnp"))
  }
}
  
object BaseRecalibrator {
  def apply(root: Configurable, input:File, output:File): BaseRecalibrator = {
    val br = new BaseRecalibrator(root)
    br.input_file :+= input
    br.out = output
    return br
  }
}