/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk.broad

import nl.lumc.sasc.biopet.core.{ Reference, BiopetJavaCommandLineFunction }
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK

trait GatkGeneral extends CommandLineGATK with BiopetJavaCommandLineFunction with Reference {
  memoryLimit = Option(3)

  override def subPath = "gatk" :: super.subPath

  jarFile = config("gatk_jar")

  override val defaultCoreMemory = 4.0

  if (config.contains("intervals")) intervals = config("intervals").asFileList
  if (config.contains("exclude_intervals")) excludeIntervals = config("exclude_intervals").asFileList
  if (config.contains("gatk_key")) gatk_key = config("gatk_key")
  if (config.contains("pedigree")) pedigree = config("pedigree")

  override def beforeGraph: Unit = {
    super.beforeGraph
    if (reference_sequence == null) reference_sequence = referenceFasta()
  }
}
