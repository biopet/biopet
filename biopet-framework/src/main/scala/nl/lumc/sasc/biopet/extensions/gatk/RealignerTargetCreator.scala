package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class RealignerTargetCreator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.RealignerTargetCreator with GatkGeneral {
  override val defaultVmem = "6G"
  memoryLimit = Some(2.5)

  if (config.contains("scattercount")) scatterCount = config("scattercount")

  if (config.contains("known")) known ++= config("known").asFileList
}

object RealignerTargetCreator {
  def apply(root: Configurable, input: File, outputDir: String): RealignerTargetCreator = {
    val re = new RealignerTargetCreator(root)
    re.input_file :+= input
    re.out = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.intervals")
    return re
  }
}