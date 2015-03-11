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
 * A dual licensing mode is applied. The source code within this project that are
 * not part of GATK Queue is freely available for non-commercial use under an AGPL
 * license; For commercial users or users who do not want to follow the AGPL
 * license, please contact us to obtain a separate license.
 */
package nl.lumc.sasc.biopet.extensions.samtools

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

/** Extension for samtools view */
class SamtoolsView(val root: Configurable) extends Samtools {
  @Input(doc = "Bam File")
  var input: File = null

  @Output(doc = "output File")
  var output: File = null

  var quality: Option[Int] = config("quality")
  var b: Boolean = config("b", default = false)
  var h: Boolean = config("h", default = false)
  var f: List[String] = config("f", default = List.empty[String])
  var F: List[String] = config("F", default = List.empty[String])

  def cmdBase = required(executable) +
    required("view") +
    optional("-q", quality) +
    repeat("-f", f) +
    repeat("-F", F) +
    conditional(b, "-b") +
    conditional(h, "-h")
  def cmdPipeInput = cmdBase + "-"
  def cmdPipe = cmdBase + required(input)

  /** Returns command to execute */
  def cmdLine = cmdPipe + " > " + required(output)
}

object SamtoolsView {
  def apply(root: Configurable, input: File, output: File): SamtoolsView = {
    val view = new SamtoolsView(root)
    view.input = input
    view.output = output
    return view
  }

  def apply(root: Configurable, input: File, outputDir: String): SamtoolsView = {
    val dir = if (outputDir.endsWith("/")) outputDir else outputDir + "/"
    val outputFile = new File(dir + swapExtension(input.getName))
    return apply(root, input, outputFile)
  }

  def apply(root: Configurable, input: File): SamtoolsView = {
    return apply(root, input, new File(swapExtension(input.getAbsolutePath)))
  }

  private def swapExtension(inputFile: String) = inputFile.stripSuffix(".bam") + ".mpileup"
}