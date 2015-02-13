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
package nl.lumc.sasc.biopet.pipelines.mapping

import nl.lumc.sasc.biopet.core.config.Configurable
import java.io.File
import java.util.Date
import nl.lumc.sasc.biopet.core.{ BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.extensions.{ Ln, Star, Stampy, Bowtie }
import nl.lumc.sasc.biopet.extensions.bwa.{ BwaSamse, BwaSampe, BwaAln, BwaMem }
import nl.lumc.sasc.biopet.tools.FastqSplitter
import nl.lumc.sasc.biopet.extensions.picard.{ MarkDuplicates, SortSam, MergeSamFiles, AddOrReplaceReadGroups }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument, ClassType }
import scala.math._

class Mapping(val root: Configurable) extends QScript with BiopetQScript {
  def this() = this(null)

  @Input(doc = "R1 fastq file", shortName = "R1", required = true)
  var input_R1: File = _

  @Input(doc = "R2 fastq file", shortName = "R2", required = false)
  var input_R2: Option[File] = None

  /** Output name */
  var outputName: String = _

  /** Skip flexiprep */
  protected var skipFlexiprep: Boolean = config("skip_flexiprep", default = false)

  /** Skip mark duplicates */
  protected var skipMarkduplicates: Boolean = config("skip_markduplicates", default = false)

  /** Skip metrics */
  protected var skipMetrics: Boolean = config("skip_metrics", default = false)

  /** Aligner */
  protected var aligner: String = config("aligner", default = "bwa")

  /** Reference */
  protected var reference: File = config("reference")

  /** Number of chunks, when not defined pipeline will automatic calculate number of chunks */
  protected var numberChunks: Option[Int] = config("number_chunks")

  /** Enable chunking */
  protected var chunking: Boolean = config("chunking", numberChunks.getOrElse(1) > 1)

  // Readgroup items
  /** Readgroup ID */
  protected var readgroupId: String = _

  // TODO: hide sampleId and libId from the command line so they do not interfere with our config values

  /** Readgroup Library */
  @Argument(doc = "Library ID", shortName = "library", required = true)
  var libId: String = _

  /**Readgroup sample */
  @Argument(doc = "Sample ID", shortName = "sample", required = true)
  var sampleId: String = _

  /** Readgroup Platform */
  protected var platform: String = config("platform", default = "illumina")

  /** Readgroup platform unit */
  protected var platformUnit: String = config("platform_unit", default = "na")

  /** Readgroup sequencing center */
  protected var readgroupSequencingCenter: Option[String] = config("readgroup_sequencing_center")

  /** Readgroup description */
  protected var readgroupDescription: Option[String] = config("readgroup_description")

  /** Readgroup sequencing date */
  protected var readgroupDate: Date = _

  /** Readgroup predicted insert size */
  protected var predictedInsertsize: Option[Int] = config("predicted_insertsize")

  protected var paired: Boolean = false
  val flexiprep = new Flexiprep(this)
  def finalBamFile: File = new File(outputDir, outputName + ".final.bam")

  def init() {
    require(outputDir != null, "Missing output directory on mapping module")
    require(input_R1 != null, "Missing output directory on mapping module")
    require(sampleId != null, "Missing sample ID on mapping module")
    require(libId != null, "Missing library ID on mapping module")

    paired = input_R2.isDefined

    if (readgroupId == null && sampleId != null && libId != null) readgroupId = sampleId + "-" + libId
    else if (readgroupId == null) readgroupId = config("readgroup_id")

    if (outputName == null) outputName = readgroupId

    if (chunking) {
      if (numberChunks.isEmpty) {
        if (config.contains("numberchunks")) numberChunks = config("numberchunks", default = None)
        else {
          val chunkSize: Int = config("chunksize", (1 << 30))
          val filesize = if (input_R1.getName.endsWith(".gz") || input_R1.getName.endsWith(".gzip")) input_R1.length * 3
          else input_R1.length
          numberChunks = Option(ceil(filesize.toDouble / chunkSize).toInt)
        }
      }
      logger.debug("Chunks: " + numberChunks.getOrElse(1))
    }
  }

