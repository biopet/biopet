package nl.lumc.sasc.biopet.pipelines.gears

import nl.lumc.sasc.biopet.core.SampleLibraryTag
import nl.lumc.sasc.biopet.core.summary.SummaryQScript
import nl.lumc.sasc.biopet.extensions.{Cutadapt, Flash}
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

  private lazy val forwardPrimers: List[String] = config("forward_primers")
  private lazy val reversePrimers: List[String] = config("reverse_primers")
  
  def combinedFastq: File = if ((forwardPrimers :: reversePrimers).nonEmpty)
    swapExt(outputDir, flash.combinedFastq, ".fastq.gz", ".clip.fastq.gz")
  else flash.combinedFastq

  def notCombinedR1Fastq: File = if ((forwardPrimers :: reversePrimers).nonEmpty)
    swapExt(outputDir, flash.notCombinedR1, ".fastq.gz", ".clip.fastq.gz")
  else flash.notCombinedR1

  def notCombinedR2Fastq: File = if ((forwardPrimers :: reversePrimers).nonEmpty)
    swapExt(outputDir, flash.notCombinedR2, ".fastq.gz", ".clip.fastq.gz")
  else flash.notCombinedR2

  /** Pipeline itself */
  def biopetScript(): Unit = {
    flash.outputDirectory = new File(outputDir, "flash")
    flash.fastqR1 = fastqR1
    flash.fastqR2 = fastqR2
    flash.isIntermediate = (forwardPrimers ::: reversePrimers).nonEmpty
    add(flash)

    if ((forwardPrimers ::: reversePrimers).nonEmpty) {
      val cutadapt = new Cutadapt(this)
      cutadapt.fastq_input = flash.combinedFastq
      cutadapt.fastq_output = this.combinedFastq
      cutadapt.stats_output = swapExt(outputDir, cutadapt.fastq_output, ".fastq.gz", ".stats")
      cutadapt.opt_anywhere ++= (forwardPrimers ::: reversePrimers)
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
  }

  /** Must return a map with used settings for this pipeline */
  def summarySettings: Map[String, Any] = Map()

  /** File to put in the summary for thie pipeline */
  def summaryFiles: Map[String, File] = Map()

  /** Name of summary output file */
  def summaryFile: File = new File(outputDir, "combine_reads.summary.json")
}
