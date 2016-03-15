package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.extensions.{ Ln, Cufflinks }
import nl.lumc.sasc.biopet.extensions.tools.MergeTables
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 20/01/16.
 */
trait CufflinksMeasurement extends QScript with Measurement {
  def makeCufflinksJob(id: String, bamFile: File) = {
    val cufflinks = new Cufflinks(this)
    cufflinks.input = bamFile
    cufflinks.outputDir = new File(outputDir, id)
    cufflinks
  }

  def biopetScript(): Unit = {
    val jobs = bamFiles.map {
      case (id, file) =>
        val cufflinks = makeCufflinksJob(id, file)
        add(cufflinks)
        id -> cufflinks
    }

    val genesFpkmFiles = jobs.toList.map {
      case (id, job) =>
        val file = new File(job.outputDir, s"$id.genes_fpkm.counts")
        add(Ln(this, job.outputGenesFpkm, file))
        file
    }

    val isoFormFpkmFiles = jobs.toList.map {
      case (id, job) =>
        val file = new File(job.outputDir, s"$id.iso_form_fpkn.counts")
        add(Ln(this, job.outputIsoformsFpkm, file))
        file
    }

    addMergeTableJob(genesFpkmFiles, mergeGenesFpkmTable, "genes_fpkm", ".genes_fpkm.counts")
    addMergeTableJob(isoFormFpkmFiles, mergeIsoFormFpkmTable, "iso_form_fpkn", ".iso_form_fpkn.counts")

    addHeatmapJob(mergeGenesFpkmTable, genesFpkmHeatmap, "genes_fpkm")
    addHeatmapJob(mergeIsoFormFpkmTable, isoFormFpkmHeatmap, "iso_form_fpkm")

    addSummaryJobs()
  }

  def mergeGenesFpkmTable: File = new File(outputDir, s"$name.genes.fpkm.tsv")
  def genesFpkmHeatmap: File = new File(outputDir, s"$name.genes.fpkm.png")

  def mergeIsoFormFpkmTable: File = new File(outputDir, s"$name.iso_form.fpkm.tsv")
  def isoFormFpkmHeatmap: File = new File(outputDir, s"$name.iso_form.fpkm.png")

  def mergeArgs = MergeArgs(List(1, 7), 10, numHeaderLines = 1, fallback = "0.0")

}
