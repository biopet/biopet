package nl.lumc.sasc.biopet.extensions.xhmm

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Created by Sander Bollen on 23-11-16.
 */
class XhmmMergeGatkDepths(val root: Configurable) extends Xhmm {

  @Input(doc = "List of input depths files")
  var gatkDepthsFiles: List[File] = Nil

  @Output(doc = "Merged output file in XHMM format")
  var output: File = _

  def cmdLine = {
    executable + required("--mergeGATKdepths") +
      repeat("--GATKdepths", gatkDepthsFiles) +
      required("-o", output)
  }

}
