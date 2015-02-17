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

import argonaut._, Argonaut._
import nl.lumc.sasc.biopet.core.summary.Summarizable
import scalaz._, Scalaz._

import scala.io.Source
import scala.collection.mutable

class Sickle(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Sickle(root) with Summarizable {

  def summaryData: Map[String, Any] = {
    val pairKept = """FastQ paired records kept: (\d*) \((\d*) pairs\)""".r
    val singleKept = """FastQ single records kept: (\d*) \(from PE1: (\d*), from PE2: (\d*)\)""".r
    val pairDiscarded = """FastQ paired records discarded: (\d*) \((\d*) pairs\)""".r
    val singleDiscarded = """FastQ single records discarded: (\d*) \(from PE1: (\d*), from PE2: (\d*)\)""".r

    var stats: mutable.Map[String, Int] = mutable.Map()

    if (output_stats.exists) for (line <- Source.fromFile(output_stats).getLines) {
      line match {
        case pairKept(reads, pairs) => stats += ("num_paired_reads_kept" -> reads.toInt)
        case singleKept(total, r1, r2) => {
          stats += ("num_reads_kept_R1" -> r1.toInt)
          stats += ("num_reads_kept_R2" -> r2.toInt)
        }
        case pairDiscarded(reads, pairs) => stats += ("num_paired_reads_discarded" -> reads.toInt)
        case singleDiscarded(total, r1, r2) => {
          stats += ("num_reads_discarded_R1" -> r1.toInt)
          stats += ("num_reads_discarded_R2" -> r2.toInt)
        }
        case _ =>
      }
    }

    stats.toMap ++ Map("version" -> getVersion)
  }

  override def resolveSummaryConflict(v1: Any, v2: Any, key: String): Any = {
    (v1, v2) match {
      case (v1: Int, v2: Int) => v1 + v2
      case _                  => v1
    }
  }

  def summaryFiles: Map[String, File] = Map()
}
