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
package nl.lumc.sasc.biopet.extensions.bwa

import java.io.File

import nl.lumc.sasc.biopet.utils.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

/**
 * Extension for bwa aln
 *
 * Based on version 0.7.12-r1039
 *
 * Created by pjvan_thof on 1/16/15.
 */
class BwaIndex(val root: Configurable) extends Bwa {
  @Input(doc = "Fastq file", required = true)
  var reference: File = _

  @Output(doc = "Index files for bwa", required = false)
  private var output: List[File] = Nil

  var a: Option[String] = config("a", freeVar = false)
  var p: Option[String] = config("p", freeVar = false)
  var b: Option[Int] = config("e", freeVar = false)
  var _6: Boolean = config("6", default = false, freeVar = false)

  override def defaultCoreMemory = 35.0

  override def beforeGraph() {
    super.beforeGraph()
    List(".sa", ".pac")
      .foreach(ext => output ::= new File(reference.getAbsolutePath + ext))
    output = output.distinct
  }

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required("index") +
    optional("-a", a) +
    optional("-p", p) +
    optional("-b", b) +
    conditional(_6, "-6") +
    required(reference)
}
