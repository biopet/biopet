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

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.kraken.{ Kraken, KrakenReport }
import nl.lumc.sasc.biopet.extensions.picard.{ SortSam, SamToFastq }
import nl.lumc.sasc.biopet.extensions.sambamba.SambambaView
import nl.lumc.sasc.biopet.extensions.tools.{ KrakenReportToJson, FastqSync }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * This is a trait for the Gears pipeline
 * The ShivaTrait is used as template for this pipeline
 */
class Gears(val root: Configurable) extends QScript with SummaryQScript {
  qscript =>
  def this() = this(null)

  @Input(shortName = "R1", required = false)
  var fastqFileR1: Option[File] = None

  @Input(shortName = "R2", required = false)
  var fastqFileR2: Option[File] = None

  @Input(doc = "From the bam all the upmapped reads are used for kraken", shortName = "bam", required = false)
  var bamFile: Option[File] = None

  @Argument(required = false)
  var outputName: String = _

  var GearsOutputFiles: Map[String, File] = Map.empty

  /** Executed before running the script */
  def init(): Unit = {
    require(fastqFileR1.isDefined || bamFile.isDefined, "Must define fastq file(s) or a bam file")
    require(fastqFileR1.isDefined != bamFile.isDefined, "Can't define a bam file and a R1 file")

    if (outputName == null) {
      if (fastqFileR1.isDefined) outputName = fastqFileR1.map(_.getName
        .stripSuffix(".gz")
        .stripSuffix(".fastq")
        .stripSuffix(".fq"))
        .getOrElse("noName")
      else outputName = bamFile.map(_.getName.stripSuffix(".bam")).getOrElse("noName")
    }
  }

  /** Method to add jobs */
  def biopetScript(): Unit = {

    val fastqFiles: List[File] = bamFile.map { bamfile =>

      // sambamba view -f bam -F "unmapped or mate_is_unmapped" <alnFile> > <extracted.bam>
      val samFilterUnmapped = new SambambaView(qscript)
      samFilterUnmapped.input = bamfile
      samFilterUnmapped.filter = Some("(unmapped or mate_is_unmapped) and not (secondary_alignment)")
      samFilterUnmapped.output = new File(outputDir, s"$outputName.unmapped.bam")
      samFilterUnmapped.isIntermediate = false
      add(samFilterUnmapped)

      // start bam to fastq (only on unaligned reads) also extract the matesam
      val samToFastq = new SamToFastq(qscript)
      samToFastq.input = samFilterUnmapped.output
      samToFastq.stringency = Some("LENIENT")
      samToFastq.fastqR1 = new File(outputDir, s"$outputName.unmapped.R1.fq.gz")
      samToFastq.fastqR2 = new File(outputDir, s"$outputName.unmapped.R2.fq.gz")
      samToFastq.fastqUnpaired = new File(outputDir, s"$outputName.unmapped.singleton.fq.gz")
      samToFastq.isIntermediate = true
      add(samToFastq)

      // sync the fastq records
      val fastqSync = new FastqSync(qscript)
      fastqSync.refFastq = samToFastq.fastqR1
      fastqSync.inputFastq1 = samToFastq.fastqR1
      fastqSync.inputFastq2 = samToFastq.fastqR2
      fastqSync.outputFastq1 = new File(outputDir, s"$outputName.unmapped.R1.sync.fq.gz")

      // TODO: need some sanity check on whether R2 is really containing reads (e.g. Single End libraries)
      fastqSync.outputFastq2 = new File(outputDir, s"$outputName.unmapped.R2.sync.fq.gz")
      fastqSync.outputStats = new File(outputDir, s"$outputName.sync.stats.json")
      add(fastqSync)

      GearsOutputFiles ++ Map("fastqsync_stats" -> fastqSync.outputStats)
      GearsOutputFiles ++ Map("fastqsync_R1" -> fastqSync.outputFastq1)
      GearsOutputFiles ++ Map("fastqsync_R2" -> fastqSync.outputFastq2)

      List(fastqSync.outputFastq1, fastqSync.outputFastq2)
    }.getOrElse(List(fastqFileR1, fastqFileR2).flatten)

    // start kraken
    val krakenAnalysis = new Kraken(qscript)
    krakenAnalysis.input = fastqFiles
    krakenAnalysis.output = new File(outputDir, s"$outputName.krkn.raw")

    krakenAnalysis.paired = (fastqFiles.length == 2)

    krakenAnalysis.classified_out = Option(new File(outputDir, s"$outputName.krkn.classified.fastq"))
    krakenAnalysis.unclassified_out = Option(new File(outputDir, s"$outputName.krkn.unclassified.fastq"))
    add(krakenAnalysis)

    GearsOutputFiles ++ Map("kraken_output_raw" -> krakenAnalysis.output)
    GearsOutputFiles ++ Map("kraken_classified_out" -> krakenAnalysis.classified_out)
    GearsOutputFiles ++ Map("kraken_unclassified_out" -> krakenAnalysis.unclassified_out)

    // create kraken summary file

    val krakenReport = new KrakenReport(qscript)
    krakenReport.input = krakenAnalysis.output
    krakenReport.show_zeros = true
    krakenReport.output = new File(outputDir, s"$outputName.krkn.full")
    add(krakenReport)

    GearsOutputFiles ++ Map("kraken_report_input" -> krakenReport.input)
    GearsOutputFiles ++ Map("kraken_report_output" -> krakenReport.output)

    val krakenReportJSON = new KrakenReportToJson(qscript)
    krakenReportJSON.inputReport = krakenReport.output
    krakenReportJSON.output = new File(outputDir, s"$outputName.krkn.json")
    krakenReportJSON.skipNames = config("skipNames", default = false)
    add(krakenReportJSON)

    addSummaryJobs()

    GearsOutputFiles ++ Map("kraken_report_json_input" -> krakenReportJSON.inputReport)
    GearsOutputFiles ++ Map("kraken_report_json_output" -> krakenReportJSON.output)
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, "gears.summary.json")

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map.empty ++
    (if (bamFile.isDefined) Map("input_bam" -> bamFile.get) else Map()) ++
    (if (fastqFileR1.isDefined) Map("input_R1" -> fastqFileR1.get) else Map())

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = Map.empty ++
    (if (bamFile.isDefined) Map("input_bam" -> bamFile.get) else Map()) ++
    (if (fastqFileR1.isDefined) Map("input_R1" -> fastqFileR1.get) else Map()) ++
    GearsOutputFiles
}

/** This object give a default main method to the pipelines */
object Gears extends PipelineCommand