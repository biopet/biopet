package nl.lumc.sasc.biopet.extensions.bedtools

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Output, Input }

/**
 * Created by ahbbollen on 5-1-16.
 */
class BedtoolsMerge(val root: Configurable) extends Bedtools {

  @Input(doc = "Input bed file")
  var input: File = _

  @Argument(doc = "Distance")
  var dist: Int = 1 //default of tool is 1

  @Output(doc = "Output bed file")
  var output: File = _

  def cmdLine = {
    required(executable) + required("merge") +
      required("-i", input) + optional("-d", dist) +
      " > " + required(output)
  }

}
