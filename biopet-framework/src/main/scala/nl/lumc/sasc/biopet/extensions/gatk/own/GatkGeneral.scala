package nl.lumc.sasc.biopet.extensions.gatk.own

import java.io.File
import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

trait GatkGeneral extends BiopetJavaCommandLineFunction {
  val analysis: String

  memoryLimit = Option(3)
  jarFile = config("gatk_jar", submodule = "gatk")

  override val defaultVmem = "7G"

  @Input(required = false)
  var intervals: List[File] = config("intervals", submodule = "gatk").getFileList

  @Input(required = false)
  var excludeIntervals: List[File] = config("exclude_intervals", submodule = "gatk").getFileList

  var reference_sequence: File = config("reference", submodule = "gatk")

  var gatkKey: File = config("gatk_key", submodule = "gatk")

  @Input(required = false)
  var pedigree: List[File] = config("pedigree", submodule = "gatk").getFileList

  override def commandLine = super.commandLine +
    required("--analysis_type", analysis) +
    required("--reference_sequence", reference_sequence) +
    repeat("--intervals", intervals) +
    repeat("--excludeIntervals", excludeIntervals) +
    optional("--gatk_key", gatkKey) +
    repeat("--pedigree", pedigree)
}
