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
package nl.lumc.sasc.biopet.pipelines.gentrap

import java.io.File

import nl.lumc.sasc.biopet.extensions.Ln
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import org.broadinstitute.gatk.queue.QScript

import nl.lumc.sasc.biopet.core._
import nl.lumc.sasc.biopet.core.config._
import nl.lumc.sasc.biopet.pipelines.mapping.Mapping

/**
 * Gentrap pipeline
 * Generic transcriptome analysis pipeline
 */
class Gentrap(val root: Configurable) extends QScript with MultiSampleQScript {

  // alternative constructor for initialization with empty configuration
  def this() = this(null)

  /*
  /** Read 1 input */
  @Input(doc = "FASTQ file input (single-end or pair 1)", fullName = "input_r1", shortName = "R1", required = true)
  var inputR1: File = _

  /** Read 2 input (optional) */
  @Input(doc = "FASTQ file input (pair 2)", fullName = "input_r2", shortName = "R2", required = false)
  var inputR2: File = _

  /** FASTQ trimming */
  @Argument(doc = "Whether to skip trimming input files", fullName = "skip_trim_input", shortName = "skipTrim", required = false)
  var skipTrim: Boolean = false

  /** FASTQ clipping */
  @Argument(doc = "Whether to skip clipping input files", fullName = "skip_clip_input", shortName = "skipClip", required = false)
  var skipClip: Boolean = false
  */

  /** Split aligner to use */
  @Argument(doc = "Split aligner", fullName = "aligner", shortName = "aln", required = true, validation = "gsnap|tophat|star|star-2pass")
  var aligner: String = _

  /*
  /** Whether library is strand-specific (dUTP protocol) or not */
  @Argument(doc = "Whether input data was made using the dUTP strand-specific protocol", fullName = "strand_specific", shortName = "strandSpec", required = true)
  var strandSpec: Boolean = _

  /** Variant calling */
  @Argument(doc = "Variant caller", fullName = "variant_caller", shortName = "varCaller", required = false, validation = "varscan|snvmix")
  var varcaller: String = _

  /** Cufflinks assembly type */
  @Argument(doc = "Cufflinks assembly type", fullName = "transcript_asm", shortName = "transAsm", required = false, validation = "none|strict|guided|blind")
  var asm: List[String] = List("none")

  /** Gene-wise read count table output */
  @Argument(doc = "Gene read count table output", fullName = "count_gene_read", shortName = "cGeneRead", required = false)
  var cGeneRead: Boolean = _

  /** Gene-wise base count table output */
  @Argument(doc = "Gene base count table output", fullName = "count_gene_base", shortName = "cGeneBase", required = false)
  var cGeneBase: Boolean = _

  /** Exon-wise read count table output */
  @Argument(doc = "Exon read count table output", fullName = "count_exon_read", shortName = "cExonRead", required = false)
  var cExonRead: Boolean = _

  /** Exon-wise base count table output */
  @Argument(doc = "Exon base count table output", fullName = "count_exon_base", shortName = "cExonBase", required = false)
  var cExonBase: Boolean = _
  */

  defaults ++= Map(
    "gsnap" -> Map(
      "novelsplicing" -> 1,
      "batch" -> 4,
      "format" -> "sam"
    )
  )

  class SampleOutput extends AbstractSampleOutput {}

  class LibraryOutput extends AbstractLibraryOutput {
    var mappedBamFile: File = _
  }

  // empty implementation
  def init() {}

  def biopetScript() {
    // validation
    /*
    inputR1 = inputR1 match {
      case null   => throw new IllegalArgumentException("Missing read 1 argument for Gentrap")
      case other  => other
    }
    skipTrim = config.getOrElse("skipTrim", false)
    */
    //val testOutput = new File("/home/warindrarto/hmm/test_output.txt")
    runSamplesJobs()
  }

  def runSingleSampleJobs(sampleConfig: Map[String, Any]): SampleOutput = {
    val so = new SampleOutput
    val sampleDir: File = new File(outputDir + "sample_" + sampleConfig("ID").toString)
    so.libraries = runLibraryJobs(sampleConfig)

    val libBamFiles = so.libraries.values.map(x => x.mappedBamFile)
    val sampleBam = new File(outputDir, sampleConfig("ID").toString + ".bam")
    val sampleBamJob =
      if (libBamFiles.size == 1) {
        val ln = new Ln(this)
        ln.in = libBamFiles.head
        ln.out = sampleBam
        ln
      } else {
        val merge = new MergeSamFiles(this)
        merge.input = libBamFiles.toList
        merge.output = sampleBam
        merge.sortOrder = "coordinate"
        merge
      }
    add(sampleBamJob)

    so
  }

  def runSingleLibraryJobs(libConfig: Map[String, Any], sampleConfig: Map[String, Any]): LibraryOutput = {
    val lo = new LibraryOutput
    val libDir: File = new File(outputDir + "sample_" +
      sampleConfig("ID").toString + File.separator + "lib_" + libConfig("ID").toString)

    // val mapping = Mapping.loadFromLibraryConfig(this, libConfig, sampleConfig, libDir)
    val mapping = new Mapping(this)
    mapping.skipFlexiprep = config("skipFlexiprep", default = false)
    mapping.skipMetrics = config("skipMetrics", default = true)
    mapping.skipMarkduplicates = config("skipMarkDuplicates", default = true)
    mapping.aligner = config("aligner", default = "gsnap")
    // TODO: handle more than 2 files in mapping?
    mapping.input_R1 =
      if (libConfig.contains("R1")) {
        val r1 = new File(libConfig("R1").toString)
        if (r1.exists)
          r1
        else throw new IllegalArgumentException("Listed file " + r1.getPath + " does not exist")
      } else
        throw new IllegalArgumentException("Missing required argument: R1")
    mapping.input_R2 =
      if (libConfig.contains("R2")) {
        val r2 = new File(libConfig("R2").toString)
        if (r2.exists)
          r2
        else throw new IllegalArgumentException("Listed file " + r2.getPath + " does not exist")
      } else
        null
    mapping.RGLB = libConfig("ID").toString
    mapping.RGSM = sampleConfig("ID").toString
    mapping.outputDir = libDir
    mapping.init()
    mapping.biopetScript()
    addAll(mapping.functions)
    lo.mappedBamFile = mapping.outputFiles("finalBamFile")

    logger.info("From single lib jobs: " + mapping.input_R1.toString)
    logger.info("From single lib jobs: " + mapping.input_R2)
    lo
  }

}

object Gentrap extends PipelineCommand
