/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable

class VariantAnnotator(val root: Configurable) extends org.broadinstitute.gatk.queue.extensions.gatk.VariantAnnotator with GatkGeneral {
  if (config.contains("scattercount")) scatterCount = config("scattercount")
  dbsnp = config("dbsnp")
}

object VariantAnnotator {
  def apply(root: Configurable, input: File, bamFiles: List[File], output: File): VariantAnnotator = {
    val va = new VariantAnnotator(root)
    va.variant = input
    va.input_file = bamFiles
    va.out = output
    va
  }
}