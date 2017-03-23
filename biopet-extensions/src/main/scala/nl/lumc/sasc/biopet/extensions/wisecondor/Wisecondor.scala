package nl.lumc.sasc.biopet.extensions.wisecondor

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference, Version }
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by Sander Bollen on 20-3-17.
 */
abstract class Wisecondor extends BiopetCommandLineFunction with Version with Reference {
  executable = config("exe", namespace = "wisecondor", default = "wisecondor")

  @Input(required = false)
  var binFile: Option[File] = config("bin_file", namespace = "wisecondor", default = None)

  @Output
  var output: File = _

  // either binSize or binFile must exist
  var binSize: Option[Int] = config("bin_size", namespace = "wisecondor", default = None)

  def binCommand: String = {
    if (binFile.isDefined && binSize.isEmpty) {
      required("-L", binFile)
    } else if (binSize.isDefined && binFile.isEmpty) {
      required("-B", binSize)
    } else {
      throw new IllegalStateException("bin_file *or* bin_size must be defined")
    }
  }

  def versionCommand = executable + " --version"
  def versionRegex = """.+, version (.*)""".r

}
