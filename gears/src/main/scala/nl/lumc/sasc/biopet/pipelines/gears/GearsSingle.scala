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

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.core.BiopetQScript.InputFile
import nl.lumc.sasc.biopet.core.{ PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.{ Gzip, Zcat }
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by wyleung
 */
class GearsSingle(val parent: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "R1 reads in FastQ format", shortName = "R1", required = false)
  var fastqR1: List[File] = Nil

  @Input(doc = "R2 reads in FastQ format", shortName = "R2", required = false)
  var fastqR2: List[File] = Nil

  @Input(doc = "All unmapped reads will be extracted from this bam for analysis", shortName = "bam", required = false)
  var bamFile: Option[File] = None

  @Argument(required = false)
  var outputName: String = _

  lazy val krakenScript = if (config("gears_use_kraken", default = false)) Some(new GearsKraken(this)) else None
  lazy val centrifugeScript = if (config("gears_use_centrifuge", default = true)) Some(new GearsCentrifuge(this)) else None
  lazy val qiimeRatx = if (config("gears_use_qiime_rtax", default = false)) Some(new GearsQiimeRtax(this)) else None
  lazy val qiimeClosed = if (config("gears_use_qiime_closed", default = false)) Some(new GearsQiimeClosed(this)) else None
  lazy val qiimeOpen = if (config("gears_use_qiime_open", default = false)) Some(new GearsQiimeOpen(this)) else None
  lazy val seqCount = if (config("gears_use_seq_count", default = false)) Some(new GearsSeqCount(this)) else None

  /** Executed before running the script */
  def init(): Unit = {
    if (fastqR1.isEmpty && !bamFile.isDefined) Logging.addError("Please specify fastq-file(s) or bam file")
    if (fastqR1.nonEmpty == bamFile.isDefined) Logging.addError("Provide either a bam file or a R1/R2 file")
    if (fastqR2.nonEmpty && fastqR1.size != fastqR2.size) Logging.addError("R1 and R2 has not the same number of files")
    if (sampleId == null || sampleId == None) Logging.addError("Missing sample ID on GearsSingle module")

    if (outputName == null) {
      if (fastqR1.nonEmpty) outputName = fastqR1.headOption.map(_.getName
        .stripSuffix(".gz")
        .stripSuffix(".fastq")
        .stripSuffix(".fq"))
        .getOrElse("noName")
      else outputName = bamFile.map(_.getName.stripSuffix(".bam")).getOrElse("noName")
    }

    if (fastqR1.nonEmpty) {
      fastqR1.foreach(inputFiles :+= InputFile(_))
      fastqR2.foreach(inputFiles :+= InputFile(_))
    } else bamFile.foreach(inputFiles :+= InputFile(_))
  }

  override def reportClass = {
    val gears = new GearsSingleReport(this)
    gears.outputDir = new File(outputDir, "report")
    gears.summaryDbFile = summaryDbFile
    sampleId.foreach(gears.args += "sampleId" -> _)
    libId.foreach(gears.args += "libId" -> _)
    Some(gears)
  }

  protected var skipFlexiprep: Boolean = config("skip_flexiprep", default = false)

  protected def executeFlexiprep(r1: List[File], r2: List[File]): (File, Option[File]) = {
    val read1: File = if (r1.size == 1) r1.head else {
      val outputFile = new File(outputDir, "merged.R1.fq.gz")
      add(Zcat(this, r1) | new Gzip(this) > outputFile)
      outputFile
    }

    val read2: Option[File] = if (r2.size <= 1) r2.headOption else {
      val outputFile = new File(outputDir, "merged.R2.fq.gz")
      add(Zcat(this, r2) | new Gzip(this) > outputFile)
      Some(outputFile)
    }

    flexiprep.map { f =>
      f.inputR1 = read1
      f.inputR2 = read2
      f.sampleId = Some(sampleId.getOrElse("noSampleName"))
      f.libId = Some(libId.getOrElse("noLibName"))
      f.outputDir = new File(outputDir, "flexiprep")
      add(f)
      (f.fastqR1Qc, f.fastqR2Qc)
    }.getOrElse((read1, read2))
  }

  lazy protected val flexiprep: Option[Flexiprep] = if (!skipFlexiprep) {
    Some(new Flexiprep(this))
  } else None

  /** Method to add jobs */
  def biopetScript(): Unit = {
    val (r1, r2): (File, Option[File]) = (fastqR1, fastqR2, bamFile) match {
      case (r1, _, _) if r1.nonEmpty => executeFlexiprep(r1, fastqR2)
      case (_, _, Some(bam)) =>
        val extract = new ExtractUnmappedReads(this)
        extract.outputDir = outputDir
        extract.bamFile = bam
        extract.outputName = outputName
        add(extract)
        executeFlexiprep(extract.fastqUnmappedR1 :: Nil, extract.fastqUnmappedR2.toList)
      case _ => throw new IllegalArgumentException("Missing input files")
    }

    lazy val combinedFastq = {
      r2 match {
        case Some(r2) =>
          val combineReads = new CombineReads(this)
          combineReads.outputDir = new File(outputDir, "combine_reads")
          combineReads.fastqR1 = r1
          combineReads.fastqR2 = r2
          add(combineReads)
          combineReads.combinedFastq
        case _ => r1
      }
    }

    krakenScript foreach { kraken =>
      kraken.outputDir = new File(outputDir, "kraken")
      kraken.fastqR1 = r1
      kraken.fastqR2 = r2
      kraken.outputName = outputName
      add(kraken)
    }

    centrifugeScript foreach { centrifuge =>
      centrifuge.outputDir = new File(outputDir, "centrifuge")
      centrifuge.fastqR1 = r1
      centrifuge.fastqR2 = r2
      centrifuge.outputName = outputName
      add(centrifuge)
      outputFiles += "centrifuge_output" -> centrifuge.centrifugeOutput
    }

    qiimeRatx foreach { qiimeRatx =>
      qiimeRatx.outputDir = new File(outputDir, "qiime_rtax")
      qiimeRatx.fastqR1 = r1
      qiimeRatx.fastqR2 = r2
      add(qiimeRatx)
    }

    qiimeClosed foreach { qiimeClosed =>
      qiimeClosed.outputDir = new File(outputDir, "qiime_closed")
      qiimeClosed.fastqInput = combinedFastq
      add(qiimeClosed)
      outputFiles += "qiime_closed_otu_table" -> qiimeClosed.otuTable
    }

    qiimeOpen foreach { qiimeOpen =>
      qiimeOpen.outputDir = new File(outputDir, "qiime_open")
      qiimeOpen.fastqInput = combinedFastq
      add(qiimeOpen)
      outputFiles += "qiime_open_otu_table" -> qiimeOpen.otuTable
    }

    seqCount.foreach { seqCount =>
      seqCount.fastqInput = combinedFastq
      seqCount.outputDir = new File(outputDir, "seq_count")
      add(seqCount)
      outputFiles += "seq_count_count_file" -> seqCount.countFile
    }

    addSummaryJobs()
  }

  /** Pipeline settings shown in the summary file */
  def summarySettings: Map[String, Any] = Map(
    "skip_flexiprep" -> skipFlexiprep,
    "gears_use_kraken" -> krakenScript.isDefined,
    "gear_use_qiime_rtax" -> qiimeRatx.isDefined,
    "gear_use_qiime_closed" -> qiimeClosed.isDefined,
    "gear_use_qiime_open" -> qiimeOpen.isDefined
  )

  /** Statistics shown in the summary file */
  def summaryFiles: Map[String, File] = Map.empty ++
    (if (bamFile.isDefined) Map("input_bam" -> bamFile.get) else Map()) ++
    fastqR1.zipWithIndex.map(x => s"input_R1_${x._2}" -> x._1) ++
    fastqR2.zipWithIndex.map(x => s"input_R2_${x._2}" -> x._1) ++
    outputFiles
}

/** This object give a default main method to the pipelines */
object GearsSingle extends PipelineCommand