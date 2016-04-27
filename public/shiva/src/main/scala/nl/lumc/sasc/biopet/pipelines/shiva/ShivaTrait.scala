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
package nl.lumc.sasc.biopet.pipelines.shiva

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.pipelines.bammetrics.TargetRegions
import nl.lumc.sasc.biopet.pipelines.mapping.MultisampleMappingTrait
import nl.lumc.sasc.biopet.pipelines.toucan.Toucan
import org.broadinstitute.gatk.queue.QScript

/**
 * This is a trait for the Shiva pipeline
 *
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaTrait extends MultisampleMappingTrait with Reference with TargetRegions { qscript: QScript =>

  override def reportClass: Option[ReportBuilderExtension] = {
    val shiva = new ShivaReport(this)
    shiva.outputDir = new File(outputDir, "report")
    shiva.summaryFile = summaryFile
    Some(shiva)
  }

  override def defaults = Map(
    "haplotypecaller" -> Map("stand_call_conf" -> 30, "stand_emit_conf" -> 0),
    "genotypegvcfs" -> Map("stand_call_conf" -> 30, "stand_emit_conf" -> 0),
    "unifiedgenotyper" -> Map("stand_call_conf" -> 30, "stand_emit_conf" -> 0)
  )

  /** Method to make the variantcalling namespace of shiva */
  def makeVariantcalling(multisample: Boolean = false): ShivaVariantcalling with QScript = {
    if (multisample) new ShivaVariantcalling(qscript) {
      override def namePrefix = "multisample"
      override def configNamespace: String = "shivavariantcalling"
      override def configPath: List[String] = super.configPath ::: "multisample" :: Nil
    }
    else new ShivaVariantcalling(qscript) {
      override def configNamespace = "shivavariantcalling"
    }
  }

  /** Method to make a sample */
  override def makeSample(id: String) = new this.Sample(id)

  /** Class that will generate jobs for a sample */
  class Sample(sampleId: String) extends super.Sample(sampleId) {
    /** Method to make a library */
    override def makeLibrary(id: String) = new this.Library(id)

    /** Sample specific settings */
    override def summarySettings = Map("single_sample_variantcalling" -> variantcalling.isDefined)

    /** Class to generate jobs for a library */
    class Library(libId: String) extends super.Library(libId) {
      /** Library specific settings */
      override def summarySettings = Map("library_variantcalling" -> variantcalling.isDefined)

      lazy val variantcalling = if (config("library_variantcalling", default = false).asBoolean &&
        (bamFile.isDefined || preProcessBam.isDefined)) {
        Some(makeVariantcalling(multisample = false))
      } else None

      /** This will add jobs for this library */
      override def addJobs() = {
        super.addJobs()

        variantcalling.foreach(vc => {
          vc.sampleId = Some(sampleId)
          vc.libId = Some(libId)
          vc.outputDir = new File(libDir, "variantcalling")
          if (preProcessBam.isDefined) vc.inputBams = Map(sampleId -> preProcessBam.get)
          else vc.inputBams = Map(sampleId -> bamFile.get)
          add(vc)
        })
      }
    }

    lazy val variantcalling = if (config("single_sample_variantcalling", default = false).asBoolean) {
      Some(makeVariantcalling(multisample = false))
    } else None

    /** This will add sample jobs */
    override def addJobs(): Unit = {
      super.addJobs()

      preProcessBam.foreach { bam =>
        variantcalling.foreach(vc => {
          vc.sampleId = Some(sampleId)
          vc.outputDir = new File(sampleDir, "variantcalling")
          vc.inputBams = Map(sampleId -> bam)
          add(vc)
        })
      }
    }
  }

  lazy val multisampleVariantCalling = if (config("multisample_variantcalling", default = true).asBoolean) {
    Some(makeVariantcalling(multisample = true))
  } else None

  lazy val svCalling = if (config("sv_calling", default = false).asBoolean) {
    Some(new ShivaSvCalling(this))
  } else None

  lazy val annotation = if (multisampleVariantCalling.isDefined &&
    config("annotation", default = false).asBoolean) {
    Some(new Toucan(this))
  } else None

  /** This will add the mutisample variantcalling */
  override def addMultiSampleJobs() = {
    super.addMultiSampleJobs()

    multisampleVariantCalling.foreach(vc => {
      vc.outputDir = new File(outputDir, "variantcalling")
      vc.inputBams = samples.flatMap { case (sampleId, sample) => sample.preProcessBam.map(sampleId -> _) }
      add(vc)

      annotation.foreach { toucan =>
        toucan.outputDir = new File(outputDir, "annotation")
        toucan.inputVCF = vc.finalFile
        add(toucan)
      }
    })

    svCalling.foreach(sv => {
      sv.outputDir = new File(outputDir, "sv_calling")
      sv.inputBams = samples.flatMap { case (sampleId, sample) => sample.preProcessBam.map(sampleId -> _) }
      add(sv)
    })
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, "Shiva.summary.json")

  /** Settings of pipeline for summary */
  override def summarySettings = super.summarySettings ++ Map(
    "annotation" -> annotation.isDefined,
    "multisample_variantcalling" -> multisampleVariantCalling.isDefined,
    "sv_calling" -> svCalling.isDefined,
    "regions_of_interest" -> roiBedFiles.map(_.getName.stripSuffix(".bed")),
    "amplicon_bed" -> ampliconBedFile.map(_.getName.stripSuffix(".bed"))
  )
}
