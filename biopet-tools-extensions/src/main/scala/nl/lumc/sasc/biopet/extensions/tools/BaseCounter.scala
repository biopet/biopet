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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.{ PrintWriter, File }

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 *
 */
class BaseCounter(val root: Configurable) extends ToolCommandFunction {
  def toolObject = nl.lumc.sasc.biopet.tools.BaseCounter

  @Input(doc = "Input Bed file", required = true)
  var refFlat: File = _

  @Input(doc = "Bam File", required = true)
  var bamFile: File = _

  var outputDir: File = _

  var prefix: String = "output"

  override def defaultCoreMemory = 6.0
  override def defaultThreads = 4

  def transcriptTotalCounts = new File(outputDir, s"$prefix.base.transcript.counts")
  def transcriptTotalSenseCounts = new File(outputDir, s"$prefix.base.transcript.sense.counts")
  def transcriptTotalAntiSenseCounts = new File(outputDir, s"$prefix.base.transcript.antisense.counts")
  def transcriptExonicCounts = new File(outputDir, s"$prefix.base.transcript.exonic.counts")
  def transcriptExonicSenseCounts = new File(outputDir, s"$prefix.base.transcript.exonic.sense.counts")
  def transcriptExonicAntiSenseCounts = new File(outputDir, s"$prefix.base.transcript.exonic.antisense.counts")
  def transcriptIntronicCounts = new File(outputDir, s"$prefix.base.transcript.intronic.counts")
  def transcriptIntronicSenseCounts = new File(outputDir, s"$prefix.base.transcript.intronic.sense.counts")
  def transcriptIntronicAntiSenseCounts = new File(outputDir, s"$prefix.base.transcript.intronic.antisense.counts")
  def exonCounts = new File(outputDir, s"$prefix.base.exon.counts")
  def exonSenseCounts = new File(outputDir, s"$prefix.base.exon.sense.counts")
  def exonAntiSenseCounts = new File(outputDir, s"$prefix.base.exon.antisense.counts")
  def intronCounts = new File(outputDir, s"$prefix.base.intron.counts")
  def intronSenseCounts = new File(outputDir, s"$prefix.base.intron.sense.counts")
  def intronAntiSenseCounts = new File(outputDir, s"$prefix.base.intron.antisense.counts")
  def geneTotalCounts = new File(outputDir, s"$prefix.base.gene.counts")
  def geneTotalSenseCounts = new File(outputDir, s"$prefix.base.gene.sense.counts")
  def geneTotalAntiSenseCounts = new File(outputDir, s"$prefix.base.gene.antisense.counts")
  def geneExonicCounts = new File(outputDir, s"$prefix.base.gene.exonic.counts")
  def geneExonicSenseCounts = new File(outputDir, s"$prefix.base.gene.exonic.sense.counts")
  def geneExonicAntiSenseCounts = new File(outputDir, s"$prefix.base.gene.exonic.antisense.counts")
  def geneIntronicCounts = new File(outputDir, s"$prefix.base.gene.intronic.counts")
  def geneIntronicSenseCounts = new File(outputDir, s"$prefix.base.gene.intronic.sense.counts")
  def geneIntronicAntiSenseCounts = new File(outputDir, s"$prefix.base.gene.intronic.antisense.counts")
  def mergeExonCounts = new File(outputDir, s"$prefix.base.exon.merge.counts")
  def mergeExonSenseCounts = new File(outputDir, s"$prefix.base.exon.merge.sense.counts")
  def mergeExonAntiSenseCounts = new File(outputDir, s"$prefix.base.exon.merge.antisense.counts")
  def mergeIntronCounts = new File(outputDir, s"$prefix.base.intron.merge.counts")
  def mergeIntronSenseCounts = new File(outputDir, s"$prefix.base.intron.merge.sense.counts")
  def mergeIntronAntiSenseCounts = new File(outputDir, s"$prefix.base.intron.merge.antisense.counts")
  def nonStrandedMetaExonCounts = new File(outputDir, s"$prefix.base.metaexons.non_stranded.counts")
  def strandedMetaExonCounts = new File(outputDir, s"$prefix.base.metaexons.stranded.counts")
  def strandedSenseMetaExonCounts = new File(outputDir, s"$prefix.base.metaexons.stranded.sense.counts")
  def strandedAntiSenseMetaExonCounts = new File(outputDir, s"$prefix.base.metaexons.stranded.antisense.counts")

  @Output
  private var outputFiles: List[File] = Nil

  override def beforeGraph(): Unit = {
    outputFiles ++= List(transcriptTotalCounts, transcriptTotalSenseCounts, transcriptTotalAntiSenseCounts,
      transcriptExonicCounts, transcriptExonicSenseCounts, transcriptExonicAntiSenseCounts,
      transcriptIntronicCounts, transcriptIntronicSenseCounts, transcriptIntronicAntiSenseCounts,
      exonCounts, exonSenseCounts, exonAntiSenseCounts,
      intronCounts, intronSenseCounts, intronAntiSenseCounts,
      geneTotalCounts, geneTotalSenseCounts, geneTotalAntiSenseCounts,
      geneExonicCounts, geneExonicSenseCounts, geneExonicAntiSenseCounts,
      geneIntronicCounts, geneIntronicSenseCounts, geneIntronicAntiSenseCounts,
      mergeExonCounts, mergeExonSenseCounts, mergeExonAntiSenseCounts,
      mergeIntronCounts, mergeIntronSenseCounts, mergeIntronAntiSenseCounts,
      nonStrandedMetaExonCounts,
      strandedMetaExonCounts, strandedSenseMetaExonCounts, strandedAntiSenseMetaExonCounts)
    jobOutputFile = new File(outputDir, s".$prefix.basecounter.out")
    if (bamFile != null) deps :+= new File(bamFile.getAbsolutePath.stripSuffix(".bam") + ".bai")
    super.beforeGraph()

  }

  override def cmdLine = super.cmdLine +
    required("--refFlat", refFlat) +
    required("-b", bamFile) +
    required("-o", outputDir) +
    optional("--prefix", prefix)
}

