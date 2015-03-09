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

import java.util.Date
import java.io.File
import scala.math._

import org.broadinstitute.gatk.queue.QScript

import nl.lumc.sasc.biopet.core.{ SampleLibraryTag, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.{ Ln, Star, Stampy, Bowtie }
import nl.lumc.sasc.biopet.extensions.bwa.{ BwaSamse, BwaSampe, BwaAln, BwaMem }
import nl.lumc.sasc.biopet.extensions.{ Gsnap, Tophat }
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.tools.FastqSplitter
import nl.lumc.sasc.biopet.extensions.picard.{ MarkDuplicates, SortSam, MergeSamFiles, AddOrReplaceReadGroups }
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep

// TODO: documentation
class Mapping(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {

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

  /** location of summary file */
  def summaryFile = new File(outputDir, sampleId.getOrElse("x") + "-" + libId.getOrElse("x") + ".summary.json")

  /** File to add to the summary */
  def summaryFiles: Map[String, File] = Map("output_bamfile" -> finalBamFile, "input_R1" -> input_R1) ++
    (if (input_R2.isDefined) Map("input_R2" -> input_R2.get) else Map())

  /** Settings to add to summary */
  def summarySettings = Map(
    "skip_metrics" -> skipMetrics,
    "skip_flexiprep" -> skipFlexiprep,
    "skip_markduplicates" -> skipMarkduplicates,
    "aligner" -> aligner,
    "chunking" -> chunking,
    "numberChunks" -> numberChunks.getOrElse(1)
  )

  /** Will be executed before script */
  def init() {
    require(outputDir != null, "Missing output directory on mapping module")
    require(input_R1 != null, "Missing output directory on mapping module")
    require(sampleId.isDefined, "Missing sample ID on mapping module")
    require(libId.isDefined, "Missing library ID on mapping module")

    paired = input_R2.isDefined

    if (readgroupId == null) readgroupId = sampleId.get + "-" + libId.get
    else if (readgroupId == null) readgroupId = config("readgroup_id")

    if (outputName == null) outputName = readgroupId

    if (chunking) {
      if (numberChunks.isEmpty) {
        if (config.contains("numberchunks")) numberChunks = config("numberchunks", default = None)
        else {
          val chunkSize: Int = config("chunksize", 1 << 30)
          val filesize = if (input_R1.getName.endsWith(".gz") || input_R1.getName.endsWith(".gzip")) input_R1.length * 3
          else input_R1.length
          numberChunks = Option(ceil(filesize.toDouble / chunkSize).toInt)
        }
      }
      logger.debug("Chunks: " + numberChunks.getOrElse(1))
    }
  }

  /** Adds all jobs of the pipeline */
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
      if (file.endsWith(".gz")) file.substring(0, file.lastIndexOf(".gz"))
      else if (file.endsWith(".gzip")) file.substring(0, file.lastIndexOf(".gzip"))
      else file
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

      val outputBam = new File(chunkDir, outputName + ".bam")
      bamFiles :+= outputBam
      aligner match {
        case "bwa"        => addBwaMem(R1, R2, outputBam, deps)
        case "bwa-aln"    => addBwaAln(R1, R2, outputBam, deps)
        case "bowtie"     => addBowtie(R1, R2, outputBam, deps)
        case "gsnap"      => addGsnap(R1, R2, outputBam, deps)
        // TODO: make TopHat here accept multiple input files
        case "tophat"     => addTophat(R1, R2, outputBam, deps)
        case "stampy"     => addStampy(R1, R2, outputBam, deps)
        case "star"       => addStar(R1, R2, outputBam, deps)
        case "star-2pass" => addStar2pass(R1, R2, outputBam, deps)
        case _            => throw new IllegalStateException("Option aligner: '" + aligner + "' is not valid")
      }
      if (config("chunk_metrics", default = false))
        addAll(BamMetrics(this, outputBam, new File(chunkDir, "metrics")).functions)
    }
    if (!skipFlexiprep) {
      flexiprep.runFinalize(fastq_R1_output, fastq_R2_output)
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
      addSummaryQScript(flexiprep)
    }

    var bamFile = bamFiles.head
    if (!skipMarkduplicates) {
      bamFile = new File(outputDir, outputName + ".dedup.bam")
      val md = MarkDuplicates(this, bamFiles, bamFile)
      add(md)
      addSummarizable(md, "mark_duplicates")
    } else if (skipMarkduplicates && chunking) {
      val mergeSamFile = MergeSamFiles(this, bamFiles, outputDir)
      add(mergeSamFile)
      bamFile = mergeSamFile.output
    }

    if (!skipMetrics) {
      val bamMetrics = BamMetrics(this, bamFile, new File(outputDir, "metrics"))
      addAll(bamMetrics.functions)
      addSummaryQScript(bamMetrics)
    }

    add(Ln(this, swapExt(outputDir, bamFile, ".bam", ".bai"), swapExt(outputDir, finalBamFile, ".bam", ".bai")))
    add(Ln(this, bamFile, finalBamFile))
    outputFiles += ("finalBamFile" -> bamFile)

    if (config("generate_wig", default = false).asBoolean)
      addAll(Bam2Wig(this, finalBamFile).functions)

    addSummaryJobs
  }

  /**
   * Add bwa aln jobs
   * @param R1
   * @param R2
   * @param output
   * @param deps
   * @return
   */
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
      bwaSampe.r = getReadGroupBwa
      bwaSampe.output = swapExt(output.getParent, output, ".bam", ".sam")
      bwaSampe.isIntermediate = true
      add(bwaSampe)

      bwaSampe.output
    } else {
      val bwaSamse = new BwaSamse(this)
      bwaSamse.fastq = R1
      bwaSamse.sai = bwaAlnR1.output
      bwaSamse.r = getReadGroupBwa
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

  /**
   * Adds bwa mem jobs
   * @param R1
   * @param R2
   * @param output
   * @param deps
   * @return
   */
  def addBwaMem(R1: File, R2: File, output: File, deps: List[File]): File = {
    val bwaCommand = new BwaMem(this)
    bwaCommand.R1 = R1
    if (paired) bwaCommand.R2 = R2
    bwaCommand.deps = deps
    bwaCommand.R = Some(getReadGroupBwa)
    bwaCommand.output = swapExt(output.getParent, output, ".bam", ".sam")
    bwaCommand.isIntermediate = true
    add(bwaCommand)
    val sortSam = SortSam(this, bwaCommand.output, output)
    if (chunking || !skipMarkduplicates) sortSam.isIntermediate = true
    add(sortSam)
    sortSam.output
  }

  def addGsnap(R1: File, R2: File, output: File, deps: List[File]): File = {
    val gsnapCommand = new Gsnap(this)
    // FIXME: ideally we should only check for null ~ but apparently it's possible to get a File object with "" as the path
    gsnapCommand.input = List(R1, R2).filterNot(x => x == null || x.getPath == "")
    gsnapCommand.deps = deps
    gsnapCommand.output = swapExt(output.getParent, output, ".bam", ".sam")
    gsnapCommand.isIntermediate = true
    add(gsnapCommand)

    val sortSam = new SortSam(this)
    sortSam.input = gsnapCommand.output
    sortSam.output = output
    sortSam.sortOrder = "coordinate"
    sortSam.isIntermediate = chunking || !skipMarkduplicates
    add(sortSam)

    sortSam.output
  }

  def addTophat(R1: File, R2: File, output: File, deps: List[File]): File = {
    // TODO: merge mapped and unmapped BAM ~ also dealing with validation errors in the unmapped BAM
    val tophat = new Tophat(this)
    // FIXME: ideally we should only check for null ~ but apparently it's possible to get a File object with "" as the path
    tophat.R1 = tophat.R1 :+ R1
    if (R2 != null && R2.getPath != "") tophat.R2 = tophat.R2 :+ R2
    tophat.output_dir = new File(outputDir, "tophat_out")
    // always output BAM
    tophat.no_convert_bam = false
    tophat.deps = deps
    // always output BAM
    tophat.no_convert_bam = false
    add(tophat)

    val ln = new Ln(this)
    ln.input = tophat.outputAcceptedHits
    ln.output = swapExt(output.getParent, output, ".bam", ".raw.bam")
    add(ln)

    val sortSam = new SortSam(this)
    sortSam.input = ln.output
    sortSam.output = output
    sortSam.sortOrder = "coordinate"
    sortSam.isIntermediate = chunking || !skipMarkduplicates
    add(sortSam)

    sortSam.output
  }
  /**
   * Adds stampy jobs
   * @param R1
   * @param R2
   * @param output
   * @param deps
   * @return
   */
  def addStampy(R1: File, R2: File, output: File, deps: List[File]): File = {

    var RG: String = "ID:" + readgroupId + ","
    RG += "SM:" + sampleId.get + ","
    RG += "LB:" + libId.get + ","
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
    sortSam.output
  }

  /**
   * Adds bowtie jobs
   * @param R1
   * @param R2
   * @param output
   * @param deps
   * @return
   */
  def addBowtie(R1: File, R2: File, output: File, deps: List[File]): File = {
    val bowtie = new Bowtie(this)
    bowtie.R1 = R1
    if (paired) bowtie.R2 = Some(R2)
    bowtie.deps = deps
    bowtie.output = this.swapExt(output.getParent, output, ".bam", ".sam")
    bowtie.isIntermediate = true
    add(bowtie)
    return addAddOrReplaceReadGroups(bowtie.output, output)
  }

  /**
   * Adds Star jobs
   * @param R1
   * @param R2
   * @param output
   * @param deps
   * @return
   */
  def addStar(R1: File, R2: File, output: File, deps: List[File]): File = {
    val starCommand = Star(this, R1, if (paired) R2 else null, outputDir, isIntermediate = true, deps = deps)
    add(starCommand)
    addAddOrReplaceReadGroups(starCommand.outputSam, output)
  }

  /**
   * Adds Start 2 pass jobs
   * @param R1
   * @param R2
   * @param output
   * @param deps
   * @return
   */
  def addStar2pass(R1: File, R2: File, output: File, deps: List[File]): File = {
    val starCommand = Star._2pass(this, R1, if (paired) R2 else null, outputDir, isIntermediate = true, deps = deps)
    addAll(starCommand._2)
    addAddOrReplaceReadGroups(starCommand._1, output)
  }

  /**
   * Adds AddOrReplaceReadGroups
   * @param input
   * @param output
   * @return
   */
  def addAddOrReplaceReadGroups(input: File, output: File): File = {
    val addOrReplaceReadGroups = AddOrReplaceReadGroups(this, input, output)
    addOrReplaceReadGroups.createIndex = true

    addOrReplaceReadGroups.RGID = readgroupId
    addOrReplaceReadGroups.RGLB = libId.get
    addOrReplaceReadGroups.RGPL = platform
    addOrReplaceReadGroups.RGPU = platformUnit
    addOrReplaceReadGroups.RGSM = sampleId.get
    if (readgroupSequencingCenter.isDefined) addOrReplaceReadGroups.RGCN = readgroupSequencingCenter.get
    if (readgroupDescription.isDefined) addOrReplaceReadGroups.RGDS = readgroupDescription.get
    if (!skipMarkduplicates) addOrReplaceReadGroups.isIntermediate = true
    add(addOrReplaceReadGroups)

    addOrReplaceReadGroups.output
  }

  /** Returns readgroup for bwa */
  def getReadGroupBwa(): String = {
    var RG: String = "@RG\\t" + "ID:" + readgroupId + "\\t"
    RG += "LB:" + libId.get + "\\t"
    RG += "PL:" + platform + "\\t"
    RG += "PU:" + platformUnit + "\\t"
    RG += "SM:" + sampleId.get + "\\t"
    if (readgroupSequencingCenter.isDefined) RG += "CN:" + readgroupSequencingCenter.get + "\\t"
    if (readgroupDescription.isDefined) RG += "DS" + readgroupDescription.get + "\\t"
    if (readgroupDate != null) RG += "DT" + readgroupDate + "\\t"
    if (predictedInsertsize.isDefined) RG += "PI" + predictedInsertsize.get + "\\t"

    RG.substring(0, RG.lastIndexOf("\\t"))
  }
}

object Mapping extends PipelineCommand