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
import nl.lumc.sasc.biopet.extensions.samtools.{SamtoolsFlagstat, SamtoolsView}
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.{AggrBaseCount, Hist2count}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class BiosBaseCounts(val root: Configurable) extends QScript with Measurement with AnnotationBed {

  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 1, fallback = "0")

  override def defaults = Map("hist2count" -> Map("column" -> 4))

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val countFiles = bamFiles.map {
      case (id, file) => id -> addBaseCounts(file, new File(outputDir, id), id, "non_stranded")
    }

    addMergeTableJob(countFiles.map(_._2._1).toList, new File(outputDir, "merge.gene.counts"), "bios_gene_base_counts", ".non_stranded.gene.counts")
    addMergeTableJob(countFiles.map(_._2._2).toList, new File(outputDir, "merge.exon.counts"), "bios_exon_base_counts", ".non_stranded.exon.counts")

    addHeatmapJob(new File(outputDir, "merge.gene.counts"), new File(outputDir, "merge.gene.png"), "bios_gene_base_counts")
    addHeatmapJob(new File(outputDir, "merge.exon.counts"), new File(outputDir, "merge.exon.png"), "bios_exon_base_counts")

    addSummaryJobs()
  }

  protected def addBaseCounts(bamFile: File,
                              outputDir: File,
                              sampleName: String,
                              name: String): (File, File) = {
    val rawOutputFile = new File(outputDir, s"$sampleName.$name.raw.counts")

    val bedtoolsCoverage = new BedtoolsCoverage(this)
    bedtoolsCoverage.hist = true
    bedtoolsCoverage.split = true
    bedtoolsCoverage.input = bamFile
    bedtoolsCoverage.intersectFile = new File("stdin")

    val hist2count = new Hist2count(this)

    bedtoolsCoverage.intersectFile = annotationBed
    add(bedtoolsCoverage | hist2count > rawOutputFile)

    val geneAggr = new AggrBaseCount(this)
    geneAggr.input = rawOutputFile
    geneAggr.output = new File(outputDir, s"$sampleName.$name.gene.counts")
    geneAggr.mode = "gene"
    geneAggr.inputLabel = sampleName
    add(geneAggr)

    val exonAggr = new AggrBaseCount(this)
    exonAggr.input = rawOutputFile
    exonAggr.output = new File(outputDir, s"$sampleName.$name.exon.counts")
    exonAggr.mode = "exon"
    exonAggr.inputLabel = sampleName
    add(exonAggr)

    val samtoolsView = new SamtoolsView(this)
    samtoolsView.input = bamFile
    samtoolsView.L = Some(annotationBed)
    val samtoolsFlagstat = new SamtoolsFlagstat(this)
    samtoolsFlagstat.output = new File(outputDir, s"$sampleName.$name.exon.flagstats")
    add(samtoolsView | samtoolsFlagstat)

    (geneAggr.output, exonAggr.output)
  }
}
