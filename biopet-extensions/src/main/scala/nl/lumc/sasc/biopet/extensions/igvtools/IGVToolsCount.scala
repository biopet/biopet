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
package nl.lumc.sasc.biopet.extensions.igvtools

import java.io.{File, FileNotFoundException}

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Input, Output}

/**
  * IGVTools `count` wrapper
  *
  * @constructor create a new IGVTools instance from a `.bam` file
  *
  */
class IGVToolsCount(val parent: Configurable) extends IGVTools {
  @Input(doc = "Bam File")
  var input: File = _

  @Input(doc = "<genome>.chrom.sizes File", required = true)
  var genomeChromSizes: File = _

  @Output
  var tdf: Option[File] = None

  @Output
  var wig: Option[File] = None

  @Output
  def logFile = new File(jobLocalDir, "igv.log")

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

  override def defaultCoreMemory = 4.0

  override def beforeGraph() {
    super.beforeGraph()

    (tdf, wig) match {
      case (Some(t), _) => jobLocalDir = t.getParentFile
      case (_, Some(w)) => jobLocalDir = w.getParentFile
      case _ => throw new IllegalArgumentException("Must have a wig or tdf file")
    }

    wig.foreach(
      x =>
        if (!x.getAbsolutePath.endsWith(".wig"))
          throw new IllegalArgumentException("WIG file should have a .wig file-extension"))
    tdf.foreach(
      x =>
        if (!x.getAbsolutePath.endsWith(".tdf"))
          throw new IllegalArgumentException("TDF file should have a .tdf file-extension"))
  }

  /** Returns command to execute */
  override def cmdLine =
    super.cmdLine +
      required("count") +
      optional("--maxZoom", maxZoom) +
      optional("--windowSize", windowSize) +
      optional("--extFactor", extFactor) +
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
      required(genomeChromSizes)

  /** This part should never fail, these values are set within this wrapper */
  private def outputArg: String = {
    (tdf, wig) match {
      case (None, None) =>
        throw new IllegalArgumentException("Either TDF or WIG should be supplied");
      case (Some(a), None) => a.getAbsolutePath;
      case (None, Some(b)) => b.getAbsolutePath;
      case (Some(a), Some(b)) => a.getAbsolutePath + "," + b.getAbsolutePath;
    }
  }
}

object IGVToolsCount {

  /**
    * Create an object by specifying the `input` (.bam),
    * and the `genomename` (hg18,hg19,mm10)
    *
    * @param input Bamfile to count reads from
    * @return a new IGVToolsCount instance
    * @throws FileNotFoundException bam File is not found
    * @throws IllegalArgumentException tdf or wig not supplied
    */
  def apply(root: Configurable, input: File, genomeChromSizes: File): IGVToolsCount = {
    val counting = new IGVToolsCount(root)
    counting.input = input
    counting.genomeChromSizes = genomeChromSizes
    counting
  }
}
