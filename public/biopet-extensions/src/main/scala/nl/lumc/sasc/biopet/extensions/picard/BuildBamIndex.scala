package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * Created by sajvanderzeeuw on 6-10-15.
 */
class BuildBamIndex(val root: Configurable) extends Picard {

  javaMainClass = new picard.sam.BuildBamIndex().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: File = _

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  override def cmdLine = super.cmdLine +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false)
}
