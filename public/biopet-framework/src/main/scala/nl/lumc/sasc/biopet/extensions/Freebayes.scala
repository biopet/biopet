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

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, BiopetCommandLineFunction }
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by pjvan_thof on 3/3/15.
 */
class Freebayes(val root: Configurable) extends BiopetCommandLineFunction with Reference {

  @Input(required = true)
  var bamfiles: List[File] = Nil

  @Input(required = true)
  var reference: File = _

  @Output(required = true)
  var outputVcf: File = null

  var ploidy: Option[Int] = config("ploidy")
  var haplotypeLength: Option[Int] = config("haplotype_length")

  executable = config("exe", default = "freebayes")
  override val versionRegex = """version:  (.*)""".r
  override def versionCommand = executable + " --version"

  override def beforeGraph: Unit = {
    super.beforeGraph
    reference = referenceFasta()
  }

  def cmdLine = executable +
    required("--fasta-reference", reference) +
    repeat("--bam", bamfiles) +
    optional("--vcf", outputVcf) +
    optional("--ploidy", ploidy) +
    optional("--haplotype-length", haplotypeLength)
}
