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
package nl.lumc.sasc.biopet.extensions.conifer

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class ConiferExport(val parent: Configurable) extends Conifer {

  @Input(doc = "Input analysis.hdf5", required = true)
  var input: File = _

  @Output(doc = "Output <sample>.svdzrpkm.bed", shortName = "out", required = true)
  var output: File = _

  override def beforeGraph() {
    this.preProcessExecutable()
  }

  override def cmdLine = super.cmdLine +
    " export " +
    " --input" + required(input) +
    " --output" + required(output)
}
