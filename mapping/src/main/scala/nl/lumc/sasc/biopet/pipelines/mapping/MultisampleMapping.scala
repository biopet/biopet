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
package nl.lumc.sasc.biopet.pipelines.mapping

import java.io.File

import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.reference.FastaSequenceFile
import nl.lumc.sasc.biopet.core.report.ReportBuilderExtension
import nl.lumc.sasc.biopet.core.{MultiSampleQScript, PipelineCommand, Reference}
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard._
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.pipelines.gears.GearsSingle
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript
import MultisampleMapping.MergeStrategy
import nl.lumc.sasc.biopet.extensions.sambamba.{SambambaMarkdup, SambambaMerge}

import scala.collection.JavaConversions._

/**
  * Created by pjvanthof on 18/12/15.
  *
  * This trait is meant to extend pipelines from that require a alignment step
  */
trait MultisampleMappingTrait extends MultiSampleQScript with Reference { qscript: QScript =>

  /** With this method the merge strategy for libraries to samples is defined. This can be overriden to fix the merge strategy. */
  def mergeStrategy: MergeStrategy.Value = {
    val value: String = config("merge_strategy", default = "preprocessmarkduplicates")
    MergeStrategy.values.find(_.toString.toLowerCase == value.toLowerCase) match {
      case Some(v) => v
      case _ => throw new IllegalArgumentException(s"merge_strategy '$value' does not exist")
    }
  }

  def init(): Unit = {}

  /** Is there are jobs that needs to be added before the rest of the jobs this methods can be overriden, to let the sample jobs this work the super call should be done also */
  def biopetScript(): Unit = {
    addSamplesJobs()
    addSummaryJobs()
  }

  /** This is de default multisample mapping report, this can be extended by other pipelines */
  override def reportClass: Option[ReportBuilderExtension] = {
    val report = new MultisampleMappingReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryDbFile = summaryDbFile
    Some(report)
  }

  override def defaults: Map[String, Any] = super.defaults ++ Map(
    "reordersam" -> Map("allow_incomplete_dict_concordance" -> true),
    "gears" -> Map("skip_flexiprep" -> true)
  )

  override def fixedValues: Map[String, Any] =
    super.fixedValues ++ Map("gearssingle" -> Map("skip_flexiprep" -> true))

  /** In a default multisample mapping run there are no multsample jobs. This method can be overriden by other pipelines */
  def addMultiSampleJobs(): Unit = {
    // this code will be executed after all code of all samples is executed
  }

  /** By default only the reference is put in the summary, when extending pippeline specific files can be added */
  def summaryFiles: Map[String, File] = Map("referenceFasta" -> referenceFasta())

  /** By default only the reference is put in the summary, when extending pippeline specific settings can be added */
  def summarySettings: Map[String, Any] =
    Map("reference" -> referenceSummary, "merge_strategy" -> mergeStrategy.toString)

  val extractTaxonomies: Boolean = config("extract_taxonomies", default = false)

  def makeSample(id: String) = new Sample(id)
  class Sample(sampleId: String) extends AbstractSample(sampleId) { sample =>

    val gearsJob: Option[GearsSingle] = mappingToGears match {
      case "unmapped" =>
        val gears = new GearsSingle(qscript)
        gears.sampleId = Some(sampleId)
        gears.outputDir = new File(sampleDir, "gears")
        Some(gears)
      case "all" =>
        val gears = new GearsSingle(qscript)
        gears.sampleId = Some(sampleId)
        gears.outputDir = new File(sampleDir, "gears")
        Some(gears)
      case "none" => None
      case x => {
        Logging.addError(s"$x is not a valid value for 'mapping_to_gears'")
        None
      }
    }

    def metricsPreprogressBam = true

    def makeLibrary(id: String) = new Library(id)
    class Library(libId: String) extends AbstractLibrary(libId) { lib =>

