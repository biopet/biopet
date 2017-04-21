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
import java.util.Date

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.bowtie.{ Bowtie, Bowtie2 }
import nl.lumc.sasc.biopet.extensions.bwa.{ BwaAln, BwaMem, BwaSampe, BwaSamse }
import nl.lumc.sasc.biopet.extensions.gmap.Gsnap
import nl.lumc.sasc.biopet.extensions.hisat.Hisat2
import nl.lumc.sasc.biopet.extensions.picard.{ AddOrReplaceReadGroups, MarkDuplicates, MergeSamFiles, ReorderSam, SortSam }
import nl.lumc.sasc.biopet.extensions.tools.FastqSplitter
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.pipelines.bammetrics.BamMetrics
import nl.lumc.sasc.biopet.pipelines.bamtobigwig.Bam2Wig
import nl.lumc.sasc.biopet.pipelines.flexiprep.Flexiprep
import nl.lumc.sasc.biopet.pipelines.gears.GearsSingle
import nl.lumc.sasc.biopet.pipelines.mapping.scripts.TophatRecondition
import nl.lumc.sasc.biopet.utils.textToSize
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

import scala.math._

/**
 * This pipeline doing a alignment to a given reference genome
 */
class Mapping(val parent: Configurable) extends QScript with SummaryQScript with SampleLibraryTag with Reference {

  def this() = this(null)

  @Input(doc = "R1 fastq file", shortName = "R1", fullName = "inputR1", required = true)
  var inputR1: File = _

  @Input(doc = "R2 fastq file", shortName = "R2", fullName = "inputR2", required = false)
  var inputR2: Option[File] = None

  /** Output name */
  var outputName: String = _

  /** Skip flexiprep */
  protected var skipFlexiprep: Boolean = config("skip_flexiprep", default = false)

  /** Skip mark duplicates */
  protected var skipMarkduplicates: Boolean = config("skip_markduplicates", default = false)

  /** Skip metrics */
  protected var skipMetrics: Boolean = config("skip_metrics", default = false)

  /** Aligner */
  protected var aligner: String = config("aligner", default = "bwa-mem")

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
  protected var platformUnit: Option[String] = config("platform_unit")

  /** Readgroup sequencing center */
  protected var readgroupSequencingCenter: Option[String] = config("readgroup_sequencing_center")

  /** Readgroup library */
  protected var readgroupLibrary: Option[String] = config("readgroup_library")

  /** Readgroup description */
  protected var readgroupDescription: Option[String] = config("readgroup_description")

  /** Readgroup sequencing date */
  protected var readgroupDate: Date = _

  /** Readgroup predicted insert size */
  protected var predictedInsertsize: Option[Int] = config("predicted_insertsize")

  val keepFinalBamFile: Boolean = config("keep_final_bam_file", default = true)

  protected var paired: Boolean = false
  val flexiprep = new Flexiprep(this)
  def mergedBamFile = new File(outputDir, outputName + ".bam")
  def finalBamFile: File = if (skipMarkduplicates) mergedBamFile
  else new File(outputDir, outputName + ".dedup.bam")

  override def defaults: Map[String, Any] = Map(
    "gsnap" -> Map("batch" -> 4),
    "star" -> Map("outsamunmapped" -> "Within")
  )

  override def fixedValues: Map[String, Any] = Map(
    "gsnap" -> Map("format" -> "sam"),
    "bowtie" -> Map("sam" -> true)
  )

  /** File to add to the summary */
  def summaryFiles: Map[String, File] = Map("output_bam" -> finalBamFile, "input_R1" -> inputR1,
    "reference" -> referenceFasta()) ++
    (if (inputR2.isDefined) Map("input_R2" -> inputR2.get) else Map()) ++
    (bam2wig match {
      case Some(b) => Map(
        "output_wigle" -> b.outputWigleFile,
        "output_tdf" -> b.outputTdfFile,
        "output_bigwig" -> b.outputBwFile)
      case _ => Map()
    })

