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
import nl.lumc.sasc.biopet.core.{ BiopetFifoPipe, PipelineCommand, SampleLibraryTag }
import nl.lumc.sasc.biopet.extensions.{ Zcat, Gzip }
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.extensions.tools.{ SeqStat, FastqSync }

import org.apache.commons.io.FilenameUtils.getExtension
import org.broadinstitute.gatk.queue.QScript

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

  /** Make a final fastq files, by default only when flexiprep is the main pipeline */
  var keepQcFastqFiles: Boolean = config("keepQcFastqFiles", default = root == null)

  /** Location of summary file */
  def summaryFile = new File(outputDir, sampleId.getOrElse("x") + "-" + libId.getOrElse("x") + ".qc.summary.json")

  /** Returns files to store in summary */
  def summaryFiles: Map[String, File] = {
    Map("input_R1" -> input_R1, "output_R1" -> fastqR1Qc) ++
      (if (paired) Map("input_R2" -> input_R2.get, "output_R2" -> fastqR2Qc.get) else Map())
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

  override def reportClass = {
    val flexiprepReport = new FlexiprepReport(this)
    flexiprepReport.outputDir = new File(outputDir, "report")
    flexiprepReport.summaryFile = summaryFile
    flexiprepReport.args = Map(
      "sampleId" -> sampleId.getOrElse("."),
      "libId" -> libId.getOrElse("."))
    Some(flexiprepReport)
  }

  /** Possible compression extensions to trim from input files. */
  private val zipExtensions = Set(".gz", ".gzip")

  /**
   * Given a file object and a set of compression extensions, return the filename without any of the compression
   * extensions and the filename's extension.
   *
   * Examples:
   *  - my_file.fq.gz returns ("my_file.fq", ".fq")
   *  - my_other_file.fastq returns ("my_file.fastq", ".fastq")
   *
   * @param f Input file object.
   * @param exts Possible compression extensions to trim.
   * @return Filename without compression extension and its extension.
   */
  private def getNameAndExt(f: File, exts: Set[String] = zipExtensions): (String, String) = {
    val unzippedFilename = zipExtensions
      .foldLeft(f.toString) { case (fname, ext) =>
      if (fname.toLowerCase.endsWith(ext)) fname.dropRight(ext.length)
      else fname
    }
    val unzippedExt = "." + getExtension(unzippedFilename)
    (unzippedFilename, unzippedExt)
  } ensuring(_._2.length > 1, "Flexiprep input files must have an extension when uncompressed.")

  /** Function that's need to be executed before the script is accessed */
  def init() {
    require(outputDir != null, "Missing output directory on flexiprep module")
    require(input_R1 != null, "Missing input R1 on flexiprep module")
    require(sampleId != null, "Missing sample ID on flexiprep module")
    require(libId != null, "Missing library ID on flexiprep module")

    paired = input_R2.isDefined

    inputFiles :+= new InputFile(input_R1)
    input_R2.foreach(inputFiles :+= new InputFile(_))

    (R1_ext, R1_name) = getNameAndExt(input_R1)
    input_R2.foreach { fileR2 =>
        paired = true
        (R2_ext, R2_name) = getNameAndExt(fileR2)
    }
  }

  /** Script to add jobs */
  def biopetScript() {
    runInitialJobs()

    if (paired) runTrimClip(input_R1, input_R2, outputDir)
    else runTrimClip(input_R1, outputDir)

    val R1_files = for ((k, v) <- outputFiles if k.endsWith("output_R1")) yield v
    val R2_files = for ((k, v) <- outputFiles if k.endsWith("output_R2")) yield v
    runFinalize(R1_files.toList, R2_files.toList)
  }

  /** Add init non chunkable jobs */
  def runInitialJobs() {
    outputFiles += ("fastq_input_R1" -> input_R1)
    if (paired) outputFiles += ("fastq_input_R2" -> input_R2.get)

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

    val seqstat_R1 = SeqStat(this, input_R1, outputDir)
    seqstat_R1.isIntermediate = true
    add(seqstat_R1)
    addSummarizable(seqstat_R1, "seqstat_R1")

    if (paired) {
      val seqstat_R2 = SeqStat(this, input_R2.get, outputDir)
      seqstat_R2.isIntermediate = true
      add(seqstat_R2)
      addSummarizable(seqstat_R2, "seqstat_R2")
    }
  }

  def fastqR1Qc = if (paired)
    new File(outputDir, s"${sampleId.getOrElse("x")}-${libId.getOrElse("x")}.R1.qc.sync.fq.gz")
  else new File(outputDir, s"${sampleId.getOrElse("x")}-${libId.getOrElse("x")}.R1.qc.fq.gz")
  def fastqR2Qc = if (paired)
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

    val qcCmdR1 = new QcCommand(this, fastqc_R1)
    qcCmdR1.input = R1_in
    qcCmdR1.read = "R1"
    qcCmdR1.output = if (paired) new File(outDir, fastqR1Qc.getName.stripSuffix(".gz"))
    else fastqR1Qc
    qcCmdR1.deps :+= fastqc_R1.output
    qcCmdR1.isIntermediate = paired || !keepQcFastqFiles
    addSummarizable(qcCmdR1, "qc_command_R1")

    if (paired) {
      val qcCmdR2 = new QcCommand(this, fastqc_R2)
      qcCmdR2.input = R2_in.get
      qcCmdR2.output = new File(outDir, fastqR2Qc.get.getName.stripSuffix(".gz"))
      qcCmdR2.read = "R2"
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

      val pipe = new BiopetFifoPipe(this, fqSync :: Nil) {
        override def configName = "qc-cmd"

        override def beforeGraph(): Unit = {
          fqSync.beforeGraph()
          super.beforeGraph()
        }

        override def beforeCmd(): Unit = {
          qcCmdR1.beforeCmd()
          qcCmdR2.beforeCmd()
          fqSync.beforeCmd()
          commands = qcCmdR1.jobs ::: qcCmdR2.jobs ::: fqSync :: Nil
          super.beforeCmd()
        }
      }

      pipe.deps ::= fastqc_R1.output
      pipe.deps ::= fastqc_R2.output
      pipe.isIntermediate = !keepQcFastqFiles
      add(pipe)

      addSummarizable(fqSync, "fastq_sync")
      outputFiles += ("syncStats" -> fqSync.outputStats)
      R1 = fqSync.outputFastq1
      R2 = Some(fqSync.outputFastq2)
    } else {
      add(qcCmdR1)
      R1 = qcCmdR1.output
    }

    val seqstat_R1_after = SeqStat(this, R1, outDir)
    add(seqstat_R1_after)
    addSummarizable(seqstat_R1_after, "seqstat_R1_qc")

    if (paired) {
      val seqstat_R2_after = SeqStat(this, R2.get, outDir)
      add(seqstat_R2_after)
      addSummarizable(seqstat_R2_after, "seqstat_R2_qc")
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
      add(zcat | new Gzip(this) > fastqR1Qc)
      if (paired) {
        val zcat = new Zcat(this)
        zcat.input = fastq_R2
        add(zcat | new Gzip(this) > fastqR2Qc.get)
      }
    }

    outputFiles += ("output_R1_gzip" -> fastqR1Qc)
    if (paired) outputFiles += ("output_R2_gzip" -> fastqR2Qc.get)

    fastqc_R1_after = Fastqc(this, fastqR1Qc, new File(outputDir, R1_name + ".qc.fastqc/"))
    add(fastqc_R1_after)
    addSummarizable(fastqc_R1_after, "fastqc_R1_qc")

    if (paired) {
      fastqc_R2_after = Fastqc(this, fastqR2Qc.get, new File(outputDir, R2_name + ".qc.fastqc/"))
      add(fastqc_R2_after)
      addSummarizable(fastqc_R2_after, "fastqc_R2_qc")
    }

    addSummaryJobs()
  }
}

object Flexiprep extends PipelineCommand
