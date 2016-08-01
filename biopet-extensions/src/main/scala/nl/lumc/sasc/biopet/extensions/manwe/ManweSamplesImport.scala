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
package nl.lumc.sasc.biopet.extensions.manwe

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Argument, Output }

/**
 * Created by ahbbollen on 24-9-15.
 */
class ManweSamplesImport(val root: Configurable) extends Manwe {

  /**
   * Creates sample and imports vcf and bed files immediately
   */

  @Argument(doc = "name of sample", required = true)
  var name: Option[String] = _

  @Argument(doc = "Group uris", required = false)
  var group: List[String] = Nil

  @Input(doc = "Vcf files to upload", required = false)
  var vcfs: List[File] = Nil

  @Input(doc = "BED files to upload", required = false)
  var beds: List[File] = Nil

  @Argument(doc = "flag for data already uploaded", required = false)
  var alreadyUploaded: Boolean = false

  @Argument(doc = "flag to mark sample as public", required = false)
  var public: Boolean = false

  @Argument(doc = "flag if sample has no coverage profile", required = false)
  var noCoverage: Boolean = false

  @Argument(doc = "Prefer genotypes derived from likelihood (PL) fields in stead of GT field", required = false)
  var preferLikelihood: Boolean = false

  @Argument(doc = "Pool size", required = false)
  var poolSize: Option[Int] = _

  @Argument(doc = " Flag whether to wait for import to complete on server", required = false)
  var waitToComplete: Boolean = false

  def subCommand = {
    required("samples") +
      required("import") +
      required(name) +
      repeat("-g", group) +
      repeat("--vcf", vcfs) +
      repeat("--bed", beds) +
      optional("-s", poolSize) +
      conditional(alreadyUploaded, "-u") +
      conditional(public, "-p") +
      conditional(preferLikelihood, "-l") +
      conditional(noCoverage, "--no-coverage-profile") +
      conditional(waitToComplete, "--wait")
  }

}
