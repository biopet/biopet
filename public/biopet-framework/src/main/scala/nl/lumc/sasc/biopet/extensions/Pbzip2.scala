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
package nl.lumc.sasc.biopet.extensions

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

/** Extension for pbzip2 */
class Pbzip2(val root: Configurable) extends BiopetCommandLineFunction {
  @Input(doc = "Zipped file")
  var input: File = _

  @Output(doc = "Unzipped file")
  var output: File = _

  executable = config("exe", default = "pbzip2")

  var decomrpess = true
  var memory: Option[Int] = config("memory")

  override val defaultVmem = (memory.getOrElse(1000) * 2 / 1000) + "G"
  override val defaultThreads = 2

  override def beforeCmd {
    if (!memory.isEmpty) memory = Option(memory.get * threads)
  }

  /** return commandline to execute */
  def cmdLine = required(executable) +
    conditional(decomrpess, "-d") +
    conditional(!decomrpess, "-z") +
    optional("-p", threads, spaceSeparated = false) +
    optional("-m", memory, spaceSeparated = false) +
    required("-c", output) +
    required(input)
}

/** Object for constructors for Pbzip2 */
object Pbzip2 {
  /** Default constructor */
  def apply(root: Configurable, input: File, output: File): Pbzip2 = {
    val pbzip2 = new Pbzip2(root)
    pbzip2.input = input
    pbzip2.output = output
    return pbzip2
  }
}