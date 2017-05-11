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
package nl.lumc.sasc.biopet.extensions.bedtools

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * Created by Sander Bollen on 26-5-16.
  */
class BedtoolsSort(val parent: Configurable) extends Bedtools with Reference {

  @Input
  var input: File = null

  @Output
  var output: File = null

  @Argument(required = false)
  var faidx: File = referenceFai

  def cmdLine =
    required(executable) + required("sort") + required("-i", input) +
      optional("-faidx", faidx) +
      (if (outputAsStdout) "" else " > " + required(output))

}
