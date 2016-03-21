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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.{ Reference, ToolCommandFunction }
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 *
 */
class GensToVcf(val root: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.BaseCounter

  @Input(doc = "Input genotypes file", required = true)
  var inputGens: File = _

  @Input(doc = "input Info file", required = false)
  var inputInfo: Option[File] = None

  @Input(required = true)
  var samplesFile: File = _

  @Input(required = true)
  var reference: File = _

  @Output(required = true)
  var outputVcf: File = _

  var contig: String = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
    if (contig == null) throw new IllegalStateException
  }

  override def cmdLine = super.cmdLine +
    required("--inputGenotypes", inputGens) +
    required("--inputInfo", inputInfo) +
    required("--outputVcf", outputVcf) +
    optional("--contig", contig) +
    required("--reference", reference) +
    required("--samplesFile", samplesFile)

}

