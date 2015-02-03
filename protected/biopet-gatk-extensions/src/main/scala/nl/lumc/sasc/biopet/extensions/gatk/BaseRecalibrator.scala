/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class BaseRecalibrator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.BaseRecalibrator with GatkGeneral {
  memoryLimit = Option(4)
  override val defaultVmem = "8G"

  if (config.contains("scattercount")) scatterCount = config("scattercount", default = 1)
  if (config.contains("dbsnp")) knownSites :+= new File(config("dbsnp").asString)
  if (config.contains("known_sites")) knownSites :+= new File(config("known_sites").asString)
}

object BaseRecalibrator {
  def apply(root: Configurable, input: File, output: File): BaseRecalibrator = {
    val br = new BaseRecalibrator(root)
    br.input_file :+= input
    br.out = output
    br.afterGraph
    return br
  }
}