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
import nl.lumc.sasc.biopet.core.{ MultiSampleQScript, Reference }
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.{ AddOrReplaceReadGroups, MarkDuplicates, SamToFastq }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.pipelines.toucan.Toucan

import scala.collection.JavaConversions._

/**
 * This is a trait for the Shiva pipeline
 *
 * Created by pjvan_thof on 2/26/15.
 */
trait ShivaTrait extends MultiSampleQScript with Reference {
  qscript =>

  /** Executed before running the script */
  def init(): Unit = {
  }

  /** Method to add jobs */
  def biopetScript(): Unit = {
    addSamplesJobs()

    addSummaryJobs()
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

    /** Sample specific settings */
    override def summarySettings = Map("single_sample_variantcalling" -> variantcalling.isDefined)

    /** Class to generate jobs for a library */
    class Library(libId: String) extends AbstractLibrary(libId) {
      /** Library specific files to add to the summary */
      def summaryFiles: Map[String, File] = {
        ((bamFile, preProcessBam) match {
          case (Some(b), Some(pb)) => Map("bamFile" -> b, "preProcessBam" -> pb)
          case (Some(b), _)        => Map("bamFile" -> b, "preProcessBam" -> b)
          case _                   => Map()
        }) ++ (inputR1.map("input_R1" -> _) ::
          inputR2.map("input_R2" -> _) ::
          inputBam.map("input_bam" -> _) :: Nil).flatten.toMap
      }

      /** Library specific stats to add to summary */
      def summaryStats: Map[String, Any] = Map()

      /** Method to execute library preprocess */
      def preProcess(input: File): Option[File] = None

      /** Library specific settings */
      override def summarySettings = Map("library_variantcalling" -> variantcalling.isDefined)

      /** Method to make the mapping submodule */
      def makeMapping = {
        val mapping = new Mapping(qscript)
        mapping.sampleId = Some(sampleId)
        mapping.libId = Some(libId)
        mapping.outputDir = libDir
        mapping.outputName = sampleId + "-" + libId
        (Some(mapping), Some(mapping.finalBamFile), preProcess(mapping.finalBamFile))
      }

      lazy val inputR1: Option[File] = config("R1")
      lazy val inputR2: Option[File] = config("R2")
      lazy val inputBam: Option[File] = if (inputR1.isEmpty) config("bam") else None

      lazy val (mapping, bamFile, preProcessBam): (Option[Mapping], Option[File], Option[File]) =
        (inputR1.isDefined, inputBam.isDefined) match {
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
        (inputR1.isDefined, inputBam.isDefined) match {
          case (true, _) => mapping.foreach(mapping => {
            mapping.input_R1 = inputR1.get
            mapping.input_R2 = inputR2
            inputFiles :+= new InputFile(mapping.input_R1, config("R1_md5"))
            mapping.input_R2.foreach(inputFiles :+= new InputFile(_, config("R2_md5")))
          })
          case (false, true) => {
            inputFiles :+= new InputFile(inputBam.get, config("bam_md5"))
            config("bam_to_fastq", default = false).asBoolean match {
              case true =>
                val samToFastq = SamToFastq(qscript, inputBam.get,
                  new File(libDir, sampleId + "-" + libId + ".R1.fq.gz"),
                  new File(libDir, sampleId + "-" + libId + ".R2.fq.gz"))
                samToFastq.isIntermediate = true
                qscript.add(samToFastq)
                mapping.foreach(mapping => {
                  mapping.input_R1 = samToFastq.fastqR1
                  mapping.input_R2 = Some(samToFastq.fastqR2)
                })
              case false =>
                val inputSam = SamReaderFactory.makeDefault.open(inputBam.get)
                val readGroups = inputSam.getFileHeader.getReadGroups

                val readGroupOke = readGroups.forall(readGroup => {
                  if (readGroup.getSample != sampleId) logger.warn("Sample ID readgroup in bam file is not the same")
                  if (readGroup.getLibrary != libId) logger.warn("Library ID readgroup in bam file is not the same")
                  readGroup.getSample == sampleId && readGroup.getLibrary == libId
                })
                inputSam.close()

                if (!readGroupOke) {
                  if (config("correct_readgroups", default = false).asBoolean) {
                    logger.info("Correcting readgroups, file:" + inputBam.get)
                    val aorrg = AddOrReplaceReadGroups(qscript, inputBam.get, bamFile.get)
                    aorrg.RGID = sampleId + "-" + libId
                    aorrg.RGLB = libId
                    aorrg.RGSM = sampleId
                    aorrg.isIntermediate = true
                    qscript.add(aorrg)
                  } else throw new IllegalStateException("Sample readgroup and/or library of input bamfile is not correct, file: " + bamFile +
                    "\nPlease note that it is possible to set 'correct_readgroups' to true in the config to automatic fix this")
                } else {
                  val oldBamFile: File = inputBam.get
                  val oldIndex: File = new File(oldBamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
                  val newIndex: File = new File(libDir, oldBamFile.getName.stripSuffix(".bam") + ".bai")
                  val baiLn = Ln(qscript, oldIndex, newIndex)
                  add(baiLn)

                  val bamLn = Ln(qscript, oldBamFile, bamFile.get)
                  bamLn.deps :+= baiLn.output
                  add(bamLn)
                }
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
          vc.sampleId = Some(sampleId)
          vc.libId = Some(libId)
          vc.outputDir = new File(libDir, "variantcalling")
          if (preProcessBam.isDefined) vc.inputBams = preProcessBam.get :: Nil
          else vc.inputBams = bamFile.get :: Nil
          vc.init()
          vc.biopetScript()
          addAll(vc.functions)
          addSummaryQScript(vc)
        })
      }
    }

    /** This will add jobs for the double preprocessing */
    protected def addDoublePreProcess(input: List[File], isIntermediate: Boolean = false): Option[File] = {
      if (input == Nil) None
      else if (input.tail == Nil) {
        val bamFile = new File(sampleDir, s"$sampleId.bam")
        val oldIndex: File = new File(input.head.getAbsolutePath.stripSuffix(".bam") + ".bai")
        val newIndex: File = new File(sampleDir, s"$sampleId.bai")
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
      Some(makeVariantcalling(multisample = false))
    } else None

    /** This will add sample jobs */
    def addJobs(): Unit = {
      addPerLibJobs()

      preProcessBam.foreach { bam =>
        val bamMetrics = new BamMetrics(qscript)
        bamMetrics.sampleId = Some(sampleId)
        bamMetrics.inputBam = bam
        bamMetrics.outputDir = new File(sampleDir, "metrics")
        bamMetrics.init()
        bamMetrics.biopetScript()
        addAll(bamMetrics.functions)
        addSummaryQScript(bamMetrics)

        variantcalling.foreach(vc => {
          vc.sampleId = Some(sampleId)
          vc.outputDir = new File(sampleDir, "variantcalling")
          vc.inputBams = bam :: Nil
          vc.init()
          vc.biopetScript()
          addAll(vc.functions)
          addSummaryQScript(vc)
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
  def addMultiSampleJobs(): Unit = {
    multisampleVariantCalling.foreach(vc => {
      vc.outputDir = new File(outputDir, "variantcalling")
      vc.inputBams = samples.flatMap(_._2.preProcessBam).toList
      vc.init()
      vc.biopetScript()
      addAll(vc.functions)
      addSummaryQScript(vc)

      annotation.foreach { toucan =>
        toucan.outputDir = new File(outputDir, "annotation")
        toucan.inputVCF = vc.finalFile
        toucan.init()
        toucan.biopetScript()
        addAll(toucan.functions)
        addSummaryQScript(toucan)
      }
    })

    svCalling.foreach(sv => {
      sv.outputDir = new File(outputDir, "sv_calling")
      samples.foreach(x => x._2.preProcessBam.foreach(bam => sv.addBamFile(bam, Some(x._1))))
      sv.init()
      sv.biopetScript()
      addAll(sv.functions)
      addSummaryQScript(sv)
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
      "amplicon_bed" -> ampliconBedFile.map(_.getName.stripSuffix(".bed")),
      "annotation" -> annotation.isDefined,
      "multisample_variantcalling" -> multisampleVariantCalling.isDefined,
      "sv_calling" -> svCalling.isDefined
    )
  }

  /** Files for the summary */
  def summaryFiles = Map("referenceFasta" -> referenceFasta())
}
