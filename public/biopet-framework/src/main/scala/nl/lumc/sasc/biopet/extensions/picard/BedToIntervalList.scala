package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.core.Reference
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for piacrd's BedToIntervalList tool
 *
 * Created by pjvan_thof on 4/15/15.
 */
class BedToIntervalList(val root: Configurable) extends Picard with Reference {
  javaMainClass = new picard.util.BedToIntervalList().getClass.getName

  @Input(doc = "Input bed file", required = true)
  var input: File = null

  @Input(doc = "Reference dict file", required = true)
  var dict: File = new File(referenceFasta().toString.stripSuffix(".fa").stripSuffix(".fasta") + ".dict")

  @Output(doc = "Output interval list", required = true)
  var output: File = null

  override def commandLine = super.commandLine +
    required("SEQUENCE_DICTIONARY=", dict, spaceSeparated = false) +
    required("INPUT=", input, spaceSeparated = false) +
    required("OUTPUT=", output, spaceSeparated = false)
}

object BedToIntervalList {
  def apply(root: Configurable, input: File, output: File): BedToIntervalList = {
    val bi = new BedToIntervalList(root)
    bi.input = input
    bi.output = output
    bi
  }
}