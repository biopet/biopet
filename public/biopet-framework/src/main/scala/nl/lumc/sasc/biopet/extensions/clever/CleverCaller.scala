package nl.lumc.sasc.biopet.extensions.clever

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class CleverCaller(val root: Configurable) extends BiopetCommandLineFunction {
  executable = config("exe", default = "clever")

  private lazy val versionexecutable: File = config("version_exe", default = (new File(executable).getParent + "/ctk-version"))

  override val defaultVmem = "4G"
  override val defaultThreads = 8

  override def versionCommand = versionexecutable.getAbsolutePath
  override val versionRegex = """(.*)""".r
  override val versionExitcode = List(0, 1)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference")
  var reference: File = _

  @Argument(doc = "Work directory")
  var workdir: String = _

  var cwd: String = _

  @Output(doc = "Clever VCF output")
  lazy val outputvcf: File = {
    new File(cwd + "predictions.vcf")
  }

  @Output(doc = "Clever raw output")
  lazy val outputraw: File = {
    new File(workdir + "predictions.raw.txt")
  }

  //  var T: Option[Int] = config("T", default = defaultThreads)
  var f: Boolean = config("f", default = true) // delete work directory before running
  //  var w: String = config("w", default = workdir + "/work")
  var a: Boolean = config("a", default = false) // don't recompute AS tags
  var k: Boolean = config("k", default = false) // keep working directory
  var r: Boolean = config("r", default = false) // take read groups into account

  override def beforeCmd {
    if (workdir == null) throw new Exception("Clever :: Workdirectory is not defined")
    //    if (input.getName.endsWith(".sort.bam")) sorted = true
  }

  def cmdLine = required(executable) +
    " --sorted " +
    " --use_xa " +
    optional("-T", nCoresRequest) +
    conditional(f, "-f") +
    conditional(a, "-a") +
    conditional(k, "-k") +
    conditional(r, "-r") +
    required(this.input) +
    required(this.reference) +
    required(this.workdir)
}

object CleverCaller {
  def apply(root: Configurable, input: File, reference: File, svDir: String, runDir: String): CleverCaller = {
    val clever = new CleverCaller(root)
    clever.input = input
    clever.reference = reference
    clever.cwd = svDir
    clever.workdir = runDir
    return clever
  }
}