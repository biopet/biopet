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
package nl.lumc.sasc.biopet.pipelines.tinycap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationGff
import nl.lumc.sasc.biopet.extensions.HtseqCount
import nl.lumc.sasc.biopet.pipelines.gentrap.measures.Measurement
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by wyleung on 11-2-16.
 */
class FragmentsPerSmallRna(val root: Configurable) extends QScript with Measurement with AnnotationGff {
  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 1, fallback = "0")

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = bamFiles.map {
      case (id, file) =>
        // Do expression counting for miRNA and siRNA
        val job = new HtseqCount(this)
        job.inputAlignment = file
        job.inputAnnotation = annotationGff
        job.format = Option("bam")
        job.stranded = Option("yes")
        job.featuretype = Option("miRNA")
        job.idattr = Option("Name")
        job.output = new File(outputDir, s"$id.$name.counts")
        add(job)

        id -> job
    }

    addMergeTableJob(jobs.values.map(_.output).toList, mergedTable, "fragments_per_smallrna", s".$name.counts")
    addHeatmapJob(mergedTable, heatmap, "fragments_per_smallrna")

    addSummaryJobs()
  }

  def mergedTable = new File(outputDir, s"$name.fragments_per_smallrna.tsv")
  def heatmap = new File(outputDir, s"$name.fragments_per_smallrna.png")
}
