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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument }

import nl.lumc.sasc.biopet.core.{ SampleLibraryTag, BiopetQScript, PipelineCommand }
import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions._
import nl.lumc.sasc.biopet.tools.SeqStat
import nl.lumc.sasc.biopet.tools.FastqSync

class Flexiprep(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "R1 fastq file (gzipped allowed)", shortName = "R1", required = true)
  var input_R1: File = _

  @Input(doc = "R2 fastq file (gzipped allowed)", shortName = "R2", required = false)
  var input_R2: Option[File] = None

  /** Skip Trim fastq files */
  var skipTrim: Boolean = config("skip_trim", default = false)

  /** Skip Clip fastq files */
  var skipClip: Boolean = config("skip_clip", default = false)

  /** Location of summary file */
  def summaryFile = new File(outputDir, sampleId.getOrElse("x") + "-" + libId.getOrElse("x") + ".qc.summary.json")

  /** Returns files to store in summary */
  def summaryFiles: Map[String, File] = {
    Map("input_R1" -> input_R1, "output_R1" -> outputFiles("output_R1_gzip")) ++
      (if (paired) Map("input_R2" -> input_R2.get, "output_R2" -> outputFiles("output_R2_gzip")) else Map())
  }

  /** returns settings to store in summary */
  def summarySettings = Map("skip_trim" -> skipTrim, "skip_clip" -> skipClip, "paired" -> paired)

  var paired: Boolean = input_R2.isDefined
  var R1_ext: String = _
  var R2_ext: String = _
  var R1_name: String = _
  var R2_name: String = _

  var fastqc_R1: Fastqc = _
  var fastqc_R2: Fastqc = _
  var fastqc_R1_after: Fastqc = _
  var fastqc_R2_after: Fastqc = _

  /** Function that's need to be executed before the script is accessed */
  def init() {
    require(outputDir != null, "Missing output directory on flexiprep module")
    require(input_R1 != null, "Missing input R1 on flexiprep module")
    require(sampleId != null, "Missing sample ID on flexiprep module")
    require(libId != null, "Missing library ID on flexiprep module")

    paired = input_R2.isDefined

    if (input_R1.endsWith(".gz")) R1_name = input_R1.getName.substring(0, input_R1.getName.lastIndexOf(".gz"))
    else if (input_R1.endsWith(".gzip")) R1_name = input_R1.getName.substring(0, input_R1.getName.lastIndexOf(".gzip"))
    else R1_name = input_R1.getName
    R1_ext = R1_name.substring(R1_name.lastIndexOf("."), R1_name.size)
    R1_name = R1_name.substring(0, R1_name.lastIndexOf(R1_ext))

    input_R2 match {
      case Some(fileR2) => {
        paired = true
        if (fileR2.endsWith(".gz")) R2_name = fileR2.getName.substring(0, fileR2.getName.lastIndexOf(".gz"))
        else if (fileR2.endsWith(".gzip")) R2_name = fileR2.getName.substring(0, fileR2.getName.lastIndexOf(".gzip"))
        else R2_name = fileR2.getName
        R2_ext = R2_name.substring(R2_name.lastIndexOf("."), R2_name.size)
        R2_name = R2_name.substring(0, R2_name.lastIndexOf(R2_ext))
      }
      case _ =>
    }
  }

  /** Script to add jobs */
  def biopetScript() {
    runInitialJobs()

    val out = if (paired) runTrimClip(outputFiles("fastq_input_R1"), Some(outputFiles("fastq_input_R2")), outputDir)
    else runTrimClip(outputFiles("fastq_input_R1"), outputDir)

    val R1_files = for ((k, v) <- outputFiles if k.endsWith("output_R1")) yield v
    val R2_files = for ((k, v) <- outputFiles if k.endsWith("output_R2")) yield v
    runFinalize(R1_files.toList, R2_files.toList)
  }

  /** Add init non chunkable jobs */
  def runInitialJobs() {
    outputFiles += ("fastq_input_R1" -> extractIfNeeded(input_R1, outputDir))
    if (paired) outputFiles += ("fastq_input_R2" -> extractIfNeeded(input_R2.get, outputDir))

    fastqc_R1 = Fastqc(this, input_R1, new File(outputDir, R1_name + ".fastqc/"))
    add(fastqc_R1)
    addSummarizable(fastqc_R1, "fastqc_R1")
    outputFiles += ("fastqc_R1" -> fastqc_R1.output)

    if (paired) {
      fastqc_R2 = Fastqc(this, input_R2.get, new File(outputDir, R2_name + ".fastqc/"))
      add(fastqc_R2)
      addSummarizable(fastqc_R2, "fastqc_R2")
      outputFiles += ("fastqc_R2" -> fastqc_R2.output)
    }
  }

  //TODO: Refactor need to combine all this functions

  /**
   * Adds all chunkable jobs of flexiprep
   * @param R1_in
   * @param outDir
   * @param chunk
   * @return
   */
  def runTrimClip(R1_in: File, outDir: File, chunk: String): (File, Option[File], List[File]) =
    runTrimClip(R1_in, None, outDir, chunk)

  /**
   * Adds all chunkable jobs of flexiprep
   * @param R1_in
   * @param outDir
   * @return
   */
  def runTrimClip(R1_in: File, outDir: File): (File, Option[File], List[File]) =
    runTrimClip(R1_in, None, outDir, "")

  /**
   * Adds all chunkable jobs of flexiprep
   * @param R1_in
   * @param R2_in
   * @param outDir
   * @return
   */
  def runTrimClip(R1_in: File, R2_in: Option[File], outDir: File): (File, Option[File], List[File]) =
    runTrimClip(R1_in, R2_in, outDir, "")

  /**
   * Adds all chunkable jobs of flexiprep
   * @param R1_in
   * @param R2_in
   * @param outDir
   * @param chunkarg
   * @return
   */
  def runTrimClip(R1_in: File, R2_in: Option[File], outDir: File, chunkarg: String): (File, Option[File], List[File]) = {
    val chunk = if (chunkarg.isEmpty || chunkarg.endsWith("_")) chunkarg else chunkarg + "_"
    var results: Map[String, File] = Map()

    var R1 = R1_in
    var R2 = R2_in
    var deps_R1 = R1 :: Nil
    var deps_R2 = if (paired) R2.get :: Nil else Nil
    def deps: List[File] = deps_R1 ::: deps_R2

    val seqtkSeq_R1 = SeqtkSeq(this, R1, swapExt(outDir, R1, R1_ext, ".sanger" + R1_ext), fastqc_R1)
    seqtkSeq_R1.isIntermediate = true
    add(seqtkSeq_R1)
    addSummarizable(seqtkSeq_R1, "seqtkSeq_R1")
    R1 = seqtkSeq_R1.output
    deps_R1 ::= R1

    if (paired) {
      val seqtkSeq_R2 = SeqtkSeq(this, R2.get, swapExt(outDir, R2.get, R2_ext, ".sanger" + R2_ext), fastqc_R2)
      seqtkSeq_R2.isIntermediate = true
      add(seqtkSeq_R2)
      addSummarizable(seqtkSeq_R2, "seqtkSeq_R2")
      R2 = Some(seqtkSeq_R2.output)
      deps_R2 ::= R2.get
    }

    val seqstat_R1 = SeqStat(this, R1, outDir)
    seqstat_R1.isIntermediate = true
    seqstat_R1.deps = deps_R1
    add(seqstat_R1)
    addSummarizable(seqstat_R1, "seqstat_R1")

    if (paired) {
      val seqstat_R2 = SeqStat(this, R2.get, outDir)
      seqstat_R2.isIntermediate = true
      seqstat_R2.deps = deps_R2
      add(seqstat_R2)
      addSummarizable(seqstat_R2, "seqstat_R2")
    }

    if (!skipClip) { // Adapter clipping

      val cutadapt_R1 = Cutadapt(this, R1, swapExt(outDir, R1, R1_ext, ".clip" + R1_ext))
      cutadapt_R1.fastqc = fastqc_R1
      cutadapt_R1.isIntermediate = true
      cutadapt_R1.deps = deps_R1
      add(cutadapt_R1)
      addSummarizable(cutadapt_R1, "clipping_R1")
      R1 = cutadapt_R1.fastq_output
      deps_R1 ::= R1
      outputFiles += ("cutadapt_R1_stats" -> cutadapt_R1.stats_output)

      if (paired) {
        val cutadapt_R2 = Cutadapt(this, R2.get, swapExt(outDir, R2.get, R2_ext, ".clip" + R2_ext))
        outputFiles += ("cutadapt_R2_stats" -> cutadapt_R2.stats_output)
        cutadapt_R2.fastqc = fastqc_R2
        cutadapt_R2.isIntermediate = true
        cutadapt_R2.deps = deps_R2
        add(cutadapt_R2)
        addSummarizable(cutadapt_R2, "clipping_R2")
        R2 = Some(cutadapt_R2.fastq_output)
        deps_R2 ::= R2.get

        val fqSync = new FastqSync(this)
        fqSync.refFastq = cutadapt_R1.fastq_input
        fqSync.inputFastq1 = cutadapt_R1.fastq_output
        fqSync.inputFastq2 = cutadapt_R2.fastq_output
        fqSync.outputFastq1 = swapExt(outDir, R1, R1_ext, ".sync" + R1_ext)
        fqSync.outputFastq2 = swapExt(outDir, R2.get, R2_ext, ".sync" + R2_ext)
        fqSync.outputStats = swapExt(outDir, R1, R1_ext, ".sync.stats")
        fqSync.deps :::= deps
        add(fqSync)
        addSummarizable(fqSync, "fastq_sync")
        outputFiles += ("syncStats" -> fqSync.outputStats)
        R1 = fqSync.outputFastq1
        R2 = Some(fqSync.outputFastq2)
        deps_R1 ::= R1
        deps_R2 ::= R2.get
      }
    }

    if (!skipTrim) { // Quality trimming
      val sickle = new Sickle(this)
      sickle.input_R1 = R1
      sickle.output_R1 = swapExt(outDir, R1, R1_ext, ".trim" + R1_ext)
      if (paired) {
        sickle.input_R2 = R2.get
        sickle.output_R2 = swapExt(outDir, R2.get, R2_ext, ".trim" + R2_ext)
        sickle.output_singles = swapExt(outDir, R2.get, R2_ext, ".trim.singles" + R1_ext)
      }
      sickle.output_stats = swapExt(outDir, R1, R1_ext, ".trim.stats")
      sickle.deps = deps
      sickle.isIntermediate = true
      add(sickle)
      addSummarizable(sickle, "trimming")
      R1 = sickle.output_R1
      if (paired) R2 = Some(sickle.output_R2)
    }

    val seqstat_R1_after = SeqStat(this, R1, outDir)
    seqstat_R1_after.deps = deps_R1
    add(seqstat_R1_after)
    addSummarizable(seqstat_R1_after, "seqstat_R1_after")

    if (paired) {
      val seqstat_R2_after = SeqStat(this, R2.get, outDir)
      seqstat_R2_after.deps = deps_R2
      add(seqstat_R2_after)
      addSummarizable(seqstat_R2_after, "seqstat_R2_after")
    }

    outputFiles += (chunk + "output_R1" -> R1)
    if (paired) outputFiles += (chunk + "output_R2" -> R2.get)
    return (R1, R2, deps)
  }

  /**
   * Adds last non chunkable jobs
   * @param fastq_R1
   * @param fastq_R2
   */
  def runFinalize(fastq_R1: List[File], fastq_R2: List[File]) {
    if (fastq_R1.length != fastq_R2.length && paired) throw new IllegalStateException("R1 and R2 file number is not the same")
    val R1 = new File(outputDir, R1_name + ".qc" + R1_ext + ".gz")
    val R2 = new File(outputDir, R2_name + ".qc" + R2_ext + ".gz")

    add(Gzip(this, fastq_R1, R1))
    if (paired) add(Gzip(this, fastq_R2, R2))

    outputFiles += ("output_R1_gzip" -> R1)
    if (paired) outputFiles += ("output_R2_gzip" -> R2)

    if (!skipTrim || !skipClip) {
      fastqc_R1_after = Fastqc(this, R1, new File(outputDir, R1_name + ".qc.fastqc/"))
      add(fastqc_R1_after)
      addSummarizable(fastqc_R1_after, "fastqc_R1_qc")

      if (paired) {
        fastqc_R2_after = Fastqc(this, R2, new File(outputDir, R2_name + ".qc.fastqc/"))
        add(fastqc_R2_after)
        addSummarizable(fastqc_R2_after, "fastqc_R2_qc")
      }
    }

    addSummaryJobs
  }

  /**
   * Extracts file if file is compressed
   * @param file
   * @param runDir
   * @return returns extracted file
   */
  def extractIfNeeded(file: File, runDir: File): File = {
    if (file == null) return file
    else if (file.getName().endsWith(".gz") || file.getName().endsWith(".gzip")) {
      var newFile: File = swapExt(runDir, file, ".gz", "")
      if (file.getName().endsWith(".gzip")) newFile = swapExt(runDir, file, ".gzip", "")
      val zcatCommand = Zcat(this, file, newFile)
      zcatCommand.isIntermediate = true
      add(zcatCommand)
      return newFile
    } else if (file.getName().endsWith(".bz2")) {
      val newFile = swapExt(runDir, file, ".bz2", "")
      val pbzip2 = Pbzip2(this, file, newFile)
      pbzip2.isIntermediate = true
      add(pbzip2)
      return newFile
    } else return file
  }
}

object Flexiprep extends PipelineCommand
