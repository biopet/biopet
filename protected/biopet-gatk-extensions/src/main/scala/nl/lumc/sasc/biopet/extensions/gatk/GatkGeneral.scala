/**
 * Due to the license issue with GATK, this part of Biopet can only be used inside the
 * LUMC. Please refer to https://git.lumc.nl/biopet/biopet/wikis/home for instructions
 * on how to use this protected part of biopet or contact us at sasc@lumc.nl
 */
package nl.lumc.sasc.biopet.extensions.gatk

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK

trait GatkGeneral extends CommandLineGATK with BiopetJavaCommandLineFunction {
  memoryLimit = Option(3)

  if (config.contains("gatk_jar")) jarFile = config("gatk_jar")

  override val defaultVmem = "7G"

  if (config.contains("intervals", submodule = "gatk")) intervals = config("intervals", submodule = "gatk").asFileList
  if (config.contains("exclude_intervals", submodule = "gatk")) excludeIntervals = config("exclude_intervals", submodule = "gatk").asFileList
  reference_sequence = config("reference", submodule = "gatk")
  gatk_key = config("gatk_key", submodule = "gatk")
  if (config.contains("pedigree", submodule = "gatk")) pedigree = config("pedigree", submodule = "gatk").asFileList
}