  /** Settings to add to summary */
  def summarySettings: Map[String, Any] = Map(
    "skip_metrics" -> skipMetrics,
    "skip_flexiprep" -> skipFlexiprep,
    "skip_markduplicates" -> skipMarkduplicates,
    "paired" -> inputR2.isDefined,
    "aligner" -> aligner,
    "chunking" -> chunking,
    "number_of_chunks" -> (if (chunking) numberChunks.getOrElse(1) else None)
  ) ++ (if (parent == null) Map("reference" -> referenceSummary) else Map())

  override def reportClass: Some[MappingReport] = {
    val mappingReport = new MappingReport(this)
    mappingReport.outputDir = new File(outputDir, "report")
    mappingReport.summaryDbFile = summaryDbFile
    mappingReport.args = Map(
      "sampleId" -> sampleId.getOrElse("."),
      "libId" -> libId.getOrElse("."))
    Some(mappingReport)
  }

  /** Will be executed before script */
  def init() {
    require(outputDir != null, "Missing output directory on mapping module")
    require(inputR1 != null, "Missing inputR1 on mapping module")
    require(sampleId.isDefined, "Missing sample ID on mapping module")
    require(libId.isDefined, "Missing library ID on mapping module")
    if (inputR1.exists() && inputR1.length() == 0) logger.warn(s"Input R1 is a empty file: $inputR1")
    inputR2.foreach(r => if (r.exists() && r.length() == 0) logger.warn(s"Input R2 is a empty file: $r"))

    inputFiles :+= new InputFile(inputR1)
    inputR2.foreach(inputFiles :+= new InputFile(_))

    paired = inputR2.isDefined

    if (readgroupId == null)
      readgroupId = config("readgroup_id", default = sampleId.get + "-" + libId.get)

    if (outputName == null) outputName = readgroupId

    if (chunking) {
      if (numberChunks.isEmpty) {
        if (config.contains("numberchunks")) numberChunks = config("numberchunks", default = None)
        else {
          val chunkSize: String = config("chunksize", default = "5G")
          val filesize = if (inputR1.getName.endsWith(".gz") || inputR1.getName.endsWith(".gzip")) inputR1.length * 3
          else inputR1.length
          numberChunks = Some(ceil(filesize.toDouble / textToSize(chunkSize)).toInt)
          if (numberChunks == Some(0)) numberChunks = Some(1)
        }
      }
      logger.debug("Chunks: " + numberChunks.getOrElse(1))
      if (numberChunks.getOrElse(1) <= 1) chunking = false
    }
  }

