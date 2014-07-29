package nl.lumc.sasc.biopet.extensions

import java.io.File
import scala.sys.process.Process
import org.apache.commons.io.FilenameUtils;
import org.broadinstitute.gatk.queue.function.InProcessFunction
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import nl.lumc.sasc.biopet.core.config.Configurable

class Ln(val root: Configurable) extends InProcessFunction with Configurable {
  this.analysisName = getClass.getSimpleName

  @Input(doc = "Input file")
  var in: File = _

  @Output(doc = "Link destination")
  var out: File = _

  var relative: Boolean = true

  private lazy val inCanonical: String = {
    // need to remove "/~" to correctly expand path with tilde
    in.getCanonicalPath().replace("/~", "")
  }

  private lazy val outCanonical: String = {
    out.getCanonicalPath().replace("/~", "")
  }

  private lazy val inToks: Array[String] = {
    inCanonical.split(File.separator)
  }

  private lazy val outToks: Array[String] = {
    outCanonical.split(File.separator)
  }

  private lazy val commonPrefixLength: Int = {
    val maxLength = scala.math.min(inToks.length, outToks.length)
    var i: Int = 0;
    while (i < maxLength && inToks(i) == outToks(i)) i += 1;
    i
  }

  private lazy val inUnique: String = {
    inToks.slice(commonPrefixLength, inToks.length).mkString(File.separator)
  }

  private lazy val outUnique: String = {
    outToks.slice(commonPrefixLength, outToks.length).mkString(File.separator)
  }

  private lazy val inRelative: String = {
    // calculate 'distance' from output directory to input
    // which is the number of directory walks required to get to the inUnique directory from outDir
    val outDir = FilenameUtils.getFullPathNoEndSeparator(outUnique)
    val dist: Int = scala.math.max(0, outDir.split(File.separator).length - 1)
    val result =
      if (dist > 0)
        ((".." + File.separator) * dist) + File.separator + inUnique
      else
        inUnique
    result
  }

  lazy val cmd: String = {
    if (relative) {
      // workaround until we have `ln` that works with relative path (i.e. `ln -r`)
      "ln -s " + inRelative + " " + outCanonical
    } else {
      "ln -s " + inCanonical + " " + outCanonical
    }
  }

  override def run {
    val process = Process(cmd).run
    System.out.println("cmd: '" + cmd + "', exitcode: " + process.exitValue)
  }
}
