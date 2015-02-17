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

/*
 * Wrapper around the seqstat implemented in D
 * 
 */

import argonaut._, Argonaut._
import nl.lumc.sasc.biopet.core.summary.Summarizable
import nl.lumc.sasc.biopet.utils.ConfigUtils
import scalaz._, Scalaz._
import scala.io.Source
import scala.collection.mutable

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Seqstat(val root: Configurable) extends BiopetCommandLineFunction with Summarizable {
  override val defaultVmem = "4G"

  @Input(doc = "Input FastQ", required = true)
  var input: File = _

  @Output(doc = "JSON summary", required = true)
  var output: File = _

  executable = config("exe", default = "fastq-seqstat")

  def cmdLine = required(executable) + required(input) + " > " + required(output)

  def summaryData: Map[String, Any] = {
    val map = ConfigUtils.fileToConfigMap(output)

    ConfigUtils.any2map(map.getOrElse("stats", Map()))
  }

  def summaryFiles: Map[String, File] = Map()

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Int, v2: Int) if key == "len_min" => if (v1 < v2) v1 else v2
      case (v1: Int, v2: Int) if key == "len_max" => if (v1 > v2) v1 else v2
      case (v1: Int, v2: Int)                     => v1 + v2
      case _                                      => v1
    }
  }
}

object Seqstat {
  def apply(root: Configurable, fastqfile: File, outDir: String): Seqstat = {
    val seqstat = new Seqstat(root)
    val ext = fastqfile.getName.substring(fastqfile.getName.lastIndexOf("."))
    seqstat.input = fastqfile
    seqstat.output = new File(outDir + fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    return seqstat
  }
}