  /** Adds all jobs of the pipeline */
  def biopetScript() {
    if (!skipFlexiprep) {
      flexiprep.outputDir = new File(outputDir, "flexiprep")
      flexiprep.inputR1 = inputR1
      flexiprep.inputR2 = inputR2
      flexiprep.sampleId = this.sampleId
      flexiprep.libId = this.libId
      flexiprep.init()
      flexiprep.runInitialJobs()
    }
    var bamFiles: List[File] = Nil
    var fastqR1Output: List[File] = Nil
    var fastqR2Output: List[File] = Nil

    val chunks: Map[File, (File, Option[File])] = {
      if (chunking) (for (t <- 1 to numberChunks.getOrElse(1)) yield {
        val chunkDir = new File(outputDir, "chunks" + File.separator + t)
        chunkDir -> (new File(chunkDir, inputR1.getName),
          if (paired) Some(new File(chunkDir, inputR2.get.getName)) else None)
      }).toMap
      else if (skipFlexiprep) Map(outputDir -> (inputR1, if (paired) inputR2 else None))
      else Map(outputDir -> (flexiprep.inputR1, flexiprep.inputR2))
    }

    if (chunking) {
      val fastSplitterR1 = new FastqSplitter(this)
      fastSplitterR1.input = inputR1
      for ((chunkDir, fastqfile) <- chunks) fastSplitterR1.output :+= fastqfile._1
      fastSplitterR1.isIntermediate = true
      add(fastSplitterR1)

      if (paired) {
        val fastSplitterR2 = new FastqSplitter(this)
        fastSplitterR2.input = inputR2.get
        for ((chunkDir, fastqfile) <- chunks) fastSplitterR2.output :+= fastqfile._2.get
        fastSplitterR2.isIntermediate = true
        add(fastSplitterR2)
      }
    }

    for ((chunkDir, fastqfile) <- chunks) {
      var R1 = fastqfile._1
      var R2 = fastqfile._2
      if (!skipFlexiprep) {
        val flexiout = flexiprep.runTrimClip(R1, R2, new File(chunkDir, "flexiprep"), chunkDir)
        logger.debug(chunkDir + " - " + flexiout)
        R1 = flexiout._1
        if (paired) R2 = flexiout._2
        fastqR1Output :+= R1
        R2.foreach(R2 => fastqR2Output :+= R2)
      }

      val outputBam = new File(chunkDir, outputName + ".bam")
      bamFiles :+= outputBam
      aligner match {
        case "bwa-mem"    => addBwaMem(R1, R2, outputBam)
        case "bwa-aln"    => addBwaAln(R1, R2, outputBam)
        case "bowtie"     => addBowtie(R1, R2, outputBam)
        case "bowtie2"    => addBowtie2(R1, R2, outputBam)
        case "gsnap"      => addGsnap(R1, R2, outputBam)
        case "hisat2"     => addHisat2(R1, R2, outputBam)
        // TODO: make TopHat here accept multiple input files
        case "tophat"     => addTophat(R1, R2, outputBam)
        case "stampy"     => addStampy(R1, R2, outputBam)
        case "star"       => addStar(R1, R2, outputBam)
        case "star-2pass" => addStar2pass(R1, R2, outputBam)
        case _            => throw new IllegalStateException("Option aligner: '" + aligner + "' is not valid")
      }
      if (chunking && numberChunks.getOrElse(1) > 1 && config("chunk_metrics", default = false))
        addAll(BamMetrics(this, outputBam, new File(chunkDir, "metrics"), sampleId, libId).functions)
    }
    if (!skipFlexiprep) {
      flexiprep.runFinalize(fastqR1Output, fastqR2Output)
      addAll(flexiprep.functions) // Add function of flexiprep to curent function pool
      addSummaryQScript(flexiprep)
    }

    var bamFile = bamFiles.head

    if (!chunking) require(bamFile == mergedBamFile)
    else {
      val mergeSamFile = MergeSamFiles(this, bamFiles, mergedBamFile)
      mergeSamFile.isIntermediate = !keepFinalBamFile || !skipMarkduplicates
      add(mergeSamFile)
      bamFile = mergeSamFile.output
    }

    if (!skipMarkduplicates) {
      bamFile = new File(outputDir, outputName + ".dedup.bam")
      val md = MarkDuplicates(this, mergedBamFile :: Nil, finalBamFile)
      md.isIntermediate = !keepFinalBamFile
      add(md)
      addSummarizable(md, "mark_duplicates")
    }

    if (!skipMetrics) {
      val bamMetrics = BamMetrics(this, finalBamFile, new File(outputDir, "metrics"), sampleId, libId)
      addAll(bamMetrics.functions)
      addSummaryQScript(bamMetrics)
    }

    outputFiles += ("finalBamFile" -> finalBamFile)

    if (config("unmapped_to_gears", default = false).asBoolean) {
      val gears = new GearsSingle(this)
      gears.bamFile = Some(finalBamFile)
      gears.sampleId = sampleId
      gears.libId = libId
      gears.outputDir = new File(outputDir, "gears")
      add(gears)
    }

    bam2wig.foreach(add(_))

    addSummaryJobs()
  }

  protected lazy val bam2wig: Option[Bam2Wig] = if (config("generate_wig", default = false)) {
    Some(Bam2Wig(this, finalBamFile))
  } else None

