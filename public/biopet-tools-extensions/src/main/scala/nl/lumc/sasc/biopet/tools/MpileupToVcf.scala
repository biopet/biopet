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
package nl.lumc.sasc.biopet.tools

import java.io.{ File, PrintWriter }

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.utils.config.Configurable
import nl.lumc.sasc.biopet.core.{ Reference, ToolCommand, ToolCommandFuntion }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.utils.ConfigUtils
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.math.{ floor, round }

class MpileupToVcf(val root: Configurable) extends ToolCommandFuntion with Reference {
  javaMainClass = getClass.getName

  @Input(doc = "Input mpileup file", shortName = "mpileup", required = false)
  var inputMpileup: File = _

  @Input(doc = "Input bam file", shortName = "bam", required = false)
  var inputBam: File = _

  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _

  var minDP: Option[Int] = config("min_dp")
  var minAP: Option[Int] = config("min_ap")
  var homoFraction: Option[Double] = config("homoFraction")
  var ploidy: Option[Int] = config("ploidy")
  var sample: String = _
  var reference: String = _

  override def defaultCoreMemory = 3.0

  override def defaults = ConfigUtils.mergeMaps(Map("samtoolsmpileup" -> Map("disable_baq" -> true, "min_map_quality" -> 1)),
    super.defaults)

  override def beforeGraph() {
    super.beforeGraph()
    reference = referenceFasta().getAbsolutePath
    val samtoolsMpileup = new SamtoolsMpileup(this)
  }

  override def beforeCmd(): Unit = {
    if (sample == null && inputBam.exists()) {
      val inputSam = SamReaderFactory.makeDefault.open(inputBam)
      val readGroups = inputSam.getFileHeader.getReadGroups
      val samples = readGroups.map(readGroup => readGroup.getSample).distinct
      sample = samples.head
      inputSam.close()
    }
  }

  override def commandLine = {
    (if (inputMpileup == null) {
      val samtoolsMpileup = new SamtoolsMpileup(this)
      samtoolsMpileup.reference = referenceFasta()
      samtoolsMpileup.input = List(inputBam)
      samtoolsMpileup.cmdPipe + " | "
    } else "") +
      super.commandLine +
      required("-o", output) +
      optional("--minDP", minDP) +
      optional("--minAP", minAP) +
      optional("--homoFraction", homoFraction) +
      optional("--ploidy", ploidy) +
      required("--sample", sample) +
      (if (inputBam == null) required("-I", inputMpileup) else "")
  }
}
