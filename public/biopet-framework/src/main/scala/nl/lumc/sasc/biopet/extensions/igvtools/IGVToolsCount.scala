
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
  var genomeName: String = config("genomeName")

  @Output
  protected var tdf: File = _

  @Output
  protected var wig: File = _

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

  override def afterGraph {
    super.afterGraph
    if (!input.exists()) throw new FileNotFoundException("Input bam is required for IGVToolsCount")

    this.tdf = new File(input.getAbsolutePath + ".tdf")
    this.wig = new File(input.getAbsolutePath.stripSuffix(".bam") + ".wig")

    // check genome name or File
    val genome = new File(genomeName)
    if (!genome.exists()) {
      // check in the IGVTools genome/directory
      val genomeInDir = new File(new File(executable).getParent + File.separator + "genomes" + File.separator + genomeName + ".chrom.sizes")
      if (!genomeInDir.exists()) {
        throw new FileNotFoundException("genomeName contains a invalid filepath/genomename or not supported by IGVTools")
      } else {
        genomeName = genomeInDir.getAbsolutePath
      }
    } else {
      // redefine the genomeName.
      genomeName = genome.getAbsolutePath
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
      required(outputArg) +
      required(genomeName)
  }

  private def outputArg: String = {
    (tdf.isInstanceOf[File], wig.isInstanceOf[File]) match {
      case (false, false) => throw new IllegalArgumentException("Either TDF or WIG should be supplied");
      case (true, false)  => tdf.getAbsolutePath;
      case (false, true)  => wig.getAbsolutePath;
      case (true, true)   => tdf.getAbsolutePath + "," + wig.getAbsolutePath;
    }
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
   * @throws FileNotFoundException bam File is not found
   * @throws IllegalArgumentException tdf or wig not supplied
   */
  def apply(root: Configurable, input: File,
            genomename: String): IGVToolsCount = {
    val counting = new IGVToolsCount(root)
    counting.input = input
    counting.genomeName = genomename
    return counting
  }
}