      /** By default the bams files are put in the summary, more files can be added here */
      def summaryFiles: Map[String, File] =
        (inputR1.map("input_R1" -> _) :: inputR2.map("input_R2" -> _) ::
          inputBam.map("input_bam" -> _) :: bamFile.map("output_bam" -> _) ::
          preProcessBam.map("output_bam_preprocess" -> _) :: Nil).flatten.toMap

      def summaryStats: Map[String, Any] = Map()

      lazy val inputR1: Option[File] = MultisampleMapping.fileMustBeAbsolute(config("R1"))
      lazy val inputR2: Option[File] = MultisampleMapping.fileMustBeAbsolute(config("R2"))
      lazy val qcFastqR1 = mapping.map(_.flexiprep.fastqR1Qc)
      lazy val qcFastqR2 = mapping.flatMap(_.flexiprep.fastqR2Qc)
      lazy val inputBam: Option[File] =
        MultisampleMapping.fileMustBeAbsolute(if (inputR1.isEmpty) config("bam") else None)
      lazy val bamToFastq: Boolean = config("bam_to_fastq", default = false)
      lazy val correctReadgroups: Boolean = config("correct_readgroups", default = false)

      def keepFinalBamfile: Boolean = samples(sampleId).libraries.size == 1

      lazy val mapping: Option[Mapping] =
        if (inputR1.isDefined || (inputBam.isDefined && bamToFastq)) {
          val m: Mapping = new Mapping(qscript) {
            override def configNamespace = "mapping"
            override def defaults: Map[String, Any] =
              super.defaults ++
                Map("keep_final_bamfile" -> keepFinalBamfile)
          }
          m.sampleId = Some(sampleId)
          m.libId = Some(libId)
          m.outputDir = libDir
          m.centrifugeKreport =
            gearsJob.flatMap(g => g.centrifugeScript.map(c => c.centrifugeNonUniqueKReport))
          m.centrifugeOutputFile =
            gearsJob.flatMap(g => g.centrifugeScript.map(c => c.centrifugeOutput))
          Some(m)
        } else None

      def bamFile: Option[File] = mapping match {
        case Some(m) => Some(m.mergedBamFile)
        case _ if inputBam.isDefined => Some(new File(libDir, s"$sampleId-$libId.bam"))
        case _ => None
      }

      /** By default the preProcessBam is the same as the normal bamFile. A pipeline can extend this is there are preprocess steps */
      def preProcessBam: Option[File] = bamFile

