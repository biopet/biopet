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

import nl.lumc.sasc.biopet.core.{ Reference, ToolCommandFunction }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Biopet extension for tool VcfWithVcf
 */
class VcfWithVcf(val root: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.VcfWithVcf

  @Input(doc = "Input vcf file", shortName = "input", required = true)
  var input: File = _

  @Input(doc = "Secondary vcf file", shortName = "secondary", required = true)
  var secondaryVcf: File = _

  @Output(doc = "Output vcf file", shortName = "output", required = true)
  var output: File = _

  @Output(doc = "Output vcf file index", shortName = "output", required = true)
  private var outputIndex: File = _

  @Input
  var reference: File = _

  var fields: List[(String, String, Option[String])] = List()

  override def defaultCoreMemory = 4.0

  override def beforeGraph() {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
    if (output.getName.endsWith(".gz")) outputIndex = new File(output.getAbsolutePath + ".tbi")
    if (output.getName.endsWith(".vcf")) outputIndex = new File(output.getAbsolutePath + ".idx")
    if (fields.isEmpty) throw new IllegalArgumentException("No fields found for VcfWithVcf")
  }

  override def cmdLine = super.cmdLine +
    required("-I", input) +
    required("-o", output) +
    required("-s", secondaryVcf) +
    required("-R", reference) +
    repeat("-f", fields.map(x => x._1 + ":" + x._2 + ":" + x._3.getOrElse("none")))
}