  /** Add bwa aln jobs */
  def addBwaAln(R1: File, R2: Option[File], output: File): File = {
    val bwaAlnR1 = new BwaAln(this)
    bwaAlnR1.fastq = R1
    bwaAlnR1.output = swapExt(output.getParent, output, ".bam", ".R1.sai")
    bwaAlnR1.isIntermediate = true
    add(bwaAlnR1)

    val samFile: File = if (paired) {
      val bwaAlnR2 = new BwaAln(this)
      bwaAlnR2.fastq = R2.get
      bwaAlnR2.output = swapExt(output.getParent, output, ".bam", ".R2.sai")
      bwaAlnR2.isIntermediate = true
      add(bwaAlnR2)

      val bwaSampe = new BwaSampe(this)
      bwaSampe.fastqR1 = R1
      bwaSampe.fastqR2 = R2.get
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
    sortSam.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(sortSam)
    sortSam.output
  }

  /** Adds bwa mem jobs */
  def addBwaMem(R1: File, R2: Option[File], output: File): File = {
    val bwaCommand = new BwaMem(this)
    bwaCommand.R1 = R1
    if (paired) bwaCommand.R2 = R2.get
    bwaCommand.R = Some(getReadGroupBwa)
    val sortSam = new SortSam(this)
    sortSam.output = output
    val pipe = bwaCommand | sortSam
    pipe.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    pipe.threadsCorrection = -1
    add(pipe)
    output
  }

  def addGsnap(R1: File, R2: Option[File], output: File): File = {
    val gsnapCommand = new Gsnap(this)
    gsnapCommand.input = if (paired) List(R1, R2.get) else List(R1)
    gsnapCommand.output = swapExt(output.getParentFile, output, ".bam", ".sam")

    val reorderSam = new ReorderSam(this)
    reorderSam.input = gsnapCommand.output
    reorderSam.output = swapExt(output.getParentFile, output, ".sorted.bam", ".reordered.bam")

    val ar = addAddOrReplaceReadGroups(reorderSam.output, output)
    val pipe = new BiopetFifoPipe(this, gsnapCommand :: ar._1 :: reorderSam :: Nil)
    pipe.threadsCorrection = -2
    pipe.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(pipe)
    ar._2
  }

  def addHisat2(R1: File, R2: Option[File], output: File): File = {
    val hisat2 = new Hisat2(this)
    hisat2.R1 = R1
    hisat2.R2 = R2
    hisat2.rgId = Some(readgroupId)
    hisat2.rg +:= s"PL:$platform"
    platformUnit.foreach(x => hisat2.rg +:= s"PU:$x")
    libId match {
      case Some(id)  => hisat2.rg +:= s"LB:$id"
      case otherwise => ;
    }
    sampleId match {
      case Some(id)  => hisat2.rg +:= s"SM:$id"
      case otherwise => ;
    }

    val sortSam = new SortSam(this)
    sortSam.output = output
    val pipe = hisat2 | sortSam
    pipe.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    pipe.threadsCorrection = 1
    add(pipe)

    output
  }

  def addTophat(R1: File, R2: Option[File], output: File): File = {
    // TODO: merge mapped and unmapped BAM ~ also dealing with validation errors in the unmapped BAM
    val tophat = new Tophat(this)
    tophat.R1 = tophat.R1 :+ R1
    if (paired) tophat.R2 = tophat.R2 :+ R2.get
    tophat.outputDir = new File(outputDir, "tophat_out")
    // always output BAM
    tophat.noConvertBam = false
    // and always keep input ordering
    tophat.keepFastaOrder = true
    add(tophat)

    // fix unmapped file coordinates
    val fixedUnmapped = new File(tophat.outputDir, "unmapped_fixup.sam")
    val fixer = new TophatRecondition(this)
    fixer.inputBam = tophat.outputAcceptedHits
    fixer.outputSam = fixedUnmapped.getAbsoluteFile
    fixer.isIntermediate = true
    add(fixer)

    // sort fixed SAM file
    val sorter = SortSam(this, fixer.outputSam, new File(tophat.outputDir, "unmapped_fixup.sorted.bam"))
    sorter.sortOrder = "coordinate"
    sorter.isIntermediate = true
    add(sorter)

    // merge with mapped file
    val mergeSamFile = MergeSamFiles(this, List(tophat.outputAcceptedHits, sorter.output),
      new File(tophat.outputDir, "fixed_merged.bam"), sortOrder = "coordinate")
    mergeSamFile.createIndex = true
    mergeSamFile.isIntermediate = true
    add(mergeSamFile)

    // make sure header coordinates are correct
    val reorderSam = new ReorderSam(this)
    reorderSam.input = mergeSamFile.output
    reorderSam.output = swapExt(output.getParent, output, ".merge.bam", ".reordered.bam")
    reorderSam.isIntermediate = true
    add(reorderSam)

    val ar = addAddOrReplaceReadGroups(reorderSam.output, output)
    ar._1.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(ar._1)
    ar._2
  }

  /** Adds stampy jobs */
  def addStampy(R1: File, R2: Option[File], output: File): File = {

    var RG: String = "ID:" + readgroupId + ","
    RG += "SM:" + sampleId.get + ","
    RG += "LB:" + libId.get + ","
    if (readgroupDescription != null) RG += "DS" + readgroupDescription + ","
    platformUnit.foreach(x => RG += "PU:" + x + ",")
    if (predictedInsertsize.getOrElse(0) > 0) RG += "PI:" + predictedInsertsize.get + ","
    if (readgroupSequencingCenter.isDefined) RG += "CN:" + readgroupSequencingCenter.get + ","
    if (readgroupDate != null) RG += "DT:" + readgroupDate + ","
    RG += "PL:" + platform

    val stampyCmd = new Stampy(this)
    stampyCmd.R1 = R1
    if (paired) stampyCmd.R2 = R2.get
    stampyCmd.readgroup = RG
    stampyCmd.sanger = true
    stampyCmd.output = this.swapExt(output.getParentFile, output, ".bam", ".sam")
    stampyCmd.isIntermediate = true
    add(stampyCmd)
    val sortSam = SortSam(this, stampyCmd.output, output)
    sortSam.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(sortSam)
    sortSam.output
  }

  /** Adds bowtie jobs */
  def addBowtie(R1: File, R2: Option[File], output: File): File = {
    val zcatR1 = extractIfNeeded(R1, output.getParentFile)
    val zcatR2 = if (paired) Some(extractIfNeeded(R2.get, output.getParentFile)) else None
    zcatR1._1.foreach(add(_))
    zcatR2.foreach(_._1.foreach(add(_)))
    val bowtie = new Bowtie(this)
    bowtie.R1 = zcatR1._2
    if (paired) bowtie.R2 = Some(zcatR2.get._2)
    bowtie.output = this.swapExt(output.getParentFile, output, ".bam", ".sam")
    bowtie.isIntermediate = true
    val ar = addAddOrReplaceReadGroups(bowtie.output, output)
    val pipe = new BiopetFifoPipe(this, (Some(bowtie) :: Some(ar._1) :: Nil).flatten)
    pipe.threadsCorrection = -1
    pipe.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(pipe)
    ar._2
  }

  /** Add bowtie2 jobs **/
  def addBowtie2(R1: File, R2: Option[File], output: File): File = {
    val bowtie2 = new Bowtie2(this)
    bowtie2.rgId = Some(readgroupId)
    bowtie2.rg +:= ("LB:" + libId.get)
    bowtie2.rg +:= ("PL:" + platform)
    platformUnit.foreach(x => bowtie2.rg +:= ("PU:" + x))
    bowtie2.rg +:= ("SM:" + sampleId.get)
    bowtie2.R1 = R1
    bowtie2.R2 = R2
    val sortSam = new SortSam(this)
    sortSam.output = output
    val pipe = bowtie2 | sortSam
    pipe.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    pipe.threadsCorrection = -1
    add(pipe)
    output
  }

  /** Adds Star jobs */
  def addStar(R1: File, R2: Option[File], output: File): File = {
    val zcatR1 = extractIfNeeded(R1, output.getParentFile)
    val zcatR2 = if (paired) Some(extractIfNeeded(R2.get, output.getParentFile)) else None
    val starCommand = Star(this, zcatR1._2, zcatR2.map(_._2), outputDir, isIntermediate = true)
    val ar = addAddOrReplaceReadGroups(starCommand.outputSam, swapExt(outputDir, output, ".bam", ".addAddOrReplaceReadGroups.bam"))

    val reorderSam = new ReorderSam(this)
    reorderSam.input = ar._2
    reorderSam.output = output

    val pipe = new BiopetFifoPipe(this, (zcatR1._1 :: zcatR2.flatMap(_._1) ::
      Some(starCommand) :: Some(ar._1) :: Some(reorderSam) :: Nil).flatten)
    pipe.threadsCorrection = -3
    zcatR1._1.foreach(x => pipe.threadsCorrection -= 1)
    zcatR2.foreach(_._1.foreach(x => pipe.threadsCorrection -= 1))
    pipe.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(pipe)
    reorderSam.output
  }

  /** Adds Start 2 pass jobs */
  def addStar2pass(R1: File, R2: Option[File], output: File): File = {
    val zcatR1 = extractIfNeeded(R1, output.getParentFile)
    val zcatR2 = if (paired) Some(extractIfNeeded(R2.get, output.getParentFile)) else None
    zcatR1._1.foreach(add(_))
    zcatR2.foreach(_._1.foreach(add(_)))

    val starCommand = Star._2pass(this, zcatR1._2, zcatR2.map(_._2), outputDir, isIntermediate = true)
    addAll(starCommand._2)
    val ar = addAddOrReplaceReadGroups(starCommand._1, output)
    ar._1.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile
    add(ar._1)
    ar._2
  }

  /** Adds AddOrReplaceReadGroups */
  def addAddOrReplaceReadGroups(input: File, output: File): (AddOrReplaceReadGroups, File) = {
    val addOrReplaceReadGroups = AddOrReplaceReadGroups(this, input, output)
    addOrReplaceReadGroups.createIndex = true

    addOrReplaceReadGroups.RGID = readgroupId
    addOrReplaceReadGroups.RGLB = libId.get
    addOrReplaceReadGroups.RGPL = platform
    addOrReplaceReadGroups.RGPU = platformUnit.getOrElse(readgroupId)
    addOrReplaceReadGroups.RGSM = sampleId.get
    if (readgroupSequencingCenter.isDefined) addOrReplaceReadGroups.RGCN = readgroupSequencingCenter.get
    if (readgroupDescription.isDefined) addOrReplaceReadGroups.RGDS = readgroupDescription.get
    addOrReplaceReadGroups.isIntermediate = chunking || !skipMarkduplicates || !keepFinalBamFile

    (addOrReplaceReadGroups, addOrReplaceReadGroups.output)
  }

  /** Returns readgroup for bwa */
  def getReadGroupBwa: String = {
    var RG: String = "@RG\\t" + "ID:" + readgroupId + "\\t"
    readgroupLibrary.foreach(lb => RG += "LB:" + lb + "\\t")
    RG += "PL:" + platform + "\\t"
    platformUnit.foreach(x => RG += "PU:" + x + "\\t")
    RG += "SM:" + sampleId.get + "\\t"
    if (readgroupSequencingCenter.isDefined) RG += "CN:" + readgroupSequencingCenter.get + "\\t"
    if (readgroupDescription.isDefined) RG += "DS:" + readgroupDescription.get + "\\t"
    if (readgroupDate != null) RG += "DT:" + readgroupDate + "\\t"
    if (predictedInsertsize.isDefined) RG += "PI:" + predictedInsertsize.get + "\\t"

    RG.substring(0, RG.lastIndexOf("\\t"))
  }

  //FIXME: This is code duplication from flexiprep, need general class to pass jobs inside a util function
  /**
   * Extracts file if file is compressed
   * @param file input file
   * @param runDir directory to extract when needed
   * @return returns extracted file
   */
  def extractIfNeeded(file: File, runDir: File): (Option[BiopetCommandLineFunction], File) = {
    require(file != null)
    if (file.getName.endsWith(".gz") || file.getName.endsWith(".gzip")) {
      var newFile: File = swapExt(runDir, file, ".gz", "")
      if (file.getName.endsWith(".gzip")) newFile = swapExt(runDir, file, ".gzip", "")
      val zcatCommand = Zcat(this, file, newFile)
      (Some(zcatCommand), newFile)
    } else if (file.getName.endsWith(".bz2")) {
      val newFile = swapExt(runDir, file, ".bz2", "")
      val pbzip2 = Pbzip2(this, file, newFile)
      (Some(pbzip2), newFile)
    } else (None, file)
  }

}

object Mapping extends PipelineCommand
