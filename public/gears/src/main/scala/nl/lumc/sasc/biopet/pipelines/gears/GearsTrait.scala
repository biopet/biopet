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
package nl.lumc.sasc.biopet.pipelines.gears

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.FullVersion
import nl.lumc.sasc.biopet.core.MultiSampleQScript
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.kraken.{ Kraken, KrakenReport }
import nl.lumc.sasc.biopet.extensions.picard.{ AddOrReplaceReadGroups, MarkDuplicates, MergeSamFiles, SamToFastq }
import nl.lumc.sasc.biopet.extensions.sambamba.SambambaView
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping
import nl.lumc.sasc.biopet.tools.FastqSync

import scala.collection.JavaConversions._

/**
 * This is a trait for the Gears pipeline
 * The ShivaTrait is used as template for this pipeline
 */
trait GearsTrait extends MultiSampleQScript with SummaryQScript { qscript =>

  /** Executed before running the script */
  def init(): Unit = {
  }

  /** Method to add jobs */
  def biopetScript(): Unit = {
    addSamplesJobs()
    addSummaryJobs()
  }

  override def reportClass = {
    val report = new GearsReport(this)
    report.outputDir = new File(outputDir, "report")
    report.summaryFile = summaryFile
    Some(report)
  }
  /** Multisample meta-genome comparison */
  def addMultiSampleJobs(): Unit = {
    // generate report from multiple samples, this is:
    // - the TSV
    // - the Spearman correlation plot + table
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, "gears.summary.json")

  /** Settings of pipeline for summary */
  def summarySettings = Map(
    "version" -> FullVersion
  )

  /** Files for the summary */
  def summaryFiles = Map()

  /** Method to make a sample */
  def makeSample(id: String) = new Sample(id)

  /** Class that will generate jobs for a sample */
  class Sample(sampleId: String) extends AbstractSample(sampleId) {
    /** Sample specific files to add to summary */
    def summaryFiles: Map[String, File] = {
      preProcessBam match {
        case Some(pb) => Map("bamFile" -> pb)
        case _        => Map()
      }
    } ++ Map(
      "alignment" -> alnFile
    )

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

      /** Alignment results of this library ~ can only be accessed after addJobs is run! */
      def alnFile: File = bamFile match {
        case Some(b) => b
        case _       => throw new IllegalStateException("The bamfile is not generated yet")
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

      /**
       * Determine where where to start the pipeline in cases where both R1 (fastq) and BAM is specified
       */
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

      /** This will add jobs for this library */
      def addJobs(): Unit = {
        (config.contains("R1"), config.contains("bam")) match {
          case (true, _) => mapping.foreach(mapping => {
            mapping.input_R1 = config("R1")
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
          addAll(mapping.functions) // Add functions of mapping to current function pool
          addSummaryQScript(mapping)
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
        md.removeDuplicates = true
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
    def alnFile: File = sampleBamLinkJob.output

    /** Job for combining all library BAMs */
    private def sampleBamLinkJob: Ln =
      makeCombineJob(libraries.values.map(_.alnFile).toList, createFile(".bam"))

    /** Ln or MergeSamFile job, depending on how many inputs are supplied */
    private def makeCombineJob(inFiles: List[File], outFile: File,
                               mergeSortOrder: String = "coordinate"): Ln = {
      require(inFiles.nonEmpty, "At least one input files for combine job")
      val input: File = {

        if (inFiles.size == 1) inFiles.head
        else {
          val mergedBam = createFile(".merged.bam")
          val mergejob = new MergeSamFiles(qscript)
          mergejob.input = inFiles
          mergejob.output = mergedBam
          mergejob.sortOrder = mergeSortOrder
          add(mergejob)
          mergejob.output
        }
      }

      val linkJob = new Ln(qscript)
      linkJob.input = input
      linkJob.output = outFile
      linkJob

    }

    /** This will add sample jobs */
    def addJobs(): Unit = {
      addPerLibJobs()
      // merge or symlink per-library alignments
      add(sampleBamLinkJob)

      if (preProcessBam.isDefined) {
        val bamMetrics = new BamMetrics(qscript)
        bamMetrics.sampleId = Some(sampleId)
        bamMetrics.inputBam = preProcessBam.get
        bamMetrics.outputDir = sampleDir
        bamMetrics.init()
        bamMetrics.biopetScript()
        addAll(bamMetrics.functions)
        addSummaryQScript(bamMetrics)
      }

      // sambamba view -f bam -F "unmapped or mate_is_unmapped" <alnFile> > <extracted.bam>
      val samFilterUnmapped = new SambambaView(qscript)
      samFilterUnmapped.input = alnFile
      samFilterUnmapped.filter = Some("unmapped or mate_is_unmapped")
      samFilterUnmapped.output = createFile(".unmapped.bam")
      samFilterUnmapped.isIntermediate = true
      qscript.add(samFilterUnmapped)

      // start bam to fastq (only on unaligned reads) also extract the matesam
      val samToFastq = SamToFastq(qscript, alnFile,
        createFile(".unmap.R1.fastq"),
        createFile(".unmap.R2.fastq")
      )
      samToFastq.isIntermediate = true
      qscript.add(samToFastq)

      // sync the fastq records
      val fastqsync = new FastqSync(qscript)
      fastqsync.refFastq = samToFastq.fastqR1
      fastqsync.inputFastq1 = samToFastq.fastqR1
      fastqsync.inputFastq2 = samToFastq.fastqR2
      fastqsync.outputFastq1 = createFile(".unmapsynced.R1.fastq.gz")
      fastqsync.outputFastq2 = createFile(".unmapsynced.R2.fastq.gz")
      fastqsync.outputStats = createFile(".syncstats.json")
      qscript.add(fastqsync)

      // start kraken
      val krakenAnalysis = new Kraken(qscript)
      krakenAnalysis.input = List(fastqsync.outputFastq1, fastqsync.outputFastq2)
      krakenAnalysis.output = createFile(".krkn.raw")
      krakenAnalysis.paired = true
      krakenAnalysis.classified_out = Option(createFile(".krkn.classified.fastq"))
      krakenAnalysis.unclassified_out = Option(createFile(".krkn.unclassified.fastq"))
      qscript.add(krakenAnalysis)

      // create kraken summary file

      val krakenReport = new KrakenReport(qscript)
      krakenReport.input = krakenAnalysis.output
      krakenReport.show_zeros = true
      krakenReport.output = createFile(".krkn.full")
      qscript.add(krakenReport)

    }
  }

}
