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
