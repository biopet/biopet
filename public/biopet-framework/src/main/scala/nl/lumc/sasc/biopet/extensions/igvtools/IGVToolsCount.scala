
package nl.lumc.sasc.biopet.extensions.igvtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output, Argument }
import java.io.{ FileNotFoundException, File }

/**
 * IGVTools `count` wrapper
 *
 * @constructor create a new IGVTools instance from a `.bam` file
 *
 */

class IGVToolsCount(val root: Configurable) extends IGVTools {
  @Input(doc = "Bam File")
  var input: File = _

  @Argument(doc = "Genome name")
  var genomename: String = config("genomename")

  @Output(doc = "Output File(s), .wig/.tdf or both separated by comma")
  var output: List[File] = _

  var tdf: File = _
  var wig: File = _

  var maxZoom: Option[Int] = config("maxZoom")
  var windowSize: Option[Int] = config("windowSize")
  var extFactor: Option[Int] = config("extFactor")

  var preExtFactor: Option[Int] = config("preExtFactor")
  var postExtFactor: Option[Int] = config("postExtFactor")

  var windowFunctions: Option[String] = config("windowFunctions")
  var strands: Option[String] = config("strands")
  var bases: Boolean = config("bases", default = false)

  var query: Option[String] = config("query")
  var minMapQuality: Option[Int] = config("minMapQuality")
  var includeDuplicates: Boolean = config("includeDuplicates", default = false)

  var pairs: Boolean = config("pairs", default = false)

  /**
   * Set `output` to `.tdf`.
   *
   * @param output `File` object to output to
   */
  def outputTDF(output: File) = {
    if (tdf.getName != "") {
      this.tdf = output
      this.output +:= output
    } else {
      logger.error("TDF File not specified")
      throw new FileNotFoundException("TDF File not specified")
    }
  }

  /**
   * Set `output` to `.wig`.
   *
   * @param output `File` object to output to
   */
  def outputWIG(output: File) = {
    if (wig.getName != "") {
      this.wig = output
      this.output +:= output
    } else {
      logger.error("WIG File not specified")
      throw new FileNotFoundException("WIG File not specified")
    }
  }

  def cmdLine = {
    required(executable) +
      required("count") +
      optional("-z", maxZoom) +
      optional("-w", windowSize) +
      optional("-e", extFactor) +
      optional("--preExtFactor", preExtFactor) +
      optional("--postExtFactor", postExtFactor) +
      optional("--windowFunctions", windowFunctions) +
      optional("--strands", strands) +
      conditional(bases, "--bases") +
      optional("--query", query) +
      optional("--minMapQuality", minMapQuality) +
      conditional(includeDuplicates, "--includeDuplicates") +
      conditional(pairs, "--pairs") +
      required(input) +
      required(output.mkString(",")) +
      required(genomename)

  }
}

object IGVToolsCount {

  /**
   * Create an object by specifying the `input` (.bam),
   * and the `genomename` (hg18,hg19,mm10)
   *
   * @param input Bamfile to count reads from
   * @param genomename Name of path to the genome.chrsizes.bed,
   * @return a new IGVToolsCount instance
   */
  def apply(root: Configurable, input: File,
            genomename: String): IGVToolsCount = {
    val counting = new IGVToolsCount(root)
    counting.input = input
    counting.genomename = genomename
    counting.outputWIG(new File(input.getAbsolutePath.substring(0, input.getAbsolutePath.lastIndexOf(".bam")) + ".wig"))
    return counting
  }

  /**
   * Create an object by specifying the `input` (.bam),
   * and the `genomename` (hg18,hg19,mm10)
   *
   * @param input Bamfile to count reads from
   * @param genomename Name of path to the genome.chrsizes.bed,
   * @param tdf File-path to output.tdf
   * @param wig File-path to output.wig
   * @return a new IGVToolsCount instance
   */
  def apply(root: Configurable, input: File,
            genomename: String, tdf: File, wig: File): IGVToolsCount = {
    val counting = new IGVToolsCount(root)
    counting.input = input
    counting.genomename = genomename
    counting.outputTDF(tdf)
    counting.outputWIG(wig)
    return counting
  }
}