      /** This method can be extended to add jobs to the pipeline, to do this the super call of this function must be called by the pipelines */
      def addJobs(): Unit = {
        inputR1.foreach(inputFiles :+= new InputFile(_, config("R1_md5")))
        inputR2.foreach(inputFiles :+= new InputFile(_, config("R2_md5")))
        inputBam.foreach(inputFiles :+= new InputFile(_, config("bam_md5")))

        if (inputR1.isDefined) {
          mapping.foreach { m =>
            m.inputR1 = inputR1.get
            m.inputR2 = inputR2
            add(m)
          }
        } else if (inputBam.isDefined) {
          if (bamToFastq) {
            val samToFastq = SamToFastq(qscript,
                                        inputBam.get,
                                        new File(libDir, sampleId + "-" + libId + ".R1.fq.gz"),
                                        new File(libDir, sampleId + "-" + libId + ".R2.fq.gz"))
            samToFastq.isIntermediate = libraries.size > 1
            qscript.add(samToFastq)
            mapping.foreach(m => {
              m.inputR1 = samToFastq.fastqR1
              m.inputR2 = Some(samToFastq.fastqR2)
              add(m)
            })
          } else {
            val inputSam = SamReaderFactory.makeDefault.open(inputBam.get)
            val header = inputSam.getFileHeader
            val readGroups = header.getReadGroups
            val referenceFile = new FastaSequenceFile(referenceFasta(), true)
            val dictOke: Boolean = {
              var oke = true
              try {
                header.getSequenceDictionary.assertSameDictionary(referenceDict)
              } catch {
                case e: AssertionError =>
                  logger.error(e.getMessage)
                  oke = false
              }
              oke
            }
            inputSam.close()
            referenceFile.close()

            val readGroupOke = readGroups.forall(readGroup => {
              if (readGroup.getSample != sampleId)
                logger.warn("Sample ID readgroup in bam file is not the same")
              if (readGroup.getLibrary != libId)
                logger.warn("Library ID readgroup in bam file is not the same")
              readGroup.getSample == sampleId && readGroup.getLibrary == libId
            }) && readGroups.nonEmpty

            if (!readGroupOke || !dictOke) {
              if (!readGroupOke && !correctReadgroups)
                Logging.addError(
                  "Sample readgroup and/or library of input bamfile is not correct, file: " + bamFile +
                    "\nPlease note that it is possible to set 'correct_readgroups' to true in the config to automatic fix this")
              if (!dictOke)
                Logging.addError(
                  "Sequence dictionary in the bam file is not the same as the reference, file: " + bamFile)

              if (!readGroupOke && correctReadgroups) {
                logger.info("Correcting readgroups, file:" + inputBam.get)
                val aorrg = AddOrReplaceReadGroups(qscript, inputBam.get, bamFile.get)
                aorrg.RGID = config("rgid", default = s"$sampleId-$libId")
                aorrg.RGLB = libId
                aorrg.RGSM = sampleId
                aorrg.RGPL = config("rgpl", default = "unknown")
                aorrg.RGPU = config("rgpu", default = "na")
                aorrg.isIntermediate = libraries.size > 1
                qscript.add(aorrg)
              }
            } else add(Ln.linkBamFile(qscript, inputBam.get, bamFile.get): _*)

            val bamMetrics = new BamMetrics(qscript)
            bamMetrics.sampleId = Some(sampleId)
            bamMetrics.libId = Some(libId)
            bamMetrics.inputBam = bamFile.get
            bamMetrics.outputDir = new File(libDir, "metrics")
            add(bamMetrics)

            if (config("execute_bam2wig", default = true)) add(Bam2Wig(qscript, bamFile.get))
          }
        } else logger.warn(s"Sample '$sampleId' does not have any input files")
      }
    }

    /** By default the bams files are put in the summary, more files can be added here */
    def summaryFiles: Map[String, File] =
      (bamFile.map("output_bam" -> _) ::
        preProcessBam.map("output_bam_preprocess" -> _) :: Nil).flatten.toMap

    def summaryStats: Map[String, Any] = Map()

    /** This is the merged bam file, None if the merged bam file is NA */
    def bamFile: Option[File] =
      if (libraries.flatMap(_._2.bamFile).nonEmpty &&
          mergeStrategy != MultisampleMapping.MergeStrategy.None)
        Some(new File(sampleDir, s"$sampleId.bam"))
      else None

    /** By default the preProcessBam is the same as the normal bamFile. A pipeline can extend this is there are preprocess steps */
    def preProcessBam: Option[File] = bamFile

    /** Default is set to keep the merged files, user can set this in the config. To change the default this method can be overriden */
    def keepMergedFiles: Boolean = config("keep_merged_files", default = true)

    /**
      * @deprecated
      */
    lazy val unmappedToGears: Boolean = config("unmapped_to_gears", default = false)
    if (config.contains("unmapped_to_gears"))
      logger.warn(
        "Config value 'unmapped_to_gears' is replaced with 'mapping_to_gears', Assumes default: mapping_to_gears=unmapped")

    lazy val mappingToGears: String =
      config("mapping_to_gears", default = if (unmappedToGears) "unmapped" else "none")

