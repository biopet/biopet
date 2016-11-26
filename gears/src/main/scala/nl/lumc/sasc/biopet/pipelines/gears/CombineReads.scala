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

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.{ Cutadapt, Flash }
import nl.lumc.sasc.biopet.pipelines.flexiprep.Fastqc
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvanthof on 29/12/15.
 */
class CombineReads(val root: Configurable) extends QScript with SummaryQScript with SampleLibraryTag {
  @Input(doc = "R1 reads in FastQ format", shortName = "R1", required = false)
  var fastqR1: File = _

  @Input(doc = "R2 reads in FastQ format", shortName = "R2", required = false)
  var fastqR2: File = _

  override def fixedValues = Map("flash" -> Map("compress" -> true))

  /** Init for pipeline */
  def init(): Unit = {
  }

  private lazy val flash = new Flash(this)

  private lazy val forwardPrimers: List[String] = config("forward_primers", default = Nil)
  private lazy val reversePrimers: List[String] = config("reverse_primers", default = Nil)

  def combinedFastq: File = if ((forwardPrimers ::: reversePrimers).nonEmpty)
    swapExt(outputDir, flash.combinedFastq, ".fastq.gz", ".clip.fastq.gz")
  else flash.combinedFastq

  def notCombinedR1Fastq: File = if ((forwardPrimers ::: reversePrimers).nonEmpty)
    swapExt(outputDir, flash.notCombinedR1, ".fastq.gz", ".clip.fastq.gz")
  else flash.notCombinedR1

  def notCombinedR2Fastq: File = if ((forwardPrimers ::: reversePrimers).nonEmpty)
    swapExt(outputDir, flash.notCombinedR2, ".fastq.gz", ".clip.fastq.gz")
  else flash.notCombinedR2

  /** Pipeline itself */
  def biopetScript(): Unit = {
    flash.outputDirectory = new File(outputDir, "flash")
    flash.fastqR1 = fastqR1
    flash.fastqR2 = fastqR2
    flash.isIntermediate = (forwardPrimers ::: reversePrimers).nonEmpty
    flash.mainFunction = true
    add(flash)

    if ((forwardPrimers ::: reversePrimers).nonEmpty) {
      val cutadapt = new Cutadapt(this)
      cutadapt.fastqInput = flash.combinedFastq
      cutadapt.fastqOutput = this.combinedFastq
      cutadapt.statsOutput = swapExt(outputDir, cutadapt.fastqOutput, ".fastq.gz", ".stats")
      (forwardPrimers ::: reversePrimers).foreach(cutadapt.anywhere += _)
      cutadapt.mainFunction = true
      add(cutadapt)
      addSummarizable(cutadapt, "cutadapt")
    }

    val combinedFastqc = Fastqc(this, this.combinedFastq, new File(outputDir, "combined_fastqc"))
    add(combinedFastqc)
    addSummarizable(combinedFastqc, "fastqc_combined")

    val notCombinedR1Fastqc = Fastqc(this, this.combinedFastq, new File(outputDir, "not_combined_R1_fastqc"))
    add(notCombinedR1Fastqc)
    addSummarizable(notCombinedR1Fastqc, "fastqc_not_combined_R1")

    val notCombinedR2Fastqc = Fastqc(this, this.combinedFastq, new File(outputDir, "not_combined_R2_fastqc"))
    add(notCombinedR2Fastqc)
    addSummarizable(notCombinedR2Fastqc, "fastqc_not_combined_R2")

    addSummaryJobs()
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map()

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, "combine_reads.summary.json")
}
