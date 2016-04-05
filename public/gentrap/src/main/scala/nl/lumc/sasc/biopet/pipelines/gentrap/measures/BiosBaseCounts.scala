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

import java.io.File

import nl.lumc.sasc.biopet.core.annotations.AnnotationBed
import nl.lumc.sasc.biopet.extensions.bedtools.BedtoolsCoverage
import nl.lumc.sasc.biopet.extensions.{ Cat, Grep }
import nl.lumc.sasc.biopet.extensions.picard.MergeSamFiles
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.pipelines.gentrap.scripts.Hist2count
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
 * Created by pjvan_thof on 1/12/16.
 */
class BiosBaseCounts(val root: Configurable) extends QScript with Measurement with AnnotationBed {

  def mergeArgs = MergeArgs(List(1), 2, numHeaderLines = 0, fallback = "0")

  override def fixedValues = Map("samtoolsview" -> Map("b" -> true, "h" -> true))

  override def defaults = Map("hist2count" -> Map("column" -> 4))

  /** Pipeline itself */
  def biopetScript(): Unit = {
    val jobs = bamFiles.map {
      case (id, file) =>
        //val plusBam: File = extractStrand(file, '+', new File(outputDir, id))
        //val minBam: File = extractStrand(file, '-', new File(outputDir, id))

        val nonStrandedCount = addBaseCounts(file, new File(outputDir, id), id, "non_stranded", None)
        //val plusStrandedCount = addBaseCounts(plusBam, new File(outputDir, id), id, "plus_strand", Some('+'))
        //val minStrandedCount = addBaseCounts(minBam, new File(outputDir, id), id, "min_strand", Some('-'))

        //val cat = new Cat(this)
        //cat.input = List(plusStrandedCount, minStrandedCount)
        //cat.output = new File(outputDir, id + File.separator + s"$id.stranded.counts")
        //add(cat)

        id -> nonStrandedCount
    }

    val nonStrandedCounts = new File(outputDir, "non_stranded.counts")
    //addMergeTableJob(jobs.map(_._2).toList, nonStrandedCounts, "non_stranded", ".non_stranded.counts")
    //addHeatmapJob(nonStrandedCounts, new File(outputDir, "non_stranded.png"), "non_stranded")

    //    val strandedCounts = new File(outputDir, "stranded.counts")
    //    addMergeTableJob(jobs.map(_._2._1).toList, strandedCounts, "stranded", ".stranded.counts")
    //    addHeatmapJob(strandedCounts, new File(outputDir, "stranded.png"), "stranded")

    addSummaryJobs()
  }

  protected def addBaseCounts(bamFile: File, outputDir: File, sampleName: String,
                              name: String, strand: Option[Char]): File = {
    val outputFile = new File(outputDir, s"$sampleName.$name.counts")

    val grep = strand.map(x => Grep(this, """\""" + x + """$""", perlRegexp = true))
    val bedtoolsCoverage = new BedtoolsCoverage(this)
    bedtoolsCoverage.hist = true
    bedtoolsCoverage.split = true
    bedtoolsCoverage.input = bamFile
    bedtoolsCoverage.intersectFile = new File("stdin")

    val hist2count = new Hist2count(this)

    grep match {
      case Some(g) => add(annotationBed :<: g | bedtoolsCoverage | hist2count > outputFile)
      case _ =>
        bedtoolsCoverage.intersectFile = annotationBed
        add(bedtoolsCoverage | hist2count > outputFile)
    }

    outputFile
  }

  //  protected def extractStrand(bamFile: File, strand: Char, outputDir: File): File = {
  //    val name = strand match {
  //      case '+' => "plus"
  //      case '-' => "min"
  //      case _   => throw new IllegalArgumentException("Only '+' or '-' allowed as strand")
  //    }
  //
  //    val forwardView = new SamtoolsView(this)
  //    forwardView.input = bamFile
  //    forwardView.output = swapExt(outputDir, bamFile, ".bam", s"$name.R1.bam")
  //    forwardView.isIntermediate = true
  //
  //    val reverseView = new SamtoolsView(this)
  //    reverseView.input = bamFile
  //    reverseView.output = swapExt(outputDir, bamFile, ".bam", s"$name.R2.bam")
  //    reverseView.isIntermediate = true
  //
  //    strand match {
  //      case '+' =>
  //        forwardView.f = List("0x10")
  //        forwardView.F = List("0x80")
  //        reverseView.f = List("0x80")
  //        reverseView.F = List("0x10")
  //      case '-' =>
  //        forwardView.F = List("0x90")
  //        reverseView.f = List("0x90")
  //    }
  //
  //    val mergeSam = MergeSamFiles(this, List(forwardView.output, reverseView.output),
  //      swapExt(outputDir, bamFile, ".bam", s"$name.bam"))
  //    mergeSam.isIntermediate = true
  //
  //    add(forwardView, reverseView, mergeSam)
  //
  //    mergeSam.output
  //  }
}
