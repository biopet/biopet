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

import java.io.File

import htsjdk.samtools.SamReaderFactory
import nl.lumc.sasc.biopet.core.{ Reference, ToolCommandFunction }
import nl.lumc.sasc.biopet.extensions.samtools.SamtoolsMpileup
import nl.lumc.sasc.biopet.utils.ConfigUtils
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import scala.collection.JavaConversions._

class MpileupToVcf(val root: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.MpileupToVcf

  @Input(doc = "Input mpileup file", shortName = "mpileup", required = false)
  var inputMpileup: File = _

  @Input(doc = "Input bam file", shortName = "bam", required = false)
  var inputBam: File = _

  @Output(doc = "Output tag library", shortName = "output", required = true)
  var output: File = _

  @Output
  private var outputIndex: File = _

  var minDP: Option[Int] = config("min_dp")
  var minAP: Option[Int] = config("min_ap")
  var homoFraction: Option[Double] = config("homoFraction")
  var ploidy: Option[Int] = config("ploidy")
  var refCalls: Boolean = config("ref_calls", default = false)
  var sample: String = _
  var reference: String = _

  override def defaultCoreMemory = 3.0

  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta().getAbsolutePath
    if (output.getName.endsWith(".vcf.gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    val samtoolsMpileup = new SamtoolsMpileup(this)
  }

  override def beforeCmd(): Unit = {
    if (sample == null && inputBam.exists() && inputBam.length() > 0) {
      val inputSam = SamReaderFactory.makeDefault.open(inputBam)
      val readGroups = inputSam.getFileHeader.getReadGroups
      val samples = readGroups.map(readGroup => readGroup.getSample).distinct
      sample = samples.head
      inputSam.close()
    }
  }

  override def cmdLine = super.cmdLine +
    required("-o", output) +
    optional("--minDP", minDP) +
    optional("--minAP", minAP) +
    optional("--homoFraction", homoFraction) +
    optional("--ploidy", ploidy) +
    conditional(refCalls, "--refCalls") +
    required("--sample", sample) +
    (if (inputAsStdin) "" else required("-I", inputMpileup))
}
