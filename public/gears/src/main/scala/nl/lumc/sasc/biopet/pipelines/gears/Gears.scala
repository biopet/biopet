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

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.{ PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.kraken.{ Kraken, KrakenReport }
import nl.lumc.sasc.biopet.extensions.picard.SamToFastq
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.extensions.tools.{ FastqSync, KrakenReportToJson }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by wyleung
 */
class Gears(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "R1 reads in FastQ format", shortName = "R1", required = false)
  var fastqR1: Option[File] = None

  @Input(doc = "R2 reads in FastQ format", shortName = "R2", required = false)
  var fastqR2: Option[File] = None

  @Input(doc = "All unmapped reads will be extracted from this bam for analysis", shortName = "bam", required = false)
  var bamFile: Option[File] = None

  @Argument(required = false)
  var outputName: String = _

  /** Executed before running the script */
  def init(): Unit = {
    require(fastqR1.isDefined || bamFile.isDefined, "Please specify fastq-file(s) or bam file")
    require(fastqR1.isDefined != bamFile.isDefined, "Provide either a bam file or la R1 file")

    if (outputName == null) {
      if (fastqR1.isDefined) outputName = fastqR1.map(_.getName
        .stripSuffix(".gz")
        .stripSuffix(".fastq")
        .stripSuffix(".fq"))
        .getOrElse("noName")
      else outputName = bamFile.map(_.getName.stripSuffix(".bam")).getOrElse("noName")
    }
  }

  override def reportClass = {
    val gears = new GearsReport(this)
    gears.outputDir = new File(outputDir, "report")
    gears.summaryFile = summaryFile
    Some(gears)
  }

  override def defaults = Map(
    "samtofastq" -> Map(
      "validationstringency" -> "LENIENT"
    )
  )
  /** Method to add jobs */
  def biopetScript(): Unit = {
    val fastqFiles: List[File] = bamFile.map { bamfile =>

      //      // sambamba view -f bam -F "unmapped or mate_is_unmapped" <alnFile> > <extracted.bam>
      //      val samFilterUnmapped = new SambambaView(this)
      //      samFilterUnmapped.input = bamfile
      //      samFilterUnmapped.filter = Some("(unmapped or mate_is_unmapped) and not (secondary_alignment) and [XH] == null")
      //      samFilterUnmapped.output = new File(outputDir, s"$outputName.unmapped.bam")
      //      samFilterUnmapped.isIntermediate = false
      //      add(samFilterUnmapped)

      val samtoolsViewSelectUnmapped = new SamtoolsView(this)
      samtoolsViewSelectUnmapped.input = bamfile
      samtoolsViewSelectUnmapped.b = true
      samtoolsViewSelectUnmapped.output = new File(outputDir, s"$outputName.unmapped.bam")
      samtoolsViewSelectUnmapped.f = List("4")
      samtoolsViewSelectUnmapped.F = List("8")
      samtoolsViewSelectUnmapped.isIntermediate = true
      add(samtoolsViewSelectUnmapped)

      // start bam to fastq (only on unaligned reads) also extract the matesam
      val samToFastq = new SamToFastq(this)
      samToFastq.input = samtoolsViewSelectUnmapped.output
      samToFastq.fastqR1 = new File(outputDir, s"$outputName.unmapped.R1.fq.gz")
      samToFastq.fastqR2 = new File(outputDir, s"$outputName.unmapped.R2.fq.gz")
      samToFastq.fastqUnpaired = new File(outputDir, s"$outputName.unmapped.singleton.fq.gz")
      samToFastq.isIntermediate = true
      add(samToFastq)

      // sync the fastq records
      val fastqSync = new FastqSync(this)
      fastqSync.refFastq = samToFastq.fastqR1
      fastqSync.inputFastq1 = samToFastq.fastqR1
      fastqSync.inputFastq2 = samToFastq.fastqR2
      fastqSync.outputFastq1 = new File(outputDir, s"$outputName.unmapped.R1.sync.fq.gz")

      // TODO: need some sanity check on whether R2 is really containing reads (e.g. Single End libraries)
      fastqSync.outputFastq2 = new File(outputDir, s"$outputName.unmapped.R2.sync.fq.gz")
      fastqSync.outputStats = new File(outputDir, s"$outputName.sync.stats")
      add(fastqSync)
      addSummarizable(fastqSync, "fastqsync")

      outputFiles += ("fastqsync_stats" -> fastqSync.outputStats)
      outputFiles += ("fastqsync_R1" -> fastqSync.outputFastq1)
      outputFiles += ("fastqsync_R2" -> fastqSync.outputFastq2)

      List(fastqSync.outputFastq1, fastqSync.outputFastq2)
    }.getOrElse(List(fastqR1, fastqR2).flatten)

    // start kraken
    val krakenAnalysis = new Kraken(this)
    krakenAnalysis.input = fastqFiles
    krakenAnalysis.output = new File(outputDir, s"$outputName.krkn.raw")

    krakenAnalysis.paired = fastqFiles.length == 2

    krakenAnalysis.classified_out = Some(new File(outputDir, s"$outputName.krkn.classified.fastq"))
    krakenAnalysis.unclassified_out = Some(new File(outputDir, s"$outputName.krkn.unclassified.fastq"))
    add(krakenAnalysis)

    outputFiles += ("kraken_output_raw" -> krakenAnalysis.output)
    outputFiles += ("kraken_classified_out" -> krakenAnalysis.classified_out.getOrElse(""))
    outputFiles += ("kraken_unclassified_out" -> krakenAnalysis.unclassified_out.getOrElse(""))

    // create kraken summary file
    val krakenReport = new KrakenReport(this)
    krakenReport.input = krakenAnalysis.output
    krakenReport.show_zeros = true
    krakenReport.output = new File(outputDir, s"$outputName.krkn.full")
    add(krakenReport)

    outputFiles += ("kraken_report_input" -> krakenReport.input)
    outputFiles += ("kraken_report_output" -> krakenReport.output)

    val krakenReportJSON = new KrakenReportToJson(this)
    krakenReportJSON.inputReport = krakenReport.output
    krakenReportJSON.output = new File(outputDir, s"$outputName.krkn.json")
    krakenReportJSON.skipNames = config("skipNames", default = false)
    add(krakenReportJSON)
    addSummarizable(krakenReportJSON, "krakenreport")

    outputFiles += ("kraken_report_json_input" -> krakenReportJSON.inputReport)
    outputFiles += ("kraken_report_json_output" -> krakenReportJSON.output)

    addSummaryJobs()
  }

  /** Location of summary file */
  def summaryFile = new File(outputDir, sampleId.getOrElse("sampleName_unknown") + ".gears.summary.json")

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map.empty

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = Map.empty ++
    (if (bamFile.isDefined) Map("input_bam" -> bamFile.get) else Map()) ++
    (if (fastqR1.isDefined) Map("input_R1" -> fastqR1.get) else Map()) ++
    outputFiles
}

/** This object give a default main method to the pipelines */
object Gears extends PipelineCommand