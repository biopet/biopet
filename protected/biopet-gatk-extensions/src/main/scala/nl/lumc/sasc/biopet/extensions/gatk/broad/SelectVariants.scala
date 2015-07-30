/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

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
    sv
  }
}