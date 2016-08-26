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
package nl.lumc.sasc.biopet.pipelines.bammetrics

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable

/**
 * Created by pjvan_thof on 11/20/15.
 */
trait TargetRegions extends Configurable {
  /** Bed files for region of interests */
  var roiBedFiles: List[File] = config("regions_of_interest", Nil)

  /** Bed of amplicon that is used */
  var ampliconBedFile: Option[File] = config("amplicon_bed")
}
