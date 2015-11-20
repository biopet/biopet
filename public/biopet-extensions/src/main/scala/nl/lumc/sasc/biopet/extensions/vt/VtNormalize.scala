package nl.lumc.sasc.biopet.extensions.vt

import java.io.File

import nl.lumc.sasc.biopet.core.{Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Output, Input}

/**
  * Created by pjvanthof on 20/11/15.
  */
class VtNormalize(val root: Configurable) extends Vt with Version with Reference {
  def versionRegex = """normalize (.*)""".r
  override def versionExitcode = List(0, 1)
  def versionCommand = executable + " normalize"

  @Input(required = true)
  var inputVcf: File = _

  @Output(required = true)
  var outputVcf: File = _

  var windowSize: Option[Int] = config("windows_size")

  var intervalsFile: Option[File] = config("intervals_file")

  var reference: File = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    reference = referenceFasta()
  }

  def cmdLine = required(executable) + required("normalize") +
    required("-o", outputVcf) +
    optional("-w", windowSize) +
    optional("-I", intervalsFile) +
    required("-r", reference)
    required(inputVcf)
}