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
package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationGtf
import nl.lumc.sasc.biopet.extensions.stringtie.{StringtieMerge, Stringtie => StringtieTool}
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvan_thof on 1/12/16.
  */
class Stringtie(val parent: Configurable) extends QScript with Measurement with AnnotationGtf {
  def mergeArgs = MergeArgs(idCols = List(1), valCol = 2, fallback = "0")

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val sampleGtfFiles: List[File] = bamFiles.map {
      case (id, file) =>
        val sampleDir = new File(outputDir, id)
        val stringtie = new StringtieTool(this)
        stringtie.inputBam = file
        stringtie.l = Some(id)
        stringtie.referenceGtf = Some(annotationGtf)
        stringtie.outputGtf = new File(sampleDir, s"$id.gtf")
        stringtie.geneAbundances = Some(new File(sampleDir, s"$id.gene_abund.tab"))
        stringtie.referenceCoverage = Some(new File(sampleDir, s"$id.cov_refs.gtf"))
        add(stringtie)

        val stringtieCount = new StringtieTool(this)
        stringtieCount.inputBam = file
        stringtieCount.l = Some(id)
        stringtieCount.referenceGtf = Some(stringtieMergedTranscripts)
        stringtieCount.outputGtf = new File(sampleDir, s"$id.merged.gtf")
        stringtieCount.geneAbundances = Some(new File(sampleDir, s"$id.merged.gene_abund.tab"))
        stringtieCount.referenceCoverage = Some(new File(sampleDir, s"$id.merged.cov_refs.gtf"))
        stringtieCount.e = true
        add(stringtieCount)

        stringtie.outputGtf
    }.toList

    val stringtieMerge = new StringtieMerge(this)
    stringtieMerge.inputGtfs = sampleGtfFiles
    stringtieMerge.referenceGtf = Some(annotationGtf)
    stringtieMerge.outputGtf = stringtieMergedTranscripts
    add(stringtieMerge)

    addSummaryJobs()
  }

  def stringtieMergedTranscripts: File = new File(outputDir, "stringtie.merged.gtf")

  override def summaryFiles: Map[String, File] =
    super.summaryFiles ++ Map("annotation_gtf" -> annotationGtf,
                              "stringtie_merged" -> stringtieMergedTranscripts)
}
