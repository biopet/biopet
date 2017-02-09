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
package nl.lumc.sasc.biopet.extensions.gatk

import java.io.File

import nl.lumc.sasc.biopet.core.{ BiopetJavaCommandLineFunction, Reference }
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Gather, Input, Output }

class CatVariants(val parent: Configurable) extends BiopetJavaCommandLineFunction with Reference {
  analysisName = "CatVariants"
  javaMainClass = "org.broadinstitute.gatk.tools.CatVariants"

  /** genome reference file <name>.fasta */
  @Input(fullName = "reference", shortName = "R", doc = "genome reference file <name>.fasta", required = true, exclusiveOf = "", validation = "")
  var reference: File = _

  /** Input VCF file/s */
  @Input(fullName = "variant", shortName = "V", doc = "Input VCF file/s", required = true, exclusiveOf = "", validation = "")
  var variant: Seq[File] = Nil

  /** output file */
  @Output(fullName = "outputFile", shortName = "out", doc = "output file", required = true, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var outputFile: File = _

  /** assumeSorted should be true if the input files are already sorted (based on the position of the variants) */
  @Argument(fullName = "assumeSorted", shortName = "assumeSorted", doc = "assumeSorted should be true if the input files are already sorted (based on the position of the variants)", required = false, exclusiveOf = "", validation = "")
  var assumeSorted: Boolean = _

  /** which type of IndexCreator to use for VCF/BCF indices */
  @Argument(fullName = "variant_index_type", shortName = "", doc = "which type of IndexCreator to use for VCF/BCF indices", required = false, exclusiveOf = "", validation = "")
  var variant_index_type: Option[String] = None

  /** the parameter (bin width or features per bin) to pass to the VCF/BCF IndexCreator */
  @Argument(fullName = "variant_index_parameter", shortName = "", doc = "the parameter (bin width or features per bin) to pass to the VCF/BCF IndexCreator", required = false, exclusiveOf = "", validation = "")
  var variant_index_parameter: Option[Int] = None

  /** Set the minimum level of logging */
  @Argument(fullName = "logging_level", shortName = "l", doc = "Set the minimum level of logging", required = false, exclusiveOf = "", validation = "")
  var logging_level: String = _

  /** Set the logging location */
  @Output(fullName = "log_to_file", shortName = "log", doc = "Set the logging location", required = false, exclusiveOf = "", validation = "")
  @Gather(classOf[org.broadinstitute.gatk.queue.function.scattergather.SimpleTextGatherFunction])
  var log_to_file: File = _

  override def defaultCoreMemory = 4.0

  override def beforeGraph() = {
    super.beforeGraph()
    if (reference == null) reference = referenceFasta()
  }

  override def cmdLine = super.cmdLine +
    required("-R", reference, spaceSeparated = true, escape = true, format = "%s") +
    repeat("-V", variant, spaceSeparated = true, escape = true, format = "%s") +
    required("-out", outputFile, spaceSeparated = true, escape = true, format = "%s") +
    conditional(assumeSorted, "-assumeSorted", escape = true, format = "%s") +
    optional("--variant_index_type", variant_index_type, spaceSeparated = true, escape = true, format = "%s") +
    optional("--variant_index_parameter", variant_index_parameter, spaceSeparated = true, escape = true, format = "%s") +
    optional("-l", logging_level, spaceSeparated = true, escape = true, format = "%s") +
    optional("-log", log_to_file, spaceSeparated = true, escape = true, format = "%s")
}
