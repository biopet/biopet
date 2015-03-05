package nl.lumc.sasc.biopet.extensions.bwa

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Output, Input }

/**
 * BWA samse wrapper
 *
 * based on executable version 0.7.10-r789
 *
 */
class BwaSamse(val root: Configurable) extends Bwa {
  @Input(doc = "Fastq file", required = true)
  var fastq: File = _

  @Input(doc = "Sai file", required = true)
  var sai: File = _

  @Input(doc = "The reference file for the bam files.", required = true)
  var reference: File = config("reference")

  @Output(doc = "Output file SAM", required = false)
  var output: File = _

  var n: Option[Int] = config("n")
  var r: String = _

  /** Returns command to execute */
  def cmdLine = required(executable) +
    required("samse") +
    optional("-n", n) +
    optional("-f", output) +
    optional("-r", r) +
    required(reference) +
    required(sai) +
    required(fastq)
}
