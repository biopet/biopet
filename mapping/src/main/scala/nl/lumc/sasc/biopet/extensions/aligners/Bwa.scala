package nl.lumc.sasc.biopet.extensions.aligners

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Bwa(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: File = _

  @Input(doc = "The reference file for the bam files.", shortName = "R")
  var reference: File = config("reference", required = true)

  @Output(doc = "Output file SAM", shortName = "output")
  var output: File = _

  var RG: String = _
  var M: Boolean = config("M", default = true)

  executable = config("exe", default = "bwa")
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0, 1)

  override val defaultVmem = "6G"
  override val defaultThreads = 8

  override def versionCommand = executable

  def cmdLine = {
    required(executable) +
      required("mem") +
      optional("-t", nCoresRequest) +
      optional("-R", RG) +
      conditional(M, "-M") +
      required(reference) +
      required(R1) +
      optional(R2) +
      " > " + required(output)
  }
}
