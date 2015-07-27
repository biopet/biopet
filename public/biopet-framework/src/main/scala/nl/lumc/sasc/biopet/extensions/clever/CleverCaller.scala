package nl.lumc.sasc.biopet.extensions.clever

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class CleverCaller(val root: Configurable) extends BiopetCommandLineFunction with Reference {
  executable = config("exe", default = "clever")

  private lazy val versionexecutable: File = config("version_exe", default = new File(executable).getParent + "/ctk-version")

  override def defaultThreads = 8

  override def versionCommand = versionexecutable.getAbsolutePath
  override def versionRegex = """(.*)""".r
  override def versionExitcode = List(0, 1)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference")
  var reference: File = _

  var workDir: File = _
  var cwd: File = _

  @Output(doc = "Clever VCF output")
  lazy val outputvcf: File = {
    new File(cwd + "predictions.vcf")
  }

  @Output(doc = "Clever raw output")
  lazy val outputraw: File = {
    new File(workDir + "predictions.raw.txt")
  }

  //  var T: Option[Int] = config("T", default = defaultThreads)
  var f: Boolean = config("f", default = true) // delete work directory before running
  //  var w: String = config("w", default = workdir + "/work")
  var a: Boolean = config("a", default = false) // don't recompute AS tags
  var k: Boolean = config("k", default = false) // keep working directory
  var r: Boolean = config("r", default = false) // take read groups into account

  override def beforeGraph() {
    super.beforeGraph()
    if (workDir == null) throw new Exception("Clever :: Workdirectory is not defined")
    if (reference == null) reference = referenceFasta()
  }

  def cmdLine = required(executable) +
    " --sorted " +
    " --use_xa " +
    optional("-T", nCoresRequest) +
    conditional(f, "-f") +
    conditional(a, "-a") +
    conditional(k, "-k") +
    conditional(r, "-r") +
    required(input) +
    required(reference) +
    required(workDir)
}

object CleverCaller {
  def apply(root: Configurable, input: File, svDir: File, runDir: File): CleverCaller = {
    val clever = new CleverCaller(root)
    clever.input = input
    clever.cwd = svDir
    clever.workDir = runDir
    clever
  }
}