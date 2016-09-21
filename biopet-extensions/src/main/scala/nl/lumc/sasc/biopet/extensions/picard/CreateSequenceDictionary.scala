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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

class CreateSequenceDictionary(val root: Configurable) extends Picard {

  javaMainClass = new picard.sam.CreateSequenceDictionary().getClass.getName

  @Input(required = true)
  var reference: File = _

  @Output(required = true)
  var output: File = _

  var genomeAssembly: Option[String] = config("genomeAssembly")
  var uri: Option[String] = config("uri")
  var species: Option[String] = config("species")
  var truncateAtWhiteSpace: Boolean = config("truncateAtWhiteSpace", default = false)
  var numSequences: Option[Int] = config("numSequences")

  override def cmdLine = super.cmdLine +
    required("REFERENCE=", reference, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false) +
    optional("GENOME_ASSEMBLY=", genomeAssembly, spaceSeparated = false) +
    optional("URI=", uri, spaceSeparated = false) +
    optional("SPECIES=", species, spaceSeparated = false) +
    conditional(truncateAtWhiteSpace, "TRUNCATE_NAMES_AT_WHITESPACE=true") +
    optional("NUM_SEQUENCES=", numSequences, spaceSeparated = false)
}