  def biopetScript() {
    if (!skipFlexiprep) {
      flexiprep.outputDir = new File(outputDir, "flexiprep")
      flexiprep.input_R1 = input_R1
      flexiprep.input_R2 = input_R2
      flexiprep.sampleId = this.sampleId
      flexiprep.libId = this.libId
      flexiprep.init
      flexiprep.runInitialJobs
    }
    var bamFiles: List[File] = Nil
    var fastq_R1_output: List[File] = Nil
    var fastq_R2_output: List[File] = Nil

    def removeGz(file: String): String = {
      if (file.endsWith(".gz")) return file.substring(0, file.lastIndexOf(".gz"))
      else if (file.endsWith(".gzip")) return file.substring(0, file.lastIndexOf(".gzip"))
      else return file
    }
    var chunks: Map[File, (String, String)] = Map()
    if (chunking) for (t <- 1 to numberChunks.getOrElse(1)) {
      val chunkDir = new File(outputDir, "chunks" + File.separator + t)
      chunks += (chunkDir -> (removeGz(chunkDir + input_R1.getName),
        if (paired) removeGz(chunkDir + input_R2.get.getName) else ""))
    }
    else chunks += (outputDir -> (
      flexiprep.extractIfNeeded(input_R1, flexiprep.outputDir),
      if (paired) flexiprep.extractIfNeeded(input_R2.get, flexiprep.outputDir) else "")
    )

    if (chunking) {
      val fastSplitter_R1 = new FastqSplitter(this)
      fastSplitter_R1.input = input_R1
      for ((chunkDir, fastqfile) <- chunks) fastSplitter_R1.output :+= fastqfile._1
      fastSplitter_R1.isIntermediate = true
      add(fastSplitter_R1)

      if (paired) {
        val fastSplitter_R2 = new FastqSplitter(this)
        fastSplitter_R2.input = input_R2.get
        for ((chunkDir, fastqfile) <- chunks) fastSplitter_R2.output :+= fastqfile._2
        fastSplitter_R2.isIntermediate = true
        add(fastSplitter_R2)
      }
    }

    for ((chunkDir, fastqfile) <- chunks) {
      var R1 = fastqfile._1
      var R2 = fastqfile._2
      var deps: List[File] = Nil
      if (!skipFlexiprep) {
        val flexiout = flexiprep.runTrimClip(R1, R2, new File(chunkDir, "flexiprep"), chunkDir)
        logger.debug(chunkDir + " - " + flexiout)
        R1 = flexiout._1
        if (paired) R2 = flexiout._2
        deps = flexiout._3
        fastq_R1_output :+= R1
        fastq_R2_output :+= R2
      }

      val outputBam = new File(chunkDir + outputName + ".bam")
      bamFiles :+= outputBam
      aligner match {
        case "bwa"        => addBwaMem(R1, R2, outputBam, deps)
        case "bwa-aln"    => addBwaAln(R1, R2, outputBam, deps)
        case "bowtie"     => addBowtie(R1, R2, outputBam, deps)
        case "stampy"     => addStampy(R1, R2, outputBam, deps)
        case "star"       => addStar(R1, R2, outputBam, deps)
        case "star-2pass" => addStar2pass(R1, R2, outputBam, deps)
        case _            => throw new IllegalStateException("Option Aligner: '" + aligner + "' is not valid")
      }
      if (config("chunk_metrics", default = false))
        addAll(BamMetrics(this, outputBam, chunkDir + "metrics/").functions)
    }
    if (!skipFlexiprep) {
      flexiprep.runFinalize(fastq_R1_output, fastq_R2_output)
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
    }

    var bamFile = bamFiles.head
    if (!skipMarkduplicates) {
      bamFile = new File(outputDir, outputName + ".dedup.bam")
      add(MarkDuplicates(this, bamFiles, bamFile))
    } else if (skipMarkduplicates && chunking) {
      val mergeSamFile = MergeSamFiles(this, bamFiles, outputDir)
      add(mergeSamFile)
      bamFile = mergeSamFile.output
    }

    if (!skipMetrics) addAll(BamMetrics(this, bamFile, new File(outputDir, "metrics")).functions)

    add(Ln(this, swapExt(outputDir, bamFile, ".bam", ".bai"), swapExt(outputDir, finalBamFile, ".bam", ".bai")))
    add(Ln(this, bamFile, finalBamFile))
    outputFiles += ("finalBamFile" -> bamFile)
  }

  def addBwaAln(R1: File, R2: File, output: File, deps: List[File]): File = {
    val bwaAlnR1 = new BwaAln(this)
    bwaAlnR1.fastq = R1
    bwaAlnR1.deps = deps
    bwaAlnR1.output = swapExt(output.getParent, output, ".bam", ".R1.sai")
    bwaAlnR1.isIntermediate = true
    add(bwaAlnR1)

    val samFile: File = if (paired) {
      val bwaAlnR2 = new BwaAln(this)
      bwaAlnR2.fastq = R2
      bwaAlnR2.deps = deps
      bwaAlnR2.output = swapExt(output.getParent, output, ".bam", ".R2.sai")
      bwaAlnR2.isIntermediate = true
      add(bwaAlnR2)

      val bwaSampe = new BwaSampe(this)
      bwaSampe.fastqR1 = R1
      bwaSampe.fastqR2 = R2
      bwaSampe.saiR1 = bwaAlnR1.output
      bwaSampe.saiR2 = bwaAlnR2.output
      bwaSampe.r = getReadGroup
      bwaSampe.output = swapExt(output.getParent, output, ".bam", ".sam")
      bwaSampe.isIntermediate = true
      add(bwaSampe)

      bwaSampe.output
    } else {
      val bwaSamse = new BwaSamse(this)
      bwaSamse.fastq = R1
      bwaSamse.sai = bwaAlnR1.output
      bwaSamse.r = getReadGroup
      bwaSamse.output = swapExt(output.getParent, output, ".bam", ".sam")
      bwaSamse.isIntermediate = true
      add(bwaSamse)

      bwaSamse.output
    }

    val sortSam = SortSam(this, samFile, output)
    if (chunking || !skipMarkduplicates) sortSam.isIntermediate = true
    add(sortSam)
    return sortSam.output
  }

