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
package nl.lumc.sasc.biopet.pipelines.shiva

import java.io.File

import nl.lumc.sasc.biopet.core.extensions.CheckValidateVcf
import nl.lumc.sasc.biopet.core.{PipelineCommand, Reference}
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.extensions.gatk._
import nl.lumc.sasc.biopet.extensions.gatk.gather.BqsrGather
import nl.lumc.sasc.biopet.extensions.tools.ValidateVcf
import nl.lumc.sasc.biopet.pipelines.bammetrics.TargetRegions
import nl.lumc.sasc.biopet.pipelines.kopisu.Kopisu
import nl.lumc.sasc.biopet.pipelines.mapping.{Mapping, MultisampleMappingTrait}
import nl.lumc.sasc.biopet.pipelines.shiva.variantcallers.somatic.TumorNormalPair
import nl.lumc.sasc.biopet.pipelines.toucan.Toucan
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.function.QFunction

/**
  * This is a trait for the Shiva pipeline
  *
  * Created by pjvan_thof on 2/26/15.
  */
class Shiva(val parent: Configurable)
    extends QScript
    with MultisampleMappingTrait
    with Reference
    with TargetRegions { qscript =>

  def this() = this(null)

  override def reportClass: Option[ReportBuilderExtension] = {
    val shiva = new ShivaReport(this)
    shiva.outputDir = new File(outputDir, "report")
    shiva.summaryDbFile = summaryDbFile
    Some(shiva)
  }

  lazy val usePrintReads: Boolean = config("use_printreads", default = true)

  /** Method to make the variantcalling namespace of shiva */
  def makeVariantcalling(multisample: Boolean,
                         sample: Option[String] = None,
                         library: Option[String] = None): ShivaVariantcalling with QScript = {
    if (multisample) new ShivaVariantcalling(qscript) {
      override def namePrefix = "multisample"
      override def configNamespace: String = "shivavariantcalling"
      override def configPath: List[String] = super.configPath ::: "multisample" :: Nil
      genders = samples.map { case (sampleName, s) => sampleName -> s.gender }

      //TODO: this needs changed when the sample/library refactoring is beeing done
      tumorSamples = samples
        .filter(_._2.sampleTags.get("type").contains("tumor"))
        .flatMap {
          case (tumorName, tumorSample) =>
            tumorSample.sampleTags.get("control") match {
              case Some(normal: String) =>
                if (!samples.contains(normal))
                  Logging.addError(s"Normal sample '$normal' does not exist")
                Some(TumorNormalPair(tumorName, normal))
              case _ =>
                Logging.addError(s"Control is missing for tumor sample '$tumorName'")
                None
            }
        }
        .toList
    } else
      new ShivaVariantcalling(qscript) {
        override def configNamespace = "shivavariantcalling"
        sampleId = sample
        libId = library
        genders = sample.map(x => x -> samples(x).gender).toMap
      }
  }

  /** Method to make a sample */
  override def makeSample(id: String) = new this.Sample(id)

  /** Class that will generate jobs for a sample */
  class Sample(sampleId: String) extends super.Sample(sampleId) {

    /** Method to make a library */
    override def makeLibrary(id: String) = new this.Library(id)

    /** Sample specific settings */
    override def summarySettings: Map[String, Any] =
      super.summarySettings ++
        Map(
          "single_sample_variantcalling" -> variantcalling.isDefined,
          "use_indel_realigner" -> useIndelRealigner
        )

    /** Class to generate jobs for a library */
    class Library(libId: String) extends super.Library(libId) {

      override def summaryFiles: Map[String, File] =
        super.summaryFiles ++
          variantcalling.map("final" -> _.finalFile) ++
          bqsrFile.map("baserecal" -> _) ++
          bqsrAfterFile.map("baserecal_after" -> _)

      lazy val useBaseRecalibration: Boolean = {
        val c: Boolean = config("use_base_recalibration", default = true)
        val br = new BaseRecalibrator(qscript)
        if (c && br.knownSites.isEmpty)
          logger.warn("No Known site found, skipping base recalibration, file: " + inputBam)
        c && br.knownSites.nonEmpty
      }
      lazy val useAnalyzeCovariates: Boolean =
        if (useBaseRecalibration) config("use_analyze_covariates", default = true) else false

      lazy val bqsrFile: Option[File] =
        if (useBaseRecalibration) Some(createFile("baserecal")) else None
      lazy val bqsrAfterFile: Option[File] =
        if (useAnalyzeCovariates) Some(createFile("baserecal.after")) else None

      override def keepFinalBamfile: Boolean =
        super.keepFinalBamfile && !useBaseRecalibration && !usePrintReads

      override def preProcessBam: Option[Mapping#File] =
        if (usePrintReads && useBaseRecalibration)
          bamFile.map(swapExt(libDir, _, ".bam", ".baserecal.bam"))
        else bamFile

      /** Library specific settings */
      override def summarySettings: Map[String, Any] = super.summarySettings ++ Map(
        "library_variantcalling" -> variantcalling.isDefined,
        "use_base_recalibration" -> useBaseRecalibration,
        "useAnalyze_covariates" -> useAnalyzeCovariates
      )

      lazy val variantcalling: Option[ShivaVariantcalling with QScript] =
        if (config("library_variantcalling", default = false).asBoolean &&
            (bamFile.isDefined || preProcessBam.isDefined)) {
          Some(
            makeVariantcalling(multisample = false,
                               sample = Some(sampleId),
                               library = Some(libId)))
        } else None

      /** This will add jobs for this library */
      override def addJobs(): Unit = {
        super.addJobs()

        if (useBaseRecalibration) {
          addBaseRecalibrator(bamFile.get,
                              libDir,
                              useIndelRealigner || libraries.size > 1,
                              usePrintReads)
        }

        variantcalling.foreach(vc => {
          vc.sampleId = Some(sampleId)
          vc.libId = Some(libId)
          vc.outputDir = new File(libDir, "variantcalling")
          if (preProcessBam.isDefined) vc.inputBams = Map(sampleId -> preProcessBam.get)
          else vc.inputBams = Map(sampleId -> bamFile.get)
          add(vc)
        })
      }

      /** Adds base recalibration jobs */
      def addBaseRecalibrator(inputBam: File,
                              dir: File,
                              isIntermediate: Boolean,
                              usePrintreads: Boolean): File = {
        require(bqsrFile.isDefined, "bqsrFile should contain something at this point")
        val baseRecalibrator = BaseRecalibrator(qscript, inputBam, bqsrFile.get) // at this point bqsrFile should exist

        if (baseRecalibrator.knownSites.isEmpty) return inputBam
        add(baseRecalibrator)

        if (useAnalyzeCovariates) {
          val baseRecalibratorAfter = BaseRecalibrator(qscript, inputBam, bqsrAfterFile.get)
          baseRecalibratorAfter.BQSR = bqsrFile
          add(baseRecalibratorAfter)
          add(
            AnalyzeCovariates(qscript,
                              baseRecalibrator.out,
                              baseRecalibratorAfter.out,
                              swapExt(dir, inputBam, ".bam", ".baserecal.pdf")))
        }
        if (usePrintreads) {
          val printReads =
            PrintReads(qscript, inputBam, swapExt(dir, inputBam, ".bam", ".baserecal.bam"))
          printReads.BQSR = Some(baseRecalibrator.out)
          printReads.isIntermediate = isIntermediate
          add(printReads)
          printReads.out
        } else inputBam
      }

    } // end of library

    lazy val bqsrFile: Option[File] = {
      val files = libraries.flatMap(_._2.bqsrFile).toList
      if (files.isEmpty) None
      else {
        val gather = new BqsrGather
        gather.inputBqsrFiles = files
        gather.outputBqsrFile = createFile("baserecal")
        add(gather)
        Some(gather.outputBqsrFile)
      }
    }

    lazy val variantcalling: Option[ShivaVariantcalling with QScript] =
      if (config("single_sample_variantcalling", default = false).asBoolean) {
        Some(makeVariantcalling(multisample = false, sample = Some(sampleId)))
      } else None

    override def keepMergedFiles: Boolean =
      config("keep_merged_files", default = !useIndelRealigner)

    lazy val useIndelRealigner: Boolean = config("use_indel_realigner", default = true)

    override def preProcessBam: Option[File] =
      if (useIndelRealigner) {
        bamFile.map(swapExt(sampleDir, _, ".bam", ".realign.bam"))
      } else bamFile

    override def summaryFiles: Map[String, File] =
      super.summaryFiles ++ variantcalling.map("final" -> _.finalFile)

    /** This will add sample jobs */
    override def addJobs(): Unit = {
      super.addJobs()

      if (useIndelRealigner) {
        addIndelRealign(bamFile.get, sampleDir, isIntermediate = false)
      }

      preProcessBam.foreach { bam =>
        variantcalling.foreach(vc => {
          vc.sampleId = Some(sampleId)
          vc.outputDir = new File(sampleDir, "variantcalling")
          vc.inputBams = Map(sampleId -> bam)
          add(vc)
        })
      }
    }
  } // End of sample

  lazy val multisampleVariantCalling: Option[ShivaVariantcalling with QScript] =
    if (config("multisample_variantcalling", default = true).asBoolean) {
      Some(makeVariantcalling(multisample = true))
    } else None

  lazy val svCalling: Option[ShivaSvCalling] =
    if (config("sv_calling", default = false).asBoolean) {
      Some(new ShivaSvCalling(this))
    } else None

  lazy val cnvCalling: Option[Kopisu] = if (config("cnv_calling", default = false).asBoolean) {
    Some(new Kopisu(this))
  } else None

  lazy val annotation: Option[Toucan] =
    if (multisampleVariantCalling.isDefined &&
        config("annotation", default = false).asBoolean) {
      Some(new Toucan(this))
    } else None

  /** This will add the mutisample variantcalling */
  override def addMultiSampleJobs(): Unit = {
    super.addMultiSampleJobs()

    addAll(
      dbsnpVcfFile
        .map(
          Shiva.makeValidateVcfJobs(this, _, referenceFasta(), new File(outputDir, ".validate")))
        .getOrElse(Nil))

    multisampleVariantCalling.foreach(vc => {
      vc.outputDir = new File(outputDir, "variantcalling")
      vc.inputBams = samples.flatMap {
        case (sampleId, sample) => sample.preProcessBam.map(sampleId -> _)
      }
      if (!usePrintReads)
        vc.inputBqsrFiles = samples.flatMap {
          case (sampleId, sample) => sample.bqsrFile.map(sampleId -> _)
        }
      add(vc)
      if (!usePrintReads) {
        import variantcallers._
        if (vc.callers.exists {
              case _: HaplotypeCaller | _: HaplotypeCallerAllele | _: HaplotypeCallerGvcf => false
              case _: UnifiedGenotyper | _: UnifiedGenotyperAllele => false
              case _ => true
            }) logger.warn("Not all variantcallers chosen can read BQSR files, All non-GATK")
      }

      annotation.foreach { toucan =>
        toucan.outputDir = new File(outputDir, "annotation")
        toucan.inputVcf = vc.finalFile
        add(toucan)
      }
    })

    svCalling.foreach { sv =>
      sv.outputDir = new File(outputDir, "sv_calling")
      sv.inputBams = samples.flatMap {
        case (sampleId, sample) => sample.preProcessBam.map(sampleId -> _)
      }
      add(sv)
    }

    cnvCalling.foreach { cnv =>
      cnv.outputDir = new File(outputDir, "cnv_calling")
      cnv.inputBams = samples.flatMap {
        case (sampleId, sample) => sample.preProcessBam.map(sampleId -> _)
      }
      add(cnv)
    }
  }

  /** Settings of pipeline for summary */
  override def summarySettings: Map[String, Any] = super.summarySettings ++ Map(
    "annotation" -> annotation.isDefined,
    "multisample_variantcalling" -> multisampleVariantCalling.isDefined,
    "sv_calling" -> svCalling.isDefined,
    "cnv_calling" -> cnvCalling.isDefined,
    "regions_of_interest" -> roiBedFiles.map(_.getName.stripSuffix(".bed")),
    "amplicon_bed" -> ampliconBedFile.map(_.getName.stripSuffix(".bed")),
    "use_print_reads" -> usePrintReads
  )

  /** Adds indel realignment jobs */
  def addIndelRealign(inputBam: File, dir: File, isIntermediate: Boolean): File = {
    val realignerTargetCreator = RealignerTargetCreator(this, inputBam, dir)
    realignerTargetCreator.isIntermediate = true
    add(realignerTargetCreator)

    val indelRealigner = IndelRealigner(this, inputBam, realignerTargetCreator.out, dir)
    indelRealigner.isIntermediate = isIntermediate
    add(indelRealigner)

    indelRealigner.out
  }

}

/** This object give a default main method to the pipelines */
object Shiva extends PipelineCommand {

  // This is used to only execute 1 validation per vcf file
  private var validateVcfSeen: Set[(File, File)] = Set()

  def makeValidateVcfJobs(root: Configurable,
                          vcfFile: File,
                          referenceFile: File,
                          outputDir: File): List[QFunction] = {
    if (validateVcfSeen.contains((vcfFile, referenceFile))) Nil
    else {
      validateVcfSeen ++= Set((vcfFile, referenceFile))
      val validateVcf = new ValidateVcf(root)
      validateVcf.inputVcf = vcfFile
      validateVcf.reference = referenceFile
      validateVcf.jobOutputFile = new File(outputDir, vcfFile.getAbsolutePath + ".validateVcf.out")

      val checkValidateVcf = new CheckValidateVcf(root)
      checkValidateVcf.inputLogFile = validateVcf.jobOutputFile
      checkValidateVcf.jobOutputFile =
        new File(outputDir, vcfFile.getAbsolutePath + ".checkValidateVcf.out")

      List(validateVcf, checkValidateVcf)
    }
  }
}
