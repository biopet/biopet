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
package nl.lumc.sasc.biopet.extensions.clever

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetCommandLineFunction, Reference, Version }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class CleverCaller(val root: Configurable) extends BiopetCommandLineFunction with Reference with Version {
  executable = config("exe", default = "clever")

  private lazy val versionExecutable: File = config("version_exe", default = new File(executable).getParent + "/ctk-version")

  override def defaultThreads = 8
  override def defaultCoreMemory = 3.0

  def versionCommand = versionExecutable.getAbsolutePath
  def versionRegex = """(.*)""".r
  override def versionExitcode = List(0, 1)

  @Input(doc = "Input file (bam)")
  var input: File = _

  @Input(doc = "Reference")
  var reference: File = _

  var cleverWorkDir: File = _

  @Output(doc = "Clever VCF output")
  lazy val outputvcf: File = {
    new File(cleverWorkDir, "predictions.vcf")
  }

  @Output(doc = "Clever raw output")
  lazy val outputraw: File = {
    new File(cleverWorkDir, "predictions.raw.txt")
  }

  //  var T: Option[Int] = config("T", default = defaultThreads)
  var f: Boolean = config("f", default = true) // delete work directory before running
  //  var w: String = config("w", default = workdir + "/work")
  var a: Boolean = config("a", default = false) // don't recompute AS tags
  var k: Boolean = config("k", default = false) // keep working directory
  var r: Boolean = config("r", default = false) // take read groups into account

  override def beforeGraph() {
    super.beforeGraph()
    if (cleverWorkDir == null) throw new Exception("Clever :: Workdirectory is not defined")
    if (reference == null) reference = referenceFasta()
  }

  def cmdLine = required(executable) +
    required("--sorted") +
    required("--use_xa") +
    optional("-T", threads) +
    conditional(f, "-f") +
    conditional(a, "-a") +
    conditional(k, "-k") +
    conditional(r, "-r") +
    required(input) +
    required(reference) +
    required(cleverWorkDir)
}

object CleverCaller {
  def apply(root: Configurable, input: File, svDir: File): CleverCaller = {
    val clever = new CleverCaller(root)
    clever.input = input
    clever.cleverWorkDir = svDir
    clever
  }
}