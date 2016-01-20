package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.extensions.Cufflinks
import nl.lumc.sasc.biopet.extensions.tools.MergeTables
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 20/01/16.
 */
trait CufflinksMeasurement extends QScript with Measurement {
  def makeCufflinksJob(id: String, bamFile: File) = {
    val cufflinks = new Cufflinks(this)
    cufflinks.input = bamFile
    cufflinks.output_dir = new File(outputDir, id)
    cufflinks
  }

  private var isoFormFiles: List[File] = Nil

  def bamToCountFile(id: String, bamFile: File): (String, File) = {
    val cufflinks = makeCufflinksJob(id, bamFile)
    add(cufflinks)

    isoFormFiles :+= cufflinks.outputIsoformsFpkm

    id -> cufflinks.outputGenesFpkm
  }

  override def biopetScript(): Unit = {
    super.biopetScript()

    add(MergeTables(this, isoFormFiles, mergeIsoFormTable,
      mergeArgs.idCols, mergeArgs.valCol, mergeArgs.numHeaderLines, mergeArgs.fallback))
  }

  def mergeIsoFormTable: File = new File(outputDir, s"$name.iso_form_table.tsv")

  def mergeArgs = MergeArgs(List(1, 7), 10, numHeaderLines = 1, fallback = "0.0")

}
