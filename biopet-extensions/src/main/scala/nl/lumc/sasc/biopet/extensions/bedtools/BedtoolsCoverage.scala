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

import java.io.{ File, PrintWriter }

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

import scala.io.Source

/** Extension for bedtools coverage */
class BedtoolsCoverage(val parent: Configurable) extends Bedtools with Reference {

  @Input(doc = "Input file (bed/gff/vcf/bam)")
  var input: File = null

  @Input(doc = "Intersect file (bed/gff/vcf)")
  var intersectFile: File = null

  @Output(doc = "output File")
  var output: File = null

  @Argument(doc = "depth", required = false)
  var depth: Boolean = false

  @Argument(doc = "sameStrand", required = false)
  var sameStrand: Boolean = false

  @Argument(doc = "diffStrand", required = false)
  var diffStrand: Boolean = false

  @Argument(doc = "sorted", required = false)
  var sorted: Boolean = config("sorted", default = false, freeVar = false)

  override def defaultCoreMemory = 4.0

  /** Returns command to execute */
  def cmdLine = required(executable) + required("coverage") +
    required("-a", input) +
    required("-b", intersectFile) +
    conditional(depth, "-d") +
    conditional(sameStrand, "-s") +
    conditional(diffStrand, "-S") +
    conditional(sorted, "-sorted") +
    (if (sorted) required("-g", BedtoolsCoverage.getGenomeFile(referenceFai, jobTempDir)) else "") +
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

  private var genomeCache: Map[(File, File), File] = Map()

  def getGenomeFile(fai: File, dir: File): File = {
    if (!genomeCache.contains((fai, dir))) genomeCache += (fai, dir) -> createGenomeFile(fai, dir)
    genomeCache((fai, dir))
  }

  /**
   * Creates the genome file. i.e. the first two columns of the fasta index
   * @return
   */
  def createGenomeFile(fai: File, dir: File): File = {
    val tmp = File.createTempFile(fai.getName, ".genome", dir)
    tmp.deleteOnExit()
    val writer = new PrintWriter(tmp)
    Source.fromFile(fai).
      getLines().
      map(s => s.split("\t").take(2).mkString("\t")).
      foreach(f => writer.println(f))
    writer.close()
    tmp
  }
}