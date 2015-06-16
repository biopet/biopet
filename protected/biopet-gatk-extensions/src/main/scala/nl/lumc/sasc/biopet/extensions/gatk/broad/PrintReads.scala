/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable

class PrintReads(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.PrintReads with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
}

object PrintReads {
  def apply(root: Configurable, input: File, output: File): PrintReads = {
    val br = new PrintReads(root)
    br.input_file :+= input
    br.out = output
    return br
  }
}