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
package nl.lumc.sasc.biopet.pipelines.gentrap.measures


import nl.lumc.sasc.biopet.core.annotations.AnnotationBed
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.Hist2count
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class BiosBaseCounts(val root: Configurable) extends QScript with Measurement with AnnotationBed {

  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 0, fallback = "0")

  override def defaults = Map("hist2count" -> Map("column" -> 4))

  /** Pipeline itself */
  def biopetScript(): Unit = {
    bamFiles.map {
      case (id, file) => id -> addBaseCounts(file, new File(outputDir, id), id, "non_stranded")
    }

    addSummaryJobs()
  }

  protected def addBaseCounts(bamFile: File,
                              outputDir: File,
                              sampleName: String,
                              name: String): File = {
    val outputFile = new File(outputDir, s"$sampleName.$name.counts")

    val bedtoolsCoverage = new BedtoolsCoverage(this)
    bedtoolsCoverage.hist = true
    bedtoolsCoverage.split = true
    bedtoolsCoverage.input = bamFile
    bedtoolsCoverage.intersectFile = new File("stdin")

    val hist2count = new Hist2count(this)

    bedtoolsCoverage.intersectFile = annotationBed
    add(bedtoolsCoverage | hist2count > outputFile)

    outputFile
  }
}
