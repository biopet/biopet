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
package nl.lumc.sasc.biopet.extensions.bedtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.File

class BedtoolsCoverage(val root: Configurable) extends Bedtools {
  @Input(doc = "Input file (bed/gff/vcf/bam)")
  var input: File = _

  @Input(doc = "Intersect file (bed/gff/vcf)")
  var intersectFile: File = _

  @Output(doc = "output File")
  var output: File = _

  @Argument(doc = "dept", required = false)
  var depth: Boolean = false

  @Argument(doc = "sameStrand", required = false)
  var sameStrand: Boolean = false

  @Argument(doc = "diffStrand", required = false)
  var diffStrand: Boolean = false

  var inputTag = "-a"

  override def beforeCmd {
    if (input.getName.endsWith(".bam")) inputTag = "-abam"
  }

  def cmdLine = required(executable) + required("coverage") +
    required(inputTag, input) +
    required("-b", intersectFile) +
    conditional(depth, "-d") +
    conditional(sameStrand, "-s") +
    conditional(diffStrand, "-S") +
    " > " + required(output)
}

object BedtoolsCoverage {
  def apply(root: Configurable, input: File, intersect: File, output: File,
            depth: Boolean = true, sameStrand: Boolean = false, diffStrand: Boolean = false): BedtoolsCoverage = {
    val bedtoolsCoverage = new BedtoolsCoverage(root)
    bedtoolsCoverage.input = input
    bedtoolsCoverage.intersectFile = intersect
    bedtoolsCoverage.output = output
    bedtoolsCoverage.depth = depth
    bedtoolsCoverage.sameStrand = sameStrand
    bedtoolsCoverage.diffStrand = diffStrand
    return bedtoolsCoverage
  }
}