    /** This method can be extended to add jobs to the pipeline, to do this the super call of this function must be called by the pipelines */
    def addJobs(): Unit = {
      addPerLibJobs() // This add jobs for each library

      mergeStrategy match {
        case MergeStrategy.None =>
        case (MergeStrategy.MergeSam) if libraries.flatMap(_._2.bamFile).size == 1 =>
          add(Ln.linkBamFile(qscript, libraries.flatMap(_._2.bamFile).head, bamFile.get): _*)
        case (MergeStrategy.PreProcessMergeSam)
            if libraries.flatMap(_._2.preProcessBam).size == 1 =>
          add(Ln.linkBamFile(qscript, libraries.flatMap(_._2.preProcessBam).head, bamFile.get): _*)
        case MergeStrategy.MergeSam =>
          add(
            MergeSamFiles(qscript,
                          libraries.flatMap(_._2.bamFile).toList,
                          bamFile.get,
                          isIntermediate = !keepMergedFiles))
        case MergeStrategy.PreProcessMergeSam =>
          add(
            MergeSamFiles(qscript,
                          libraries.flatMap(_._2.preProcessBam).toList,
                          bamFile.get,
                          isIntermediate = !keepMergedFiles))
        case MergeStrategy.MarkDuplicates =>
          add(
            MarkDuplicates(qscript,
                           libraries.flatMap(_._2.bamFile).toList,
                           bamFile.get,
                           isIntermediate = !keepMergedFiles))
        case MergeStrategy.PreProcessMarkDuplicates =>
          add(
            MarkDuplicates(qscript,
                           libraries.flatMap(_._2.preProcessBam).toList,
                           bamFile.get,
                           isIntermediate = !keepMergedFiles))
        case MergeStrategy.PreProcessSambambaMarkdup =>
          val mergedBam = if (libraries.flatMap(_._2.bamFile).size == 1) {
            add(
              Ln.linkBamFile(qscript,
                             libraries.flatMap(_._2.preProcessBam).head,
                             new File(sampleDir, "merged.bam")): _*)
            libraries.flatMap(_._2.preProcessBam).head
          } else {
            val merge = new SambambaMerge(qscript)
            merge.input = libraries.flatMap(_._2.preProcessBam).toList
            merge.output = new File(sampleDir, "merged.bam")
            merge.isIntermediate = true
            add(merge)
            merge.output
          }
          add(SambambaMarkdup(qscript, mergedBam, bamFile.get, isIntermediate = !keepMergedFiles))
          add(
            Ln(qscript,
               bamFile.get + ".bai",
               bamFile.get.getAbsolutePath.stripSuffix(".bam") + ".bai"))
        case _ =>
          throw new IllegalStateException(
            "This should not be possible, unimplemented MergeStrategy?")
      }

      if (mergeStrategy != MergeStrategy.None && libraries.flatMap(_._2.bamFile).nonEmpty) {
        val bamMetrics = new BamMetrics(qscript)
        bamMetrics.sampleId = Some(sampleId)
        bamMetrics.inputBam = if (metricsPreprogressBam) preProcessBam.get else bamFile.get
        bamMetrics.outputDir = new File(sampleDir, "metrics")
        add(bamMetrics)

        if (config("execute_bam2wig", default = true)) add(Bam2Wig(qscript, preProcessBam.get))
      }

      mappingToGears match {
        case "unmapped" =>
          gearsJob.get.bamFile = preProcessBam
          add(gearsJob.get)
        case "all" =>
          gearsJob.get.fastqR1 = libraries.flatMap(_._2.qcFastqR1).toList
          gearsJob.get.fastqR2 = libraries.flatMap(_._2.qcFastqR2).toList
          add(gearsJob.get)
        case _ =>
      }
    }
  }
}

/** This class is the default implementation that can be used on the command line */
class MultisampleMapping(val parent: Configurable) extends QScript with MultisampleMappingTrait {
  def this() = this(null)
}

object MultisampleMapping extends PipelineCommand {

  object MergeStrategy extends Enumeration {
    val None, MergeSam, MarkDuplicates, PreProcessMergeSam, PreProcessMarkDuplicates,
    PreProcessSambambaMarkdup = Value
  }

  /** When file is not absolute an error is raise att the end of the script of a pipeline */
  def fileMustBeAbsolute(file: Option[File]): Option[File] = {
    if (file.forall(_.isAbsolute)) file
    else {
      Logging.addError(s"$file should be a absolute file path")
      file.map(_.getAbsoluteFile)
    }
  }
}
