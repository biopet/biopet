package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

class Raxml(val root: Configurable) extends BiopetCommandLineFunction {

  override val defaultThreads = 1
  override def versionCommand = executable + " -v"
  override val versionRegex = """.*version ([\w\.]*) .*""".r

  @Input(doc = "Input phy/fasta file", required = true)
  var input: File = _

  @Argument(doc = "Model", required = true)
  var m: String = _

  @Argument(doc = "Parsimony seed", required = false)
  var p: Option[Int] = config("p")

  @Argument(doc = "Bootstrapping seed", required = false)
  var b: Option[Int] = config("b")

  @Argument(doc = "Number of runs", required = false)
  var N: Option[Int] = config("N")

  @Argument(doc = "Name of output files", required = true)
  var n: String = _

  @Argument(doc = "Name of output files", required = true)
  var f: String = "d"

  @Argument(doc = "Output directory", required = true)
  var w: String = _

  @Input(required = false)
  var t: File = _

  @Input(required = false)
  var z: File = _

  @Output(doc = "Output files", required = false)
  private var out: List[File] = Nil

  var executableNonThreads: String = config("exe", default = "raxmlHPC")
  var executableThreads: String = config("exe_pthreads")

  override def afterGraph {
    if (threads == 0) threads = getThreads(defaultThreads)
    executable = if (threads > 1 && executableThreads != null) executableThreads else executableNonThreads
    super.afterGraph
    out +:= getInfoFile
    f match {
      case "d" if b.isEmpty => {
        out +:= getBestTreeFile
        for (t <- 0 until N.getOrElse(1)) {
          out +:= new File(w + File.separator + "RAxML_log." + n + ".RUN." + t)
          out +:= new File(w + File.separator + "RAxML_parsimonyTree." + n + ".RUN." + t)
          out +:= new File(w + File.separator + "RAxML_result." + n + ".RUN." + t)
        }
      }
      case "d" if b.isDefined => out +:= getBootstrapFile
      case "b" => {
        out +:= new File(w + File.separator + "RAxML_bipartitionsBranchLabels." + n)
        out +:= new File(w + File.separator + "RAxML_bipartitions." + n)
      }
      case _ =>
    }
  }

  def getBestTreeFile: File = new File(w + File.separator + "RAxML_bestTree." + n)
  def getBootstrapFile: File = new File(w + File.separator + "RAxML_bootstrap." + n)
  def getInfoFile: File = new File(w + File.separator + "RAxML_info." + n)

  def cmdLine = required(executable) +
    required("-m", m) +
    required("-s", input) +
    optional("-p", p) +
    optional("-b", b) +
    optional("-N", N) +
    optional("-n", n) +
    optional("-w", w) +
    optional("-f", f) +
    optional("-t", t) +
    optional("-z", z) +
    required("-T", threads)
}