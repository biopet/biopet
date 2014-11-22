package nl.lumc.sasc.biopet.pipelines.flexiprep

import nl.lumc.sasc.biopet.core.config.Configurable

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import scala.io.Source
import scala.collection.mutable.Map

class Sickle(root: Configurable) extends nl.lumc.sasc.biopet.extensions.Sickle(root) {
  def getSummary: Json = {
    val pairKept = """FastQ paired records kept: (\d*) \((\d*) pairs\)""".r
    val singleKept = """FastQ single records kept: (\d*) \(from PE1: (\d*), from PE2: (\d*)\)""".r
    val pairDiscarded = """FastQ paired records discarded: (\d*) \((\d*) pairs\)""".r
    val singleDiscarded = """FastQ single records discarded: (\d*) \(from PE1: (\d*), from PE2: (\d*)\)""".r

    var stats: Map[String, Int] = Map()

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

    val temp = ("" := stats.toMap) ->: jEmptyObject
    return temp.fieldOrEmptyObject("")
  }
}

object Sickle {
  def mergeSummaries(jsons: List[Json]): Json = {
    var total: Map[String, Int] = Map()

    for (json <- jsons) {
      for (key <- json.objectFieldsOrEmpty) {
        if (json.field(key).get.isNumber) {
          val number = json.field(key).get.numberOrZero.toInt
          if (total.contains(key)) total(key) += number
          else total += (key -> number)
        }
      }
    }

    val temp = ("" := total.toMap) ->: jEmptyObject
    return temp.fieldOrEmptyObject("")
  }
}