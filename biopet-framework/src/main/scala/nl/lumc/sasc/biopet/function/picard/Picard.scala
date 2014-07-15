package nl.lumc.sasc.biopet.function.picard

import nl.lumc.sasc.biopet.core.BiopetJavaCommandLineFunction
import org.broadinstitute.gatk.utils.commandline.{Input, Output, Argument}

trait Picard extends BiopetJavaCommandLineFunction {
  @Argument(doc="VERBOSITY", required = false)
  var verbosity: String = config("verbosity", "INFO", "picard")
  
  @Argument(doc="QUIET", required = false)
  var quiet: Boolean = config("quiet", false, "picard")
  
  @Argument(doc="VALIDATION_STRINGENCY", required = false)
  var stringency: String = config("validationstringency", "STRICT", "picard")
  
  @Argument(doc="COMPRESSION_LEVEL", required = false)
  var compression: Int = config("compressionlevel", 5, "picard")
  
  @Argument(doc="MAX_RECORDS_IN_RAM", required = false)
  var maxRecordsInRam: Int = config("maxrecordsinram", 500000, "picard")
  
  @Argument(doc="CREATE_INDEX", required = false)
  var createIndex: Boolean = config("createindex", true, "picard")
  
  @Argument(doc="CREATE_MD5_FILE", required = false)
  var createMd5: Boolean = config("createmd5", false, "picard")
  
  override def versionCommand = executeble + " " + javaOpts + " " + javaExecutable + " -h"
  override val versionRegex = """Version: (.*)""".r
  override val versionExitcode = List(0,1)
  
  override val defaultVmem = "8G"
  memoryLimit = Option(5.0)
  
  override def commandLine = super.commandLine +
    required("TMP_DIR=" + jobTempDir) +
    optional("VERBOSITY=", verbosity, spaceSeparated=false) +
    conditional(quiet, "QUIET=TRUE") +
    optional("VALIDATION_STRINGENCY=", stringency, spaceSeparated=false) +
    optional("COMPRESSION_LEVEL=", compression, spaceSeparated=false) +
    optional("MAX_RECORDS_IN_RAM=", maxRecordsInRam, spaceSeparated=false) +
    conditional(createIndex, "CREATE_INDEX=TRUE") +
    conditional(createMd5, "CREATE_MD5_FILE=TRUE")
}
