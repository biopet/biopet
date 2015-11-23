package nl.lumc.sasc.biopet.extensions.vt

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * Created by pjvanthof on 20/11/15.
  */
class VtDecompose(val root: Configurable) extends Vt with Version with Reference {
  def versionRegex = """decompose (.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand = executable + " decompose"

  @Input(required = true)
  var inputVcf: File = _

  @Output(required = true)
  var outputVcf: File = _

  var intervalsFile: Option[File] = config("intervals_file")

  val smartDecompose: Boolean = config("smart_decompose", default = false)

  def cmdLine = required(executable) + required("decompose") +
    required("-o", outputVcf) +
    optional("-I", intervalsFile) +
    conditional(smartDecompose, "-s") +
    required(inputVcf)
}