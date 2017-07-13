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

import nl.lumc.sasc.biopet.core.BiopetQScript
import nl.lumc.sasc.biopet.extensions.picard.SamToFastq
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsView
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.queue.QScript

/**
  * Created by pjvanthof on 04/12/15.
  */
class ExtractUnmappedReads(val parent: Configurable) extends QScript with BiopetQScript {

  var bamFile: File = _

  var outputName: String = _

  override def defaults = Map(
    "samtofastq" -> Map(
      "validationstringency" -> "LENIENT"
    )
  )

  lazy val paired: Boolean = config("paired_bam", default = true)

  def init(): Unit = {
    require(bamFile != null)
    if (outputName == null) outputName = bamFile.getName.stripSuffix(".bam")
  }

  def fastqUnmappedR1 = new File(outputDir, s"$outputName.unmapped.R1.fq.gz")
  def fastqUnmappedR2: Option[File] =
    if (paired) Some(new File(outputDir, s"$outputName.unmapped.R2.fq.gz"))
    else None
  def fastqUnmappedSingletons = new File(outputDir, s"$outputName.unmapped.singletons.fq.gz")

  def biopetScript(): Unit = {
    val samtoolsViewSelectUnmapped = new SamtoolsView(this)
    samtoolsViewSelectUnmapped.input = bamFile
    samtoolsViewSelectUnmapped.b = true
    samtoolsViewSelectUnmapped.h = true
    samtoolsViewSelectUnmapped.output = swapExt(outputDir, bamFile, ".bam", ".unmapped.bam")
    if (paired) samtoolsViewSelectUnmapped.f = List("12")
    else samtoolsViewSelectUnmapped.f = List("4")
    samtoolsViewSelectUnmapped.isIntermediate = true
    add(samtoolsViewSelectUnmapped)

    // start bam to fastq (only on unaligned reads) also extract the matesam
    val samToFastq = new SamToFastq(this)
    samToFastq.input = samtoolsViewSelectUnmapped.output
    samToFastq.fastqR1 = fastqUnmappedR1
    if (paired) {
      samToFastq.fastqR2 = fastqUnmappedR2.get
      samToFastq.fastqUnpaired = fastqUnmappedSingletons
    }
    samToFastq.isIntermediate = !config("keep_unmapped_fastq", default = false).asBoolean
    add(samToFastq)
  }
}
