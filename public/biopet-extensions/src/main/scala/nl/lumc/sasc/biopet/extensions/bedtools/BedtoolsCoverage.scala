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

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/** Extension for bedtools coverage */
class BedtoolsCoverage(val root: Configurable) extends Bedtools {

  @Input(doc = "Input file (bed/gff/vcf/bam)")
  var input: File = null

  @Input(doc = "Intersect file (bed/gff/vcf)")
  var intersectFile: File = null

  @Output(doc = "output File")
  var output: File = null

  var depth: Boolean = false
  var sameStrand: Boolean = false
  var diffStrand: Boolean = false
  var split: Boolean = false
  var hist: Boolean = false

  override def defaultCoreMemory = 4.0

  /** Returns command to execute */
  def cmdLine = required(executable) + required("coverage") +
    required("-a", input) +
    required("-b", intersectFile) +
    conditional(split, "-split") +
    conditional(hist, "-hist") +
    conditional(depth, "-d") +
    conditional(sameStrand, "-s") +
    conditional(diffStrand, "-S") +
    (if (outputAsStsout) "" else " > " + required(output))
}

object BedtoolsCoverage {
  /** Returns defaul bedtools coverage */
  def apply(root: Configurable, input: File, intersect: File, output: Option[File] = None,
            depth: Boolean = false, sameStrand: Boolean = false, diffStrand: Boolean = false): BedtoolsCoverage = {
    val bedtoolsCoverage = new BedtoolsCoverage(root)
    bedtoolsCoverage.input = input
    bedtoolsCoverage.intersectFile = intersect
    output.foreach(bedtoolsCoverage.output = _)
    bedtoolsCoverage.depth = depth
    bedtoolsCoverage.sameStrand = sameStrand
    bedtoolsCoverage.diffStrand = diffStrand
    bedtoolsCoverage
  }
}