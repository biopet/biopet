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
 * A dual licensing mode is applied. The source code within this project is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.{ Version, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

import scalaz.std.boolean.option

/**
 * extension for raxml
 * based on version 8.1.3
 */
class Raxml(val parent: Configurable) extends BiopetCommandLineFunction with Version {

  override def defaultThreads = 1
  def versionCommand = executable + " -v"
  def versionRegex = """.*version ([\w\.]*) .*""".r

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
  var w: File = null

  @Input(required = false)
  var t: Option[File] = _

  @Input(required = false)
  var z: Option[File] = _

  @Output(doc = "Output files", required = false)
  private var out: List[File] = Nil

  var noBfgs: Boolean = config("no_bfgs", default = false)

  var executableNonThreads: String = config("exe", default = "raxmlHPC")
  var executableThreads: Option[String] = config("exe_pthreads")

  /** Sets correct output files to job */
  override def beforeGraph() {
    require(w != null)
    executable = if (threads > 1 && executableThreads.isDefined) executableThreads.get else executableNonThreads
    super.beforeGraph()
    out :::= List(Some(getInfoFile), getBestTreeFile, getBootstrapFile, getBipartitionsFile).flatten
    f match {
      case "d" if b.isEmpty => for (t <- 0 until N.getOrElse(1)) {
        out +:= new File(w, "RAxML_log." + n + ".RUN." + t)
        out +:= new File(w, "RAxML_parsimonyTree." + n + ".RUN." + t)
        out +:= new File(w, "RAxML_result." + n + ".RUN." + t)
      }
      case "b" => out +:= new File(w, "RAxML_bipartitionsBranchLabels." + n)
      case _   =>
    }
  }

  /** Returns bestTree file */
  def getBestTreeFile = option(f == "d" && b.isEmpty, new File(w, "RAxML_bestTree." + n))

  /** Returns bootstrap file */
  def getBootstrapFile = option(f == "d" && b.isDefined, new File(w, "RAxML_bootstrap." + n))

  /** Returns bipartitions file */
  def getBipartitionsFile = option(f == "b", new File(w, "RAxML_bipartitions." + n))

  /** Returns info file */
  def getInfoFile = new File(w, "RAxML_info." + n)

  /** return commandline to execute */
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
    conditional(noBfgs, "--no-bfgs") +
    required("-T", threads)
}
