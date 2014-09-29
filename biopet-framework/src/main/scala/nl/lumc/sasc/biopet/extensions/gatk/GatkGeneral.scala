package nl.lumc.sasc.biopet.extensions.gatk

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK

trait GatkGeneral extends CommandLineGATK with BiopetJavaCommandLineFunction {
  memoryLimit = Option(2)
  
  override val defaultVmem = "6G"
  
  if (config.contains("intervals", submodule = "gatk")) intervals = config("intervals", submodule = "gatk").getFileList
  if (config.contains("exclude_intervals", submodule = "gatk")) excludeIntervals = config("exclude_intervals", submodule = "gatk").getFileList
  reference_sequence = config("reference", submodule = "gatk")
  gatk_key = config("gatk_key", submodule = "gatk")
  if (config.contains("pedigree", submodule = "gatk")) pedigree = config("pedigree", submodule = "gatk").getFileList
}
