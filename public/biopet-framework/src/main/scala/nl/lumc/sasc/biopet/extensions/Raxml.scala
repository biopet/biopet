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
  var executableThreads: Option[String] = config("exe_pthreads")

  override def afterGraph {
    if (threads == 0) threads = getThreads(defaultThreads)
    executable = if (threads > 1 && executableThreads.isDefined) executableThreads.get else executableNonThreads
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
  def getBipartitionsFile: File = new File(w + File.separator + "RAxML_bipartitions." + n)
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
