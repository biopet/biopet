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
import scalaz._, Scalaz._
import scala.io.Source
import scala.collection.mutable.Map

import nl.lumc.sasc.biopet.core.BiopetCommandLineFunction
import nl.lumc.sasc.biopet.core.config.Configurable
import org.broadinstitute.gatk.utils.commandline.{ Input, Output }
import java.io.File

class Seqstat(val root: Configurable) extends BiopetCommandLineFunction {
  override val defaultVmem = "4G"

  @Input(doc = "Input FastQ", required = true)
  var input: File = _

  @Output(doc = "JSON summary", required = true)
  var output: File = _

  executable = config("exe", default = "fastq-seqstat")

  def cmdLine = required(executable) + required(input) + " > " + required(output)

  def getSummary: Json = {
    val json = Parse.parseOption(Source.fromFile(output).mkString)
    if (json.isEmpty) return jNull
    else return json.get.fieldOrEmptyObject("stats")
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

  def mergeSummaries(jsons: List[Json]): Json = {
    def addJson(json: Json, total: Map[String, Long]) {
      for (key <- json.objectFieldsOrEmpty) {
        if (json.field(key).get.isObject) addJson(json.field(key).get, total)
        else if (json.field(key).get.isNumber) {
          val number = json.field(key).get.numberOrZero.toLong
          if (total.contains(key)) {
            if (key == "len_min") {
              if (total(key) > number) total(key) = number
            } else if (key == "len_max") {
              if (total(key) < number) total(key) = number
            } else total(key) += number
          } else total += (key -> number)
        }
      }
    }

    var basesTotal: Map[String, Long] = Map()
    var readsTotal: Map[String, Long] = Map()
    var encoding: Set[Json] = Set()
    for (json <- jsons) {
      encoding += json.fieldOrEmptyString("qual_encoding")

      val bases = json.fieldOrEmptyObject("bases")
      addJson(bases, basesTotal)

      val reads = json.fieldOrEmptyObject("reads")
      addJson(reads, readsTotal)
    }
    return ("bases" := (
      ("num_n" := basesTotal("num_n")) ->:
      ("num_total" := basesTotal("num_total")) ->:
      ("num_qual_gte" := (
        ("1" := basesTotal("1")) ->:
        ("10" := basesTotal("10")) ->:
        ("20" := basesTotal("20")) ->:
        ("30" := basesTotal("30")) ->:
        ("40" := basesTotal("40")) ->:
        ("50" := basesTotal("50")) ->:
        ("60" := basesTotal("60")) ->:
        jEmptyObject
      )) ->: jEmptyObject)) ->:
        ("reads" := (
          ("num_with_n" := readsTotal("num_with_n")) ->:
          ("num_total" := readsTotal("num_total")) ->:
          ("len_min" := readsTotal("len_min")) ->:
          ("len_max" := readsTotal("len_max")) ->:
          ("num_mean_qual_gte" := (
            ("1" := readsTotal("1")) ->:
            ("10" := readsTotal("10")) ->:
            ("20" := readsTotal("20")) ->:
            ("30" := readsTotal("30")) ->:
            ("40" := readsTotal("40")) ->:
            ("50" := readsTotal("50")) ->:
            ("60" := readsTotal("60")) ->:
            jEmptyObject
          )) ->: jEmptyObject)) ->:
            ("qual_encoding" := encoding.head) ->:
            jEmptyObject
  }
}
