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

import java.io.File

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.Ln

import scala.collection.mutable
import scala.io.Source

class Cutadapt(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Cutadapt(root) {
  var fastqc: Fastqc = _

  override def beforeCmd() {
    super.beforeCmd

    val foundAdapters = fastqc.foundAdapters.map(_.seq)
    if (default_clip_mode == "3") opt_adapter ++= foundAdapters
    else if (default_clip_mode == "5") opt_front ++= foundAdapters
    else if (default_clip_mode == "both") opt_anywhere ++= foundAdapters
  }

  override def summaryStats: Map[String, Any] = {
    val trimR = """.*Trimmed reads: *(\d*) .*""".r
    val tooShortR = """.*Too short reads: *(\d*) .*""".r
    val tooLongR = """.*Too long reads: *(\d*) .*""".r
    val adapterR = """Adapter '([C|T|A|G]*)'.*trimmed (\d*) times.""".r

    val stats: mutable.Map[String, Int] = mutable.Map("trimmed" -> 0, "tooshort" -> 0, "toolong" -> 0)
    val adapter_stats: mutable.Map[String, List[Any]] = mutable.Map()

    if (stats_output.exists) for (line <- Source.fromFile(stats_output).getLines) {
      line match {
        case trimR(m)     => stats += ("trimmed" -> m.toInt)
        case tooShortR(m) => stats += ("tooshort" -> m.toInt)
        case tooLongR(m)  => stats += ("toolong" -> m.toInt)
        case adapterR(adapter, count) =>
          val adapterName = fastqc.foundAdapters.find(_.seq == adapter) match {
            case None    => "unknown"
            case Some(a) => a.name
          }
          adapter_stats += (adapterName -> List(adapter, count.toInt))
        case _ =>
      }
    }

    Map("num_reads_affected" -> stats("trimmed"),
      "num_reads_discarded_too_short" -> stats("tooshort"),
      "num_reads_discarded_too_long" -> stats("toolong"),
      "adapters" -> adapter_stats.toMap
    )
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
