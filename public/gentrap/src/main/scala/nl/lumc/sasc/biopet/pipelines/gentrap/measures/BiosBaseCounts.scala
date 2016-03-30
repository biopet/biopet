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
package nl.lumc.sasc.biopet.pipelines.gentrap.measures

import nl.lumc.sasc.biopet.core.annotations.AnnotationBed
import nl.lumc.sasc.biopet.extensions.Cat
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class BiosBaseCounts(val root: Configurable) extends QScript with Measurement with AnnotationBed {

  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 0, fallback = "0")

  override def fixedValues = Map("SamtoolsView" -> Map("b" -> true, "h" -> true))

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = bamFiles.map {
      case (id, file) =>
        val plusBam: File = extractStrand(file, '+', new File(outputDir, id))
        val minBam: File = extractStrand(file, '-', new File(outputDir, id))

        val nonStrandedCount = addBaseCounts(file, new File(outputDir, id), "non_stranded", None)
        val plusStrandedCount = addBaseCounts(plusBam, new File(outputDir, id), "plus_strand", Some('+'))
        val minStrandedCount = addBaseCounts(minBam, new File(outputDir, id), "min_strand", Some('-'))

        val cat = new Cat(this)
        cat.input = List(plusStrandedCount, minStrandedCount)
        cat.output = swapExt(outputDir, file, ".bam", s".stranded.counts")
        add(cat)

        id -> (nonStrandedCount, cat.output)
    }

    //TODO: Merge table
    //TODO: Heatmap

    addSummaryJobs()
  }

  protected def addBaseCounts(bamFile: File, outputDir: File, name: String, strand: Option[Char]): File = {
    //TODO: Add counting
    swapExt(outputDir, bamFile, ".bam", s".$name.counts")
  }

  protected def extractStrand(bamFile: File, strand: Char, outputDir: File): File = {
    val name = strand match {
      case '+' => "plus"
      case '-' => "min"
      case _ => throw new IllegalArgumentException("Only '+' or '-' allowed as strand")
    }

    val forwardView = new SamtoolsView(this)
    forwardView.input = bamFile
    forwardView.output = swapExt(outputDir, bamFile, ".bam", s"$name.R1.bam")
    forwardView.isIntermediate = true

    val reverseView = new SamtoolsView(this)
    reverseView.input = bamFile
    reverseView.output = swapExt(outputDir, bamFile, ".bam", s"$name.R2.bam")
    reverseView.isIntermediate = true

    strand match {
      case '+' =>
        forwardView.f = List("0x10")
        forwardView.F = List("0x80")
        reverseView.f = List("0x80")
        reverseView.F = List("0x10")
      case '-' =>
        forwardView.F = List("0x90")
        reverseView.f = List("0x90")
    }

    val mergeSam = new MergeSamFiles(this)
    mergeSam.input = List(forwardView.output, reverseView.output)
    mergeSam.output = swapExt(outputDir, bamFile, ".bam", s"$name.bam")
    mergeSam.isIntermediate = true

    add(forwardView, reverseView, mergeSam)

    mergeSam.output
  }
}
