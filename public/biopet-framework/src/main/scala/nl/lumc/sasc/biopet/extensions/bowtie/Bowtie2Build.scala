package nl.lumc.sasc.biopet.extensions.bowtie

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Argument, Input }

/**
 * Created by pjvan_thof on 8/15/15.
 */
class Bowtie2Build(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(required = true)
  var reference: File = _

  @Output
  var outputFiles: List[File] = Nil

  @Argument(required = true)
  var baseName: String = _

  executable = config("exe", default = "bowtie2-build", freeVar = false)
  override def versionRegex = """.*[Vv]ersion:? (\d*\.\d*\.\d*)""".r
  override def versionCommand = executable + " --version"

  override def defaultCoreMemory = 15.0

  override def beforeGraph: Unit = {
    outputFiles ::= new File(reference.getParentFile, baseName + ".1.ebwt")
    outputFiles ::= new File(reference.getParentFile, baseName + ".2.ebwt")
  }

  def cmdLine = required("cd", reference.getParentFile) + "; " +
    required(executable) +
    required(reference) +
    required(baseName)
}
