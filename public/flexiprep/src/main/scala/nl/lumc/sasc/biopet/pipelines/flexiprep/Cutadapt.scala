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
package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.summary.Summarizable

import scala.io.Source

import nl.lumc.sasc.biopet.extensions.Ln
import org.broadinstitute.gatk.utils.commandline.{ Input }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import java.io.File
import nl.lumc.sasc.biopet.core.config.Configurable
import scala.collection.mutable

class Cutadapt(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Cutadapt(root) with Summarizable {
  var fastqc: Fastqc = _

  override def beforeCmd() {
    super.beforeCmd

    val foundAdapters = fastqc.foundAdapters.map(_.seq)
    if (default_clip_mode == "3") opt_adapter ++= foundAdapters
    else if (default_clip_mode == "5") opt_front ++= foundAdapters
    else if (default_clip_mode == "both") opt_anywhere ++= foundAdapters
  }

  override def cmdLine = {
    if (opt_adapter.nonEmpty || opt_anywhere.nonEmpty || opt_front.nonEmpty) {
      analysisName = getClass.getSimpleName
      super.cmdLine
    } else {
      analysisName = getClass.getSimpleName + "-ln"
      Ln(this, fastq_input, fastq_output, relative = true).cmd
    }
  }

  def summaryData: Map[String, Any] = {
    val trimR = """.*Trimmed reads: *(\d*) .*""".r
    val tooShortR = """.*Too short reads: *(\d*) .*""".r
    val tooLongR = """.*Too long reads: *(\d*) .*""".r
    val adapterR = """Adapter '([C|T|A|G]*)'.*trimmed (\d*) times.""".r

    val stats: mutable.Map[String, Int] = mutable.Map("trimmed" -> 0, "tooshort" -> 0, "toolong" -> 0)
    val adapter_stats: mutable.Map[String, Int] = mutable.Map()

    if (stats_output.exists) for (line <- Source.fromFile(stats_output).getLines) {
      line match {
        case trimR(m)                 => stats += ("trimmed" -> m.toInt)
        case tooShortR(m)             => stats += ("tooshort" -> m.toInt)
        case tooLongR(m)              => stats += ("toolong" -> m.toInt)
        case adapterR(adapter, count) => adapter_stats += (adapter -> count.toInt)
        case _                        =>
      }
    }

    Map("version" -> getVersion,
      "num_reads_affected" -> stats("trimmed"),
      "num_reads_discarded_too_short" -> stats("tooshort"),
      "num_reads_discarded_too_long" -> stats("toolong"),
      "adapters" -> adapter_stats
    )
  }

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Int, v2: Int) => v1 + v2
      case _                  => v1
    }
  }

  def summaryFiles: Map[String, File] = Map("input" -> fastq_input, "output" -> fastq_output)
}

object Cutadapt {
  def apply(root: Configurable, input: File, output: File): Cutadapt = {
    val cutadapt = new Cutadapt(root)
    cutadapt.fastq_input = input
    cutadapt.fastq_output = output
    cutadapt.stats_output = new File(output.getAbsolutePath.substring(0, output.getAbsolutePath.lastIndexOf(".")) + ".stats")
    return cutadapt
  }
}
