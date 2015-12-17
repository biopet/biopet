package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.{ BiopetQScript, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.Flash
import nl.lumc.sasc.biopet.extensions.qiime._
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 12/4/15.
 */
class GearsQiimeClosed(val root: Configurable) extends QScript with BiopetQScript with SampleLibraryTag {

  var fastqR1: File = _

  var fastqR2: Option[File] = None

  override def defaults = Map(
    "splitlibrariesfastq" -> Map(
      "barcode_type" -> "not-barcoded"
    )
  )

  def init() = {
    require(fastqR1 != null)
  }

  private var _otuMap: File = _
  def otuMap = _otuMap

  private var _otuTable: File = _
  def otuTable = _otuTable

  def biopetScript() = {

    val fastqFile = fastqR2 match {
      case Some(r2) =>
        val flash = new Flash(this)
        flash.outputDirectory = new File(outputDir, "combine_reads_flash")
        flash.fastqR1 = fastqR1
        flash.fastqR2 = r2
        add(flash)
        flash.combinedFastq
      case _ => fastqR1
    }

    val splitLib = new SplitLibrariesFastq(this)
    splitLib.input :+= fastqFile
    splitLib.outputDir = new File(outputDir, "split_libraries_fastq")
    sampleId.foreach(splitLib.sample_ids :+= _)
    add(splitLib)

    val closedReference = new PickClosedReferenceOtus(this)
    closedReference.inputFasta = splitLib.outputSeqs
    closedReference.outputDir = new File(outputDir, "pick_closed_reference_otus")
    add(closedReference)
    _otuMap = closedReference.otuMap
    _otuTable = closedReference.otuTable
  }
}
