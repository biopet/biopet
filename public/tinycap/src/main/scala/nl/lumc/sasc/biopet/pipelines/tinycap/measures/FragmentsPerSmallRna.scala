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
