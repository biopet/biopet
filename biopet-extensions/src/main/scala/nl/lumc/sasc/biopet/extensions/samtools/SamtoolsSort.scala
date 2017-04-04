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
package nl.lumc.sasc.biopet.extensions.samtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by pjvanthof on 22/09/15.
 */
class SamtoolsSort(val parent: Configurable) extends Samtools {

  @Input(required = true)
  var input: File = _

  @Output
  var output: File = _

  var compresion: Option[Int] = config("l")
  var outputFormat: Option[String] = config("O")
  var sortByName: Boolean = config("sort_by_name", default = false)
  var prefix: String = _

  override def beforeGraph(): Unit = {
    super.beforeGraph()
    prefix = config("prefix", default = new File(System.getProperty("java.io.tmpdir"), output.getName))
  }

  def cmdLine = required(executable) + required("sort") +
    optional("-m", (coreMemory + "G")) +
    optional("-@", threads) +
    optional("-O", outputFormat) +
    required("-T", prefix) +
    conditional(sortByName, "-n") +
    (if (outputAsStsout) "" else required("-o", output)) +
    (if (inputAsStdin) "" else required(input))
}
