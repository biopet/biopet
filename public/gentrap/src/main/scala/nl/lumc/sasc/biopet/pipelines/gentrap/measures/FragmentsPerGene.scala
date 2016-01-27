package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationGtf
import nl.lumc.sasc.biopet.extensions.HtseqCount
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class FragmentsPerGene(val root: Configurable) extends QScript with Measurement with AnnotationGtf {
  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 1, fallback = "0")

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = bamFiles.map {
      case (id, file) =>
        //TODO: ID sorting job

        val job = new HtseqCount(this)
        job.inputAnnotation = annotationGtf
        job.inputAlignment = file
        job.output = new File(outputDir, s"$id.$name.counts")
        job.format = Option("bam")
        add(job)
        // We are forcing the sort order to be ID-sorted, since HTSeq-count often chokes when using position-sorting due
        // to its buffer not being large enough.
        //TODO: ID sorting job
        //job.order = Option("name")
        id -> job
    }

    addMergeTableJob(jobs.values.map(_.output).toList, mergedTable, "fragments_per_gene", s".$name.counts")
    addHeatmapJob(mergedTable, heatmap, "fragments_per_gene")
  }

  def mergedTable = new File(outputDir, s"$name.fragments_per_gene.tsv")
  def heatmap = new File(outputDir, s"$name.fragments_per_gene.png")
}
