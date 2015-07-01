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

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ Reference, MultiSampleQScript }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{ AddOrReplaceReadGroups, SamToFastq, MarkDuplicates }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import scala.collection.JavaConversions._

/**
 * This is a trait for the Shiva pipeline
 *
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaTrait extends MultiSampleQScript with SummaryQScript with Reference {
  qscript =>

  /** Executed before running the script */
  def init: Unit = {
  }

  /** Method to add jobs */
  def biopetScript: Unit = {
    addSamplesJobs()

    addSummaryJobs
  }

  override def reportClass = {
    val shiva = new ShivaReport(this)
    shiva.outputDir = new File(outputDir, "report")
    shiva.summaryFile = summaryFile
    Some(shiva)
  }

  /** Method to make the variantcalling submodule of shiva */
  def makeVariantcalling(multisample: Boolean = false): ShivaVariantcallingTrait = {
    if (multisample) new ShivaVariantcalling(qscript) {
      override def namePrefix = "multisample"
      override def configName = "shivavariantcalling"
      override def configPath: List[String] = super.configPath ::: "multisample" :: Nil
    }
    else new ShivaVariantcalling(qscript) {
      override def configName = "shivavariantcalling"
    }
  }

  /** Method to make a sample */
  def makeSample(id: String) = new Sample(id)

  /** Class that will generate jobs for a sample */
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    /** Sample specific files to add to summary */
    def summaryFiles: Map[String, File] = {
      preProcessBam match {
        case Some(b) => Map("preProcessBam" -> b)
        case _       => Map()
      }
    }

    /** Sample specific stats to add to summary */
    def summaryStats: Map[String, Any] = Map()

    /** Method to make a library */
    def makeLibrary(id: String) = new Library(id)

    /** Class to generate jobs for a library */
    class Library(libId: String) extends AbstractLibrary(libId) {
      /** Library specific files to add to the summary */
      def summaryFiles: Map[String, File] = {
        (bamFile, preProcessBam) match {
          case (Some(b), Some(pb)) => Map("bamFile" -> b, "preProcessBam" -> pb)
          case (Some(b), _)        => Map("bamFile" -> b)
          case _                   => Map()
        }
      }

      /** Library specific stats to add to summary */
      def summaryStats: Map[String, Any] = Map()

      /** Method to execute library preprocess */
      def preProcess(input: File): Option[File] = None

      /** Method to make the mapping submodule */
      def makeMapping = {
        val mapping = new Mapping(qscript)
        mapping.sampleId = Some(sampleId)
        mapping.libId = Some(libId)
        mapping.outputDir = libDir
        mapping.outputName = sampleId + "-" + libId
        (Some(mapping), Some(mapping.finalBamFile), preProcess(mapping.finalBamFile))
      }

      lazy val (mapping, bamFile, preProcessBam): (Option[Mapping], Option[File], Option[File]) =
        (config.contains("R1"), config.contains("bam")) match {
          case (true, _) => makeMapping // Default starting from fastq files
          case (false, true) => // Starting from bam file
            config("bam_to_fastq", default = false).asBoolean match {
              case true => makeMapping // bam file will be converted to fastq
              case false =>
                val file = new File(libDir, sampleId + "-" + libId + ".final.bam")
                (None, Some(file), preProcess(file))
            }
          case _ => (None, None, None)
        }

      lazy val variantcalling = if (config("library_variantcalling", default = false).asBoolean &&
        (bamFile.isDefined || preProcessBam.isDefined)) {
        Some(makeVariantcalling(multisample = false))
      } else None

      /** This will add jobs for this library */
      def addJobs(): Unit = {
        (config.contains("R1"), config.contains("bam")) match {
          case (true, _) => mapping.foreach(mapping => {
            mapping.input_R1 = config("R1")
            mapping.input_R2 = config("R2")
          })
          case (false, true) => config("bam_to_fastq", default = false).asBoolean match {
            case true =>
              val samToFastq = SamToFastq(qscript, config("bam"),
                new File(libDir, sampleId + "-" + libId + ".R1.fastq"),
                new File(libDir, sampleId + "-" + libId + ".R2.fastq"))
              samToFastq.isIntermediate = true
              qscript.add(samToFastq)
              mapping.foreach(mapping => {
                mapping.input_R1 = samToFastq.fastqR1
                mapping.input_R2 = Some(samToFastq.fastqR2)
              })
            case false =>
              val inputSam = SamReaderFactory.makeDefault.open(config("bam"))
              val readGroups = inputSam.getFileHeader.getReadGroups

              val readGroupOke = readGroups.forall(readGroup => {
                if (readGroup.getSample != sampleId) logger.warn("Sample ID readgroup in bam file is not the same")
                if (readGroup.getLibrary != libId) logger.warn("Library ID readgroup in bam file is not the same")
                readGroup.getSample == sampleId && readGroup.getLibrary == libId
              })
              inputSam.close()

              if (!readGroupOke) {
                if (config("correct_readgroups", default = false).asBoolean) {
                  logger.info("Correcting readgroups, file:" + config("bam"))
                  val aorrg = AddOrReplaceReadGroups(qscript, config("bam"), bamFile.get)
                  aorrg.RGID = sampleId + "-" + libId
                  aorrg.RGLB = libId
                  aorrg.RGSM = sampleId
                  aorrg.isIntermediate = true
                  qscript.add(aorrg)
                } else throw new IllegalStateException("Sample readgroup and/or library of input bamfile is not correct, file: " + bamFile +
                  "\nPlease note that it is possible to set 'correct_readgroups' to true in the config to automatic fix this")
              } else {
                val oldBamFile: File = config("bam")
                val oldIndex: File = new File(oldBamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
                val newIndex: File = new File(libDir, oldBamFile.getName.stripSuffix(".bam") + ".bai")
                val baiLn = Ln(qscript, oldIndex, newIndex)
                add(baiLn)

                val bamLn = Ln(qscript, oldBamFile, bamFile.get)
                bamLn.deps :+= baiLn.output
                add(bamLn)
              }
          }
          case _ => logger.warn("Sample: " + sampleId + "  Library: " + libId + ", no reads found")
        }

        mapping.foreach(mapping => {
          mapping.init()
          mapping.biopetScript()
          addAll(mapping.functions) // Add functions of mapping to curent function pool
          addSummaryQScript(mapping)
        })

        variantcalling.foreach(vc => {
          vc.sampleId = Some(libId)
          vc.libId = Some(sampleId)
          vc.outputDir = new File(libDir, "variantcalling")
          if (preProcessBam.isDefined) vc.inputBams = preProcessBam.get :: Nil
          else vc.inputBams = bamFile.get :: Nil
          vc.init
          vc.biopetScript
          addAll(vc.functions)
          addSummaryQScript(vc)
        })
      }
    }

    /** This will add jobs for the double preprocessing */
    protected def addDoublePreProcess(input: List[File], isIntermediate: Boolean = false): Option[File] = {
      if (input == Nil) None
      else if (input.tail == Nil) {
        val bamFile = new File(sampleDir, input.head.getName)
        val oldIndex: File = new File(input.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex: File = new File(sampleDir, input.head.getName.stripSuffix(".bam") + ".bai")
        val baiLn = Ln(qscript, oldIndex, newIndex)
        add(baiLn)

        val bamLn = Ln(qscript, input.head, bamFile)
        bamLn.deps :+= baiLn.output
        add(bamLn)
        Some(bamFile)
      } else {
        val md = new MarkDuplicates(qscript)
        md.input = input
        md.output = new File(sampleDir, sampleId + ".dedup.bam")
        md.outputMetrics = new File(sampleDir, sampleId + ".dedup.metrics")
        md.isIntermediate = isIntermediate
        add(md)
        addSummarizable(md, "mark_duplicates")
        Some(md.output)
      }
    }

    lazy val preProcessBam: Option[File] = addDoublePreProcess(libraries.flatMap(lib => {
      (lib._2.bamFile, lib._2.preProcessBam) match {
        case (_, Some(file)) => Some(file)
        case (Some(file), _) => Some(file)
        case _               => None
      }
    }).toList)

    lazy val variantcalling = if (config("single_sample_variantcalling", default = false).asBoolean) {
      Some(makeVariantcalling(multisample = true))
    } else None

    /** This will add sample jobs */
    def addJobs(): Unit = {
      addPerLibJobs()

      if (preProcessBam.isDefined) {
        val bamMetrics = new BamMetrics(qscript)
        bamMetrics.sampleId = Some(sampleId)
        bamMetrics.inputBam = preProcessBam.get
        bamMetrics.outputDir = new File(sampleDir, "metrics")
        bamMetrics.init()
        bamMetrics.biopetScript()
        addAll(bamMetrics.functions)
        addSummaryQScript(bamMetrics)

        variantcalling.foreach(vc => {
          vc.sampleId = Some(sampleId)
          vc.outputDir = new File(sampleDir, "variantcalling")
          vc.inputBams = preProcessBam.get :: Nil
          vc.init
          vc.biopetScript
          addAll(vc.functions)
          addSummaryQScript(vc)
        })
      }
    }
  }

  lazy val variantcalling = if (config("multisample_sample_variantcalling", default = true).asBoolean) {
    Some(makeVariantcalling(multisample = true))
  } else None

  /** This will add the mutisample variantcalling */
  def addMultiSampleJobs(): Unit = {
    variantcalling.foreach(vc => {
      vc.outputDir = new File(outputDir, "variantcalling")
      vc.inputBams = samples.flatMap(_._2.preProcessBam).toList
      vc.init
      vc.biopetScript
      addAll(vc.functions)
      addSummaryQScript(vc)
    })
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, "Shiva.summary.json")

  /** Settings of pipeline for summary */
  def summarySettings = {
    val roiBedFiles: List[File] = config("regions_of_interest", Nil)
    val ampliconBedFile: Option[File] = config("amplicon_bed")

    Map(
      "reference" -> referenceSummary,
      "regions_of_interest" -> roiBedFiles.map(_.getName.stripSuffix(".bed")),
      "amplicon_bed" -> ampliconBedFile.map(_.getName.stripSuffix(".bed"))
    )
  }

  /** Files for the summary */
  def summaryFiles = Map("referenceFasta" -> referenceFasta())
}
