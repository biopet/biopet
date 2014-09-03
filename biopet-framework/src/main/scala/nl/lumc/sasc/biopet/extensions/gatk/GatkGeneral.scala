package nl.lumc.sasc.biopet.extensions.gatk

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.queue.extensions.gatk.CommandLineGATK

trait GatkGeneral extends CommandLineGATK with BiopetJavaCommandLineFunction {
  memoryLimit = Option(2)
  
  override val defaultVmem = "4G"
  
  if (config.contains("intervals")) intervals = config("intervals").getFileList
  if (config.contains("exclude_intervals")) excludeIntervals = config("exclude_intervals").getFileList
  reference_sequence = config("reference")
  gatk_key = config("gatk_key")
  if (config.contains("pedigree")) pedigree = config("pedigree").getFileList
}
