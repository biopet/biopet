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
package nl.lumc.sasc.biopet.extensions.picard

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Argument, Input, Output }

/** Extension for picard AddOrReplaceReadGroups */
class AddOrReplaceReadGroups(val root: Configurable) extends Picard {
  javaMainClass = new picard.sam.AddOrReplaceReadGroups().getClass.getName

  @Input(doc = "The input SAM or BAM files to analyze.", required = true)
  var input: File = _

  @Output(doc = "The output file to bam file to", required = true)
  var output: File = _

  @Output(doc = "The output file to bam file to", required = true)
  lazy val outputIndex: File = new File(output.getAbsolutePath.stripSuffix(".bam") + ".bai")

  @Argument(doc = "Sort order of output file Required. Possible values: {unsorted, queryname, coordinate} ", required = true)
  var sortOrder: String = _

  @Argument(doc = "RGID", required = true)
  var RGID: String = _

  @Argument(doc = "RGLB", required = true)
  var RGLB: String = _

  @Argument(doc = "RGPL", required = true)
  var RGPL: String = _

  @Argument(doc = "RGPU", required = true)
  var RGPU: String = _

  @Argument(doc = "RGSM", required = true)
  var RGSM: String = _

  @Argument(doc = "RGCN", required = false)
  var RGCN: String = _

  @Argument(doc = "RGDS", required = false)
  var RGDS: String = _

  @Argument(doc = "RGDT", required = false)
  var RGDT: String = _

  @Argument(doc = "RGPI", required = false)
  var RGPI: Option[Int] = _

  /** Returns command to execute */
  override def cmdLine = super.cmdLine +
    (if (inputAsStdin) required("INPUT=", new File("/dev/stdin"), spaceSeparated = false)
    else required("INPUT=", input, spaceSeparated = false)) +
    (if (outputAsStsout) required("OUTPUT=", new File("/dev/stdout"), spaceSeparated = false)
    else required("OUTPUT=", output, spaceSeparated = false)) +
    required("SORT_ORDER=", sortOrder, spaceSeparated = false) +
    required("RGID=", RGID, spaceSeparated = false) +
    required("RGLB=", RGLB, spaceSeparated = false) +
    required("RGPL=", RGPL, spaceSeparated = false) +
    required("RGPU=", RGPU, spaceSeparated = false) +
    required("RGSM=", RGSM, spaceSeparated = false) +
    optional("RGCN=", RGCN, spaceSeparated = false) +
    optional("RGDS=", RGDS, spaceSeparated = false) +
    optional("RGDT=", RGDT, spaceSeparated = false) +
    optional("RGPI=", RGPI, spaceSeparated = false)
}

object AddOrReplaceReadGroups {
  /** Returns default AddOrReplaceReadGroups */
  def apply(root: Configurable, input: File, output: File, sortOrder: String = null): AddOrReplaceReadGroups = {
    val addOrReplaceReadGroups = new AddOrReplaceReadGroups(root)
    addOrReplaceReadGroups.input = input
    addOrReplaceReadGroups.output = output
    if (sortOrder == null) addOrReplaceReadGroups.sortOrder = "coordinate"
    else addOrReplaceReadGroups.sortOrder = sortOrder
    addOrReplaceReadGroups
  }
}