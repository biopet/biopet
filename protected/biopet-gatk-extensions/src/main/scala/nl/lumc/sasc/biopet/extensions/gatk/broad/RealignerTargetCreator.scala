/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

class RealignerTargetCreator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.RealignerTargetCreator with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")

  if (config.contains("known")) known ++= config("known").asFileList
}

object RealignerTargetCreator {
  def apply(root: Configurable, input: File, outputDir: File): RealignerTargetCreator = {
    val re = new RealignerTargetCreator(root)
    re.input_file :+= input
    re.out = new File(outputDir, input.getName.stripSuffix(".bam") + ".realign.intervals")
    return re
  }
}