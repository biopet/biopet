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
package nl.lumc.sasc.biopet.extensions.hisat

import java.io.File

import nl.lumc.sasc.biopet.core.{BiopetCommandLineFunction, Reference, Version}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Input

/**
  * Created by pjvan_thof on 7-6-16.
  */
class Hisat2Build(val parent: Configurable) extends BiopetCommandLineFunction with Version {

  @Input(required = true)
  var inputFasta: File = _

  var hisat2IndexBase: String = _

  @Input(required = false)
  var snp: Option[File] = config("snp")

  @Input(required = false)
  var haplotype: Option[File] = config("haplotype")

  @Input(required = false)
  var ss: Option[File] = config("ss")

  @Input(required = false)
  var exon: Option[File] = config("exon")

  executable = config("exe", default = "hisat2-build", freeVar = false)
  def versionRegex = """.*hisat.*version (.*)""".r
  def versionCommand = executable + " --version"

  var bmax: Option[Int] = config("bmax")
  var bmaxdivn: Option[Int] = config("bmaxdivn")
  var dcv: Option[Int] = config("dcv")
  var offrate: Option[Int] = config("offrate")
  var ftabchars: Option[Int] = config("ftabchars")
  var localoffrate: Option[Int] = config("localoffrate")
  var localftabchars: Option[Int] = config("localftabchars")
  var seed: Option[Int] = config("seed")

  var largeIndex: Boolean = config("large_index", default = false)
  var memoryFitting: Boolean = config("memory_fitting", default = false)
  var nodc: Boolean = config("nodc", default = false)
  var noref: Boolean = config("noref", default = false)
  var justref: Boolean = config("justref", default = false)
  var quiet: Boolean = config("quiet", default = false)

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    val indexDir = new File(hisat2IndexBase).getParentFile
    val indexName = new File(hisat2IndexBase).getName
    jobOutputFile = new File(indexDir, s".$indexName.hisat2-build.out")
  }

  def cmdLine =
    required(executable) +
      optional("-p", threads) +
      optional("--bmax", bmax) +
      optional("--bmaxdivn", bmaxdivn) +
      optional("--dcv", dcv) +
      optional("--offrate", offrate) +
      optional("--ftabchars", ftabchars) +
      optional("--localoffrate", localoffrate) +
      optional("--localftabchars", localftabchars) +
      optional("--seed", seed) +
      optional("--snp", snp) +
      optional("--haplotype", haplotype) +
      optional("--ss", ss) +
      optional("--exon", exon) +
      conditional(largeIndex, "--large-index") +
      conditional(memoryFitting, "--memory-fitting") +
      conditional(nodc, "--nodc") +
      conditional(noref, "--noref") +
      conditional(justref, "--justref") +
      conditional(quiet, "--quiet") +
      required(inputFasta) +
      required(hisat2IndexBase)

}
