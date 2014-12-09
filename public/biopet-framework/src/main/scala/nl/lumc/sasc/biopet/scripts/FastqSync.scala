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
package nl.lumc.sasc.biopet.scripts

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

import scala.io.Source

class FastqSync(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("sync_paired_end_reads.py")

  @Input(doc = "Start fastq")
  var input_start_fastq: File = _

  @Input(doc = "R1 input")
  var input_R1: File = _

  @Input(doc = "R2 input")
  var input_R2: File = _

  @Output(doc = "R1 output")
  var output_R1: File = _

  @Output(doc = "R2 output")
  var output_R2: File = _

  //No output Annotation so file 
  var output_stats: File = _

  def cmdLine = {
    getPythonCommand +
      required(input_start_fastq) +
      required(input_R1) +
      required(input_R2) +
      required(output_R1) +
      required(output_R2) +
      " > " +
      required(output_stats)
  }

  def getSummary: Json = {
    val R1_filteredR = """Filtered (\d*) reads from first read file.""".r
    val R2_filteredR = """Filtered (\d*) reads from second read file.""".r
    val readsLeftR = """Synced read files contain (\d*) reads.""".r

    var R1_filtered = 0
    var R2_filtered = 0
    var readsLeft = 0

    if (output_stats.exists) for (line <- Source.fromFile(output_stats).getLines) {
      line match {
        case R1_filteredR(m) => R1_filtered = m.toInt
        case R2_filteredR(m) => R2_filtered = m.toInt
        case readsLeftR(m)   => readsLeft = m.toInt
        case _               =>
      }
    }

    return ("num_reads_discarded_R1" := R1_filtered) ->:
      ("num_reads_discarded_R2" := R2_filtered) ->:
      ("num_reads_kept" := readsLeft) ->:
      jEmptyObject
  }
}

object FastqSync {
  def apply(root: Configurable, input_start_fastq: File, input_R1: File, input_R2: File,
            output_R1: File, output_R2: File, output_stats: File): FastqSync = {
    val fastqSync = new FastqSync(root)
    fastqSync.input_start_fastq = input_start_fastq
    fastqSync.input_R1 = input_R1
    fastqSync.input_R2 = input_R2
    fastqSync.output_R1 = output_R1
    fastqSync.output_R2 = output_R2
    fastqSync.output_stats = output_stats
    return fastqSync
  }

  def mergeSummaries(jsons: List[Json]): Json = {
    var R1_filtered = 0
    var R2_filtered = 0
    var readsLeft = 0

    for (json <- jsons) {
      R1_filtered += json.field("num_reads_discarded_R1").get.numberOrZero.toInt
      R2_filtered += json.field("num_reads_discarded_R2").get.numberOrZero.toInt
      readsLeft += json.field("num_reads_kept").get.numberOrZero.toInt
    }

    return ("num_reads_discarded_R1" := R1_filtered) ->:
      ("num_reads_discarded_R2" := R2_filtered) ->:
      ("num_reads_kept" := readsLeft) ->:
      jEmptyObject
  }
}