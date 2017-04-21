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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.summary.{ Summarizable, SummaryQScript }
import nl.lumc.sasc.biopet.core.{ BiopetFifoPipe, PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.{ Gzip, Zcat }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.utils.IoUtils._
import nl.lumc.sasc.biopet.extensions.tools.{ FastqSync, SeqStat, ValidateFastq }
import nl.lumc.sasc.biopet.utils.Logging
import org.broadinstitute.gatk.queue.QScript

class Flexiprep(val parent: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  def this() = this(null)

  @Input(doc = "R1 fastq file (gzipped allowed)", shortName = "R1", fullName = "inputR1", required = true)
  var inputR1: File = _

  @Input(doc = "R2 fastq file (gzipped allowed)", shortName = "R2", fullName = "inputR2", required = false)
  var inputR2: Option[File] = None

  /** Skip Trim fastq files */
  var skipTrim: Boolean = config("skip_trim", default = false)

  /** Skip Clip fastq files */
  var skipClip: Boolean = config("skip_clip", default = false)

  /** Make a final fastq files, by default only when flexiprep is the main pipeline */
  var keepQcFastqFiles: Boolean = config("keepQcFastqFiles", default = parent == null)

  override def defaults = super.defaults ++ Map("max_threads" -> 4)

  /** Returns files to store in summary */
  def summaryFiles: Map[String, File] = {
    Map("input_R1" -> inputR1, "output_R1" -> fastqR1Qc) ++
      (if (paired) Map("input_R2" -> inputR2.get, "output_R2" -> fastqR2Qc.get) else Map())
  }

  /** returns settings to store in summary */
  def summarySettings = Map("skip_trim" -> skipTrim, "skip_clip" -> skipClip, "paired" -> paired)

  var paired: Boolean = inputR2.isDefined
  var R1Name: String = _
  var R2Name: String = _

  var fastqcR1: Fastqc = _
  var fastqcR2: Fastqc = _
  var fastqcR1After: Fastqc = _
  var fastqcR2After: Fastqc = _

  override def reportClass: Some[FlexiprepReport] = {
    val flexiprepReport = new FlexiprepReport(this)
    flexiprepReport.outputDir = new File(outputDir, "report")
    flexiprepReport.summaryDbFile = summaryDbFile
    flexiprepReport.args = Map(
      "sampleId" -> sampleId.getOrElse("."),
      "libId" -> libId.getOrElse("."))
    Some(flexiprepReport)
  }

  /** Function that's need to be executed before the script is accessed */
  def init() {
    paired = inputR2.isDefined
    if (inputR1 == null) Logging.addError("Missing input R1 on flexiprep module")
    if (sampleId == null || sampleId.isEmpty) Logging.addError("Missing sample ID on flexiprep module")
    if (libId == null || libId.isEmpty) Logging.addError("Missing library ID on flexiprep module")

    if (inputR1 == null) Logging.addError("Missing input R1 on flexiprep module")
    else {
      inputFiles :+= new InputFile(inputR1)
      inputR2.foreach(inputFiles :+= new InputFile(_))

      R1Name = getUncompressedFileName(inputR1)
      inputR2.foreach { fileR2 =>
        paired = true
        R2Name = getUncompressedFileName(fileR2)
        if (fileR2 == inputR1) Logging.addError(s"R1 and R2 for $sampleId -> $libId are the same")
      }
    }
  }

  /** Script to add jobs */
  def biopetScript() {
    if (inputR1 != null) {
      runInitialJobs()

      if (paired) runTrimClip(inputR1, inputR2, outputDir)
      else runTrimClip(inputR1, outputDir)

      val R1Files = for ((k, v) <- outputFiles if k.endsWith("output_R1")) yield v
      val R2Files = for ((k, v) <- outputFiles if k.endsWith("output_R2")) yield v
      runFinalize(R1Files.toList, R2Files.toList)
    }
  }

  /** Add init non chunkable jobs */
  def runInitialJobs() {
    outputFiles += ("fastq_input_R1" -> inputR1)
    if (paired) outputFiles += ("fastq_input_R2" -> inputR2.get)

    fastqcR1 = Fastqc(this, inputR1, new File(outputDir, R1Name + ".fastqc/"))
    add(fastqcR1)
    addSummarizable(fastqcR1, "fastqc_R1")
    outputFiles += ("fastqc_R1" -> fastqcR1.output)

    val validateFastq = new ValidateFastq(this)
    validateFastq.r1Fastq = inputR1
    validateFastq.r2Fastq = inputR2
    validateFastq.jobOutputFile = new File(outputDir, ".validate_fastq.log.out")
    add(validateFastq)

    if (config("abort_on_corrupt_fastq", default = true)) {
      val checkValidateFastq = new CheckValidateFastq
      checkValidateFastq.inputLogFile = validateFastq.jobOutputFile
      checkValidateFastq.jobOutputFile = new File(outputDir, ".check.validate_fastq.log.out")
      add(checkValidateFastq)
    }

    if (paired) {
      fastqcR2 = Fastqc(this, inputR2.get, new File(outputDir, R2Name + ".fastqc/"))
      add(fastqcR2)
      addSummarizable(fastqcR2, "fastqc_R2")
      outputFiles += ("fastqc_R2" -> fastqcR2.output)
    }

    val seqstatR1 = SeqStat(this, inputR1, outputDir)
    add(seqstatR1)
    addSummarizable(seqstatR1, "seqstat_R1")

    if (paired) {
      val seqstatR2 = SeqStat(this, inputR2.get, outputDir)
      add(seqstatR2)
      addSummarizable(seqstatR2, "seqstat_R2")
    }
  }

  def fastqR1Qc: File = if (paired)
    new File(outputDir, s"${sampleId.getOrElse("x")}-${libId.getOrElse("x")}.R1.qc.sync.fq.gz")
  else new File(outputDir, s"${sampleId.getOrElse("x")}-${libId.getOrElse("x")}.R1.qc.fq.gz")
  def fastqR2Qc: Option[File] = if (paired)
    Some(new File(outputDir, s"${sampleId.getOrElse("x")}-${libId.getOrElse("x")}.R2.qc.sync.fq.gz"))
  else None

  /** Adds all chunkable jobs of flexiprep */
  def runTrimClip(R1_in: File, outDir: File, chunk: String): (File, Option[File]) =
    runTrimClip(R1_in, None, outDir, chunk)

  /** Adds all chunkable jobs of flexiprep */
  def runTrimClip(R1_in: File, outDir: File): (File, Option[File]) =
    runTrimClip(R1_in, None, outDir, "")

  /** Adds all chunkable jobs of flexiprep */
  def runTrimClip(R1_in: File, R2_in: Option[File], outDir: File): (File, Option[File]) =
    runTrimClip(R1_in, R2_in, outDir, "")

  /** Adds all chunkable jobs of flexiprep */
  def runTrimClip(R1_in: File,
                  R2_in: Option[File],
                  outDir: File,
                  chunkarg: String): (File, Option[File]) = {
    val chunk = if (chunkarg.isEmpty || chunkarg.endsWith("_")) chunkarg else chunkarg + "_"

    var R1 = R1_in
    var R2 = R2_in

    val qcCmdR1 = new QcCommand(this, fastqcR1, "R1")
    qcCmdR1.input = R1_in
    qcCmdR1.output = if (paired) new File(outDir, fastqR1Qc.getName.stripSuffix(".gz"))
    else fastqR1Qc
    qcCmdR1.deps :+= fastqcR1.output
    qcCmdR1.isIntermediate = paired || !keepQcFastqFiles
    addSummarizable(qcCmdR1, "qc_command_R1")

    if (paired) {
      val qcCmdR2 = new QcCommand(this, fastqcR2, "R2")
      qcCmdR2.input = R2_in.get
      qcCmdR2.output = new File(outDir, fastqR2Qc.get.getName.stripSuffix(".gz"))
      addSummarizable(qcCmdR2, "qc_command_R2")

      qcCmdR1.compress = false
      qcCmdR2.compress = false

      val fqSync = new FastqSync(this)
      fqSync.refFastq = R1_in
      fqSync.inputFastq1 = qcCmdR1.output
      fqSync.inputFastq2 = qcCmdR2.output
      fqSync.outputFastq1 = new File(outDir, fastqR1Qc.getName)
      fqSync.outputFastq2 = new File(outDir, fastqR2Qc.get.getName)
      fqSync.outputStats = new File(outDir, s"${sampleId.getOrElse("x")}-${libId.getOrElse("x")}.sync.stats")

      val pipe = new BiopetFifoPipe(this, fqSync :: qcCmdR1.jobs ::: qcCmdR2.jobs) with Summarizable {
        override def configNamespace = "qc_cmd"

        override def beforeGraph(): Unit = {
          fqSync.beforeGraph()
          commands = qcCmdR1.jobs ::: qcCmdR2.jobs ::: fqSync :: Nil
          super.beforeGraph()
        }

        override def beforeCmd(): Unit = {
          qcCmdR1.beforeCmd()
          qcCmdR2.beforeCmd()
          fqSync.beforeCmd()
          commands = qcCmdR1.jobs ::: qcCmdR2.jobs ::: fqSync :: Nil
          commands.foreach(addPipeJob)
          super.beforeCmd()
        }

        /** Must return files to store into summary */
        def summaryFiles: Map[String, File] = Map()

        /** Must returns stats to store into summary */
        def summaryStats: Any = Map()

        override def summaryDeps: List[File] = qcCmdR1.summaryDeps ::: qcCmdR2.summaryDeps ::: super.summaryDeps
      }

      pipe.jobOutputFile = new File(outDir, ".qc_cmd.out")
      pipe.deps ::= fastqcR1.output
      pipe.deps ::= fastqcR2.output
      pipe.deps ::= R1_in
      pipe.deps ::= R2_in.get
      pipe.nCoresRequest = Some(4)
      pipe.isIntermediate = !keepQcFastqFiles
      addSummarizable(pipe, "qc_cmd")
      add(pipe)

      addSummarizable(fqSync, "fastq_sync")
      outputFiles += ("syncStats" -> fqSync.outputStats)
      R1 = fqSync.outputFastq1
      R2 = Some(fqSync.outputFastq2)
    } else {
      qcCmdR1.nCoresRequest = Some(2)
      qcCmdR1.jobOutputFile = new File(outDir, ".qc_cmd.out")
      add(qcCmdR1)
      R1 = qcCmdR1.output
    }

    val seqstatR1After = SeqStat(this, R1, outDir)
    add(seqstatR1After)
    addSummarizable(seqstatR1After, "seqstat_R1_qc")

    if (paired) {
      val seqstatR2After = SeqStat(this, R2.get, outDir)
      add(seqstatR2After)
      addSummarizable(seqstatR2After, "seqstat_R2_qc")
    }

    outputFiles += (chunk + "output_R1" -> R1)
    if (paired) outputFiles += (chunk + "output_R2" -> R2.get)
    (R1, R2)
  }

  /** Adds last non chunkable jobs */
  def runFinalize(fastq_R1: List[File], fastq_R2: List[File]) {
    if (fastq_R1.length != fastq_R2.length && paired)
      throw new IllegalStateException("R1 and R2 file number is not the same")

    if (fastq_R1.length > 1) {
      val zcat = new Zcat(this)
      zcat.input = fastq_R1
      val cmdR1 = zcat | new Gzip(this) > fastqR1Qc
      cmdR1.isIntermediate = !keepQcFastqFiles
      add(cmdR1)
      if (paired) {
        val zcat = new Zcat(this)
        zcat.input = fastq_R2
        val cmdR2 = zcat | new Gzip(this) > fastqR2Qc.get
        cmdR2.isIntermediate = !keepQcFastqFiles
        add(cmdR2)
      }
    }

    val validateFastq = new ValidateFastq(this)
    validateFastq.r1Fastq = fastqR1Qc
    validateFastq.r2Fastq = fastqR2Qc
    validateFastq.jobOutputFile = new File(outputDir, ".validate_fastq.qc.log.out")
    add(validateFastq)

    if (config("abort_on_corrupt_fastq", default = true)) {
      val checkValidateFastq = new CheckValidateFastq
      checkValidateFastq.inputLogFile = validateFastq.jobOutputFile
      checkValidateFastq.jobOutputFile = new File(outputDir, ".check.validate_fastq.qc.log.out")
      add(checkValidateFastq)
    }

    outputFiles += ("output_R1_gzip" -> fastqR1Qc)
    if (paired) outputFiles += ("output_R2_gzip" -> fastqR2Qc.get)

    fastqcR1After = Fastqc(this, fastqR1Qc, new File(outputDir, R1Name + ".qc.fastqc/"))
    add(fastqcR1After)
    addSummarizable(fastqcR1After, "fastqc_R1_qc")

    if (paired) {
      fastqcR2After = Fastqc(this, fastqR2Qc.get, new File(outputDir, R2Name + ".qc.fastqc/"))
      add(fastqcR2After)
      addSummarizable(fastqcR2After, "fastqc_R2_qc")
    }

    addSummaryJobs()
  }
}

object Flexiprep extends PipelineCommand
