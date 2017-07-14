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
package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.qiime._
import nl.lumc.sasc.biopet.extensions.seqtk.SeqtkSample
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 12/4/15.
  */
class GearsQiimeOpen(val parent: Configurable)
    extends QScript
    with SummaryQScript
    with SampleLibraryTag {

  var fastqInput: File = _

  override def defaults = Map(
    "splitlibrariesfastq" -> Map(
      "barcode_type" -> "not-barcoded"
    )
  )

  def init(): Unit = {
    require(fastqInput != null)
    require(sampleId.isDefined)
  }

  private var _otuMap: File = _
  def otuMap: File = _otuMap

  private var _otuTable: File = _
  def otuTable: File = _otuTable

  def biopetScript(): Unit = {

    val splitLib = new SplitLibrariesFastq(this)
    splitLib.input :+= fastqInput
    splitLib.outputDir = new File(outputDir, "split_libraries_fastq")
    sampleId.foreach(splitLib.sampleIds :+= _.replaceAll("_", "-"))
    splitLib.isIntermediate = true
    add(splitLib)

    val openReference = new PickOpenReferenceOtus(this)
    openReference.inputFasta = addDownsample(
      splitLib.outputSeqs,
      new File(splitLib.outputDir, s"${sampleId.get}.downsample.fna"))
    openReference.outputDir = new File(outputDir, "pick_open_reference_otus")
    add(openReference)
    _otuMap = openReference.otuMap
    _otuTable = openReference.otuTable

    addSummaryJobs()
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map("otu_table" -> otuTable, "otu_map" -> otuMap)

  val downSample: Option[Double] = config("downsample")

  def addDownsample(input: File, output: File): File = {
    downSample match {
      case Some(x) =>
        val seqtk = new SeqtkSample(this)
        seqtk.input = input
        seqtk.sample = x
        seqtk.output = output
        add(seqtk)
        output
      case _ => input
    }
  }
}
