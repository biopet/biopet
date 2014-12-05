/**
 * Biopet is built on top of GATK Queue for building bioinformatic
 * pipelines. It is mainly intended to support LUMC SHARK cluster which is running
 * SGE. But other types of HPC that are supported by GATK Queue (such as PBS)
 * should also be able to execute Biopet tools and pipelines.
 *
 * Copyright 2014 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Contact us at: sasc@lumc.nl
 *
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.picard

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{ Argument }

trait Picard extends BiopetJavaCommandLineFunction {
  @Argument(doc = "VERBOSITY", required = false)
  var verbosity: String = config("verbosity", submodule = "picard")

  @Argument(doc = "QUIET", required = false)
  var quiet: Boolean = config("quiet", default = false, submodule = "picard")

  @Argument(doc = "VALIDATION_STRINGENCY", required = false)
  var stringency: String = config("validationstringency", submodule = "picard")

  @Argument(doc = "COMPRESSION_LEVEL", required = false)
  var compression: Option[Int] = config("compressionlevel", submodule = "picard")

  @Argument(doc = "MAX_RECORDS_IN_RAM", required = false)
  var maxRecordsInRam: Option[Int] = config("maxrecordsinram", submodule = "picard")

  @Argument(doc = "CREATE_INDEX", required = false)
  var createIndex: Boolean = config("createindex", default = true, submodule = "picard")

  @Argument(doc = "CREATE_MD5_FILE", required = false)
  var createMd5: Boolean = config("createmd5", default = false, submodule = "picard")

  //  override def versionCommand = executable + " " + javaOpts + " " + javaExecutable + " -h"
  //  override val versionRegex = """Version: (.*)""".r
  //  override val versionExitcode = List(0, 1)

  override val defaultVmem = "8G"
  memoryLimit = Option(3.0)

  override def commandLine = super.commandLine +
    required("TMP_DIR=" + jobTempDir) +
    optional("VERBOSITY=", verbosity, spaceSeparated = false) +
    conditional(quiet, "QUIET=TRUE") +
    optional("VALIDATION_STRINGENCY=", stringency, spaceSeparated = false) +
    optional("COMPRESSION_LEVEL=", compression, spaceSeparated = false) +
    optional("MAX_RECORDS_IN_RAM=", maxRecordsInRam, spaceSeparated = false) +
    conditional(createIndex, "CREATE_INDEX=TRUE") +
    conditional(createMd5, "CREATE_MD5_FILE=TRUE")
}
