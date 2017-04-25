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
package nl.lumc.sasc.biopet.extensions.bedtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/** Extension for bedtools intersect */
class BedtoolsIntersect(val parent: Configurable) extends Bedtools {

  @Input(doc = "Input file (bed/gff/vcf/bam)")
  var input: File = _

  @Input(doc = "Intersect file (bed/gff/vcf)")
  var intersectFile: File = _

  @Output(doc = "output File")
  var output: File = _

  @Argument(doc = "Min overlap", required = false)
  var minOverlap: Option[Double] = config("minoverlap")

  @Argument(doc = "Only count", required = false)
  var count: Boolean = false

  var inputTag = "-a"

  var ubam = false

  override def beforeCmd() {
    if (input.getName.endsWith(".bam")) inputTag = "-abam"
  }

  /** Returns command to execute */
  def cmdLine: String = required(executable) +
    required("intersect") +
    required(inputTag, input) +
    required("-b", intersectFile) +
    optional("-f", minOverlap) +
    conditional(count, "-c") +
    conditional(ubam, "-ubam") +
    " > " + required(output)
}

object BedtoolsIntersect {
  /** Returns default bedtools intersect */
  def apply(root: Configurable, input: File, intersect: File, output: File,
            minOverlap: Option[Double] = None, count: Boolean = false): BedtoolsIntersect = {
    val bedtoolsIntersect = new BedtoolsIntersect(root)
    bedtoolsIntersect.input = input
    bedtoolsIntersect.intersectFile = intersect
    bedtoolsIntersect.output = output
    if (output.getName.endsWith(".sam")) bedtoolsIntersect.ubam = true
    bedtoolsIntersect.minOverlap = minOverlap
    bedtoolsIntersect.count = count
    bedtoolsIntersect
  }
}