  def addBwaMem(R1: File, R2: File, output: File, deps: List[File]): File = {
    val bwaCommand = new BwaMem(this)
    bwaCommand.R1 = R1
    if (paired) bwaCommand.R2 = R2
    bwaCommand.deps = deps
    bwaCommand.R = Some(getReadGroup)
    bwaCommand.output = swapExt(output.getParent, output, ".bam", ".sam")
    bwaCommand.isIntermediate = true
    add(bwaCommand)
    val sortSam = SortSam(this, bwaCommand.output, output)
    if (chunking || !skipMarkduplicates) sortSam.isIntermediate = true
    add(sortSam)
    return sortSam.output
  }

  def addStampy(R1: File, R2: File, output: File, deps: List[File]): File = {

    var RG: String = "ID:" + readgroupId + ","
    RG += "SM:" + sampleId + ","
    RG += "LB:" + libId + ","
    if (readgroupDescription != null) RG += "DS" + readgroupDescription + ","
    RG += "PU:" + platformUnit + ","
    if (predictedInsertsize.getOrElse(0) > 0) RG += "PI:" + predictedInsertsize.get + ","
    if (readgroupSequencingCenter.isDefined) RG += "CN:" + readgroupSequencingCenter.get + ","
    if (readgroupDate != null) RG += "DT:" + readgroupDate + ","
    RG += "PL:" + platform

    val stampyCmd = new Stampy(this)
    stampyCmd.R1 = R1
    if (paired) stampyCmd.R2 = R2
    stampyCmd.deps = deps
    stampyCmd.readgroup = RG
    stampyCmd.sanger = true
    stampyCmd.output = this.swapExt(output.getParent, output, ".bam", ".sam")
    stampyCmd.isIntermediate = true
    add(stampyCmd)
    val sortSam = SortSam(this, stampyCmd.output, output)
    if (chunking || !skipMarkduplicates) sortSam.isIntermediate = true
    add(sortSam)
    return sortSam.output
  }

  def addBowtie(R1: File, R2: File, output: File, deps: List[File]): File = {
    val bowtie = new Bowtie(this)
    bowtie.R1 = R1
    if (paired) bowtie.R2 = R2
    bowtie.deps = deps
    bowtie.output = this.swapExt(output.getParent, output, ".bam", ".sam")
    bowtie.isIntermediate = true
    add(bowtie)
    return addAddOrReplaceReadGroups(bowtie.output, output)
  }

  def addStar(R1: File, R2: File, output: File, deps: List[File]): File = {
    val starCommand = Star(this, R1, if (paired) R2 else null, outputDir, isIntermediate = true, deps = deps)
    add(starCommand)
    return addAddOrReplaceReadGroups(starCommand.outputSam, output)
  }

  def addStar2pass(R1: File, R2: File, output: File, deps: List[File]): File = {
    val starCommand = Star._2pass(this, R1, if (paired) R2 else null, outputDir, isIntermediate = true, deps = deps)
    addAll(starCommand._2)
    return addAddOrReplaceReadGroups(starCommand._1, output)
  }

  def addAddOrReplaceReadGroups(input: File, output: File): File = {
    val addOrReplaceReadGroups = AddOrReplaceReadGroups(this, input, output)
    addOrReplaceReadGroups.createIndex = true

    addOrReplaceReadGroups.RGID = readgroupId
    addOrReplaceReadGroups.RGLB = libId
    addOrReplaceReadGroups.RGPL = platform
    addOrReplaceReadGroups.RGPU = platformUnit
    addOrReplaceReadGroups.RGSM = sampleId
    if (readgroupSequencingCenter.isDefined) addOrReplaceReadGroups.RGCN = readgroupSequencingCenter.get
    if (readgroupDescription.isDefined) addOrReplaceReadGroups.RGDS = readgroupDescription.get
    if (!skipMarkduplicates) addOrReplaceReadGroups.isIntermediate = true
    add(addOrReplaceReadGroups)

    return addOrReplaceReadGroups.output
  }

  def getReadGroup(): String = {
    var RG: String = "@RG\\t" + "ID:" + readgroupId + "\\t"
    RG += "LB:" + libId + "\\t"
    RG += "PL:" + platform + "\\t"
    RG += "PU:" + platformUnit + "\\t"
    RG += "SM:" + sampleId + "\\t"
    if (readgroupSequencingCenter.isDefined) RG += "CN:" + readgroupSequencingCenter.get + "\\t"
    if (readgroupDescription.isDefined) RG += "DS" + readgroupDescription.get + "\\t"
    if (readgroupDate != null) RG += "DT" + readgroupDate + "\\t"
    if (predictedInsertsize.isDefined) RG += "PI" + predictedInsertsize.get + "\\t"

    return RG.substring(0, RG.lastIndexOf("\\t"))
  }
}

object Mapping extends PipelineCommand