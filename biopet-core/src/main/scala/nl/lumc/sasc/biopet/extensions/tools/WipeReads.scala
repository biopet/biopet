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
package nl.lumc.sasc.biopet.extensions.tools

import java.io.File

import nl.lumc.sasc.biopet.core.ToolCommandFunction
import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{Argument, Input, Output}

/**
  * WipeReads function class for usage in Biopet pipelines
  *
  * @param parent Configuration object for the pipeline
  */
class WipeReads(val parent: Configurable) extends ToolCommandFunction {

  def toolObject = nl.lumc.sasc.biopet.tools.WipeReads

  @Input(doc = "Input BAM file (must be indexed)", shortName = "I", required = true)
  var inputBam: File = _

  @Input(doc = "Interval file", shortName = "r", required = true)
  var intervalFile: File = _

  @Argument(doc = "Minimum MAPQ of reads in target region to remove (default: 0)")
  val minMapQ: Option[Int] = config("min_mapq")

  @Argument(doc = "Read group IDs to be removed (default: remove reads from all read groups)")
  val readgroup: Set[String] = config("read_group", default = Nil)

  @Argument(
    doc = "Whether to remove multiple-mapped reads outside the target regions (default: yes)")
  val limitRemoval: Boolean = config("limit_removal", default = false)

  @Argument(doc = "Whether to index output BAM file or not")
  val noMakeIndex: Boolean = config("no_make_index", default = false)

  @Argument(doc = "GTF feature containing intervals (default: exon)")
  val featureType: Option[String] = config("feature_type")

  @Argument(doc = "Expected maximum number of reads in target regions (default: 7e7)")
  val bloomSize: Option[Long] = config("bloom_size")

  @Argument(doc = "False positive rate (default: 4e-7)")
  val falsePositive: Option[Long] = config("false_positive")

  @Output(doc = "Output BAM", shortName = "o", required = true)
  var outputBam: File = _

  @Output(required = false)
  private var outputIndex: Option[File] = None

  @Output(doc = "BAM containing discarded reads", shortName = "f", required = false)
  var discardedBam: Option[File] = None

  override def beforeGraph() {
    super.beforeGraph()
    if (!noMakeIndex) outputIndex = Some(new File(outputBam.getPath.stripSuffix(".bam") + ".bai"))
  }

  override def cmdLine: String =
    super.cmdLine +
      required("-I", inputBam) +
      required("-r", intervalFile) +
      required("-o", outputBam) +
      optional("-f", discardedBam) +
      optional("-Q", minMapQ) +
      readgroup.foreach(rg => required("-G", rg)) +
      conditional(limitRemoval, "--limit_removal") +
      conditional(noMakeIndex, "--no_make_index") +
      optional("-t", featureType) +
      optional("--bloom_size", bloomSize) +
      optional("--false_positive", falsePositive)

}
