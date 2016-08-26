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
import nl.lumc.sasc.biopet.utils.Logging
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 *
 */
class SnptestToVcf(val root: Configurable) extends ToolCommandFunction with Reference {
  def toolObject = nl.lumc.sasc.biopet.tools.SnptestToVcf

  @Input(doc = "input Info file", required = true)
  var inputInfo: File = null

  @Input(required = true)
  var reference: File = _

  @Output(required = true)
  var outputVcf: File = _

  var contig: String = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
    if (contig == null) throw new IllegalStateException("Contig is missing")
    //if (outputVcf.getName.endsWith(".vcf.gz")) outputFiles :+= new File(outputVcf.getAbsolutePath + ".tbi")
  }

  override def cmdLine = super.cmdLine +
    required("--inputInfo", inputInfo) +
    required("--outputVcf", outputVcf) +
    optional("--contig", contig) +
    required("--referenceFasta", reference)

}

