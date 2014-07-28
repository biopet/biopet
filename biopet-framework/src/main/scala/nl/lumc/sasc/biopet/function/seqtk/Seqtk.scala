/**
 * Copyright (c) 2014 Leiden University Medical Center
 *
 * @author  Wibowo Arindrarto
 */

package nl.lumc.sasc.biopet.function.seqtk

import java.io.File
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

/**
 * Abstract class for all seqtk wrappers.
 */
abstract class Seqtk extends BiopetCommandLineFunction {
  executable = config("exe", default = "seqtk", submodule = "seqtk")
  override def versionCommand = executable
  override val versionRegex = """Version: (.*)""".r
}
