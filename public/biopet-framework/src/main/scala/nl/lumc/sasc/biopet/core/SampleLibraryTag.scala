package nl.lumc.sasc.biopet.core

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.Argument

/**
 * Created by pjvan_thof on 2/16/15.
 */
trait SampleLibraryTag extends Configurable {
  @Argument(doc = "Sample ID", shortName = "sample", required = false)
  var sampleId: Option[String] = root match {
    case tag: SampleLibraryTag => tag.sampleId
    case _                     => None
  }

  @Argument(doc = "Library ID", shortName = "library", required = false)
  var libId: Option[String] = root match {
    case tag: SampleLibraryTag => tag.libId
    case _                     => None
  }
}
