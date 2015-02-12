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
package nl.lumc.sasc.biopet.extensions

import java.io.File

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

class Star(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "The reference file for the bam files.", required = false)
  var reference: File = new File(config("reference"))

  @Input(doc = "Fastq file R1", required = false)
  var R1: File = _

  @Input(doc = "Fastq file R2", required = false)
  var R2: File = _

  @Output(doc = "Output SAM file", required = false)
  var outputSam: File = _

  @Output(doc = "Output tab file", required = false)
  var outputTab: File = _

  @Input(doc = "sjdbFileChrStartEnd file", required = false)
  var sjdbFileChrStartEnd: File = _

  @Output(doc = "Output genome file", required = false)
  var outputGenome: File = _

  @Output(doc = "Output SA file", required = false)
  var outputSA: File = _

  @Output(doc = "Output SAindex file", required = false)
  var outputSAindex: File = _

  executable = config("exe", "STAR")

  @Argument(doc = "Output Directory")
  var outputDir: File = _

  var genomeDir: File = config("genomeDir", new File(reference.getParent, "/star"))
  var runmode: String = _
  var sjdbOverhang: Int = _
  var outFileNamePrefix: String = _

  override val defaultVmem = "6G"
  override val defaultThreads = 8

  override def beforeGraph() {
    if (outFileNamePrefix != null && !outFileNamePrefix.endsWith(".")) outFileNamePrefix += "."
    val prefix = if (outFileNamePrefix != null) outputDir + outFileNamePrefix else outputDir
    if (runmode == null) {
      outputSam = new File(prefix + "Aligned.out.sam")
      outputTab = new File(prefix + "SJ.out.tab")
    } else if (runmode == "genomeGenerate") {
      genomeDir = outputDir
      outputGenome = new File(prefix + "Genome")
      outputSA = new File(prefix + "SA")
      outputSAindex = new File(prefix + "SAindex")
      sjdbOverhang = config("sjdboverhang", 75)
    }
  }

  def cmdLine: String = {
    var cmd: String = required("cd", outputDir) + "&&" + required(executable)
    if (runmode != null && runmode == "genomeGenerate") { // Create index
      cmd += required("--runMode", runmode) +
        required("--genomeFastaFiles", reference)
    } else { // Aligner
      cmd += required("--readFilesIn", R1) + optional(R2)
    }
    cmd += required("--genomeDir", genomeDir) +
      optional("--sjdbFileChrStartEnd", sjdbFileChrStartEnd) +
      optional("--runThreadN", nCoresRequest) +
      optional("--outFileNamePrefix", outFileNamePrefix)
    if (sjdbOverhang > 0) cmd += optional("--sjdbOverhang", sjdbOverhang)

    return cmd
  }
}

object Star {
  def apply(configurable: Configurable, R1: File, R2: File, outputDir: File, isIntermediate: Boolean = false, deps: List[File] = Nil): Star = {
    val star = new Star(configurable)
    star.R1 = R1
    if (R2 != null) star.R2 = R2
    star.outputDir = outputDir
    star.isIntermediate = isIntermediate
    star.deps = deps
    star.beforeGraph
    return star
  }

  def _2pass(configurable: Configurable, R1: File, R2: File, outputDir: File, isIntermediate: Boolean = false, deps: List[File] = Nil): (File, List[Star]) = {
    val starCommand_pass1 = Star(configurable, R1, if (R2 != null) R2 else null, new File(outputDir, "aln-pass1"))
    starCommand_pass1.isIntermediate = isIntermediate
    starCommand_pass1.deps = deps
    starCommand_pass1.beforeGraph

    val starCommand_reindex = new Star(configurable)
    starCommand_reindex.sjdbFileChrStartEnd = starCommand_pass1.outputTab
    starCommand_reindex.outputDir = new File(outputDir, "re-index")
    starCommand_reindex.runmode = "genomeGenerate"
    starCommand_reindex.isIntermediate = isIntermediate
    starCommand_reindex.beforeGraph

    val starCommand_pass2 = Star(configurable, R1, if (R2 != null) R2 else null, new File(outputDir, "aln-pass2"))
    starCommand_pass2.genomeDir = starCommand_reindex.outputDir
    starCommand_pass2.isIntermediate = isIntermediate
    starCommand_pass2.deps = deps
    starCommand_pass2.beforeGraph

    return (starCommand_pass2.outputSam, List(starCommand_pass1, starCommand_reindex, starCommand_pass2))
  }
}