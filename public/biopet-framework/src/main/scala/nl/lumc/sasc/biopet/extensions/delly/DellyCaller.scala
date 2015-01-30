package nl.lumc.sasc.biopet.extensions.delly

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable

class DellyCaller(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "delly")

  private lazy val versionexecutable: File = new File(executable)

  override val defaultVmem = "8G"
  override val defaultThreads = 1

  override def versionCommand = versionexecutable.getAbsolutePath
  override val versionRegex = """DELLY \(Version: (.*)\)""".r
  override val versionExitcode = List(0, 1)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Output(doc = "Delly VCF output")
  var outputvcf: File = _

  @Argument(doc = "What kind of analysis to run: DEL,DUP,INV,TRA")
  var analysistype: String = _

  def cmdLine = required(executable) +
    "-t" + required(analysistype) +
    "-o" + required(outputvcf) +
    required(input)

}
