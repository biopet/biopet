package nl.lumc.sasc.biopet.function.aligners

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File
import scala.sys.process._

class Bwa(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Fastq file R1", shortName = "R1")
  var R1: File = _

  @Input(doc = "Fastq file R2", shortName = "R2", required = false)
  var R2: File = _

  @Input(doc = "The reference file for the bam files.", shortName = "R")
  var referenceFile: File = config("referenceFile", required = true)

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
      required(referenceFile) +
      required(R1) +
      optional(R2) +
      " > " + required(output)
  }
}
