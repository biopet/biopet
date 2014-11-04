package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

class Raxml(val root: Configurable) extends BiopetCommandLineFunction {

  override val defaultThreads = 4
  override def versionCommand = executable + " -v"
  override val versionRegex = """.*version \w* .*""".r

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

  @Argument(doc = "Output directory", required = false)
  var w: String = jobLocalDir.getAbsolutePath

  @Input(required = false)
  var t: File = _

  @Input(required = false)
  var z: File = _

  @Output(doc = "Output files", required = false)
  private var out: List[File] = Nil

  executable = config("exe", default = "raxmlHPC")

  override def afterGraph {
    super.afterGraph
    f match {
      case "d" if b.isEmpty   => out +:= getBestTree
      case "d" if b.isDefined => out +:= getBootstrap
    }
  }

  def getBestTree: File = new File(w + File.separator + "RAxML_bestTree." + n)
  def getBootstrap: File = new File(w + File.separator + "RAxML_bootstrap." + n)

  def cmdLine = required(executable) +
    required("-m", m) +
    required("-s", input) +
    optional("-p", p) +
    optional("-b", b) +
    optional("-N", N) +
    optional("-w", w) +
    optional("-f", f) +
    optional("-t", t) +
    optional("-z", z) +
    (if (threads > 1) required("-T", threads) else "")
}