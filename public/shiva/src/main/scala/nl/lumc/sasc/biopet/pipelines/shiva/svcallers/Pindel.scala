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
package nl.lumc.sasc.biopet.pipelines.shiva.svcallers

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import nl.lumc.sasc.biopet.core.PipelineCommand
import nl.lumc.sasc.biopet.extensions.pindel._
import nl.lumc.sasc.biopet.utils.config.Configurable

/// Pindel is actually a mini pipeline executing binaries from the pindel package
class Pindel(val root: Configurable) extends SvCaller {
  val name = "pindel"

  def this() = this(null)

  def biopetScript() {
    for ((sample, bamFile) <- inputBams) {
      val pindelDir = new File(outputDir, sample)

      val config_file: File = new File(pindelDir, sample + ".pindel.cfg")
      val cfg = new PindelConfig(this)
      cfg.input = bamFile

      // FIXME: get the real insert size of the bam (from bammetrics?)
      cfg.insertsize = 500
      cfg.sampleName = sample
      cfg.output = config_file
      add(cfg)

      val pindel = PindelCaller(this, cfg.output, pindelDir)
      add(pindel)

      // Current date
      val today = Calendar.getInstance().getTime()
      val todayformat = new SimpleDateFormat("yyyyMMdd")

      val pindelVcf = new PindelVCF(this)
      pindelVcf.pindelOutputRoot = Some(pindelDir)
      pindelVcf.referenceDate = todayformat.format(today) // officially, we should enter the date of the genome here
      pindelVcf.outputVCF = new File(pindelDir, s"${sample}.pindel.vcf")
      add(pindelVcf)
    }

  }
}

object Pindel extends PipelineCommand {
  def apply(root: Configurable, input: File, reference: File, runDir: String): Pindel = {
    val pindel = new Pindel(root)
    // run the following for activating the pipeline steps
    pindel.init()
    pindel.biopetScript()
    pindel
  }
}