package nl.lumc.sasc.biopet.pipelines.flexiprep.scripts

import java.io.File

import org.broadinstitute.gatk.utils.commandline.{ Input, Output }

import argonaut._, Argonaut._
import scalaz._, Scalaz._
import scala.io.Source
import scala.collection.mutable.Map

import nl.lumc.sasc.biopet.core.config.Configurable
import nl.lumc.sasc.biopet.extensions.PythonCommandLineFunction

class Seqstat(val root: Configurable) extends PythonCommandLineFunction {
  setPythonScript("__init__.py", "pyfastqc/")
  setPythonScript("seq_stat.py")

  @Input(doc = "Fastq input", shortName = "fastqc", required = true)
  var input_fastq: File = _

  @Output(doc = "Output file", shortName = "out", required = true)
  var out: File = _

  var fmt: String = _

  def cmdLine = {
    getPythonCommand +
      optional("--fmt", fmt) +
      required("-o", out) +
      required(input_fastq)
  }

  def getSummary: Json = {
    val json = Parse.parseOption(Source.fromFile(out).mkString)
    if (json.isEmpty) return jNull
    else return json.get.fieldOrEmptyObject("stats")
  }
}

object Seqstat {
  def apply(root: Configurable, fastqfile: File, outDir: String): Seqstat = {
    val seqstat = new Seqstat(root)
    val ext = fastqfile.getName.substring(fastqfile.getName.lastIndexOf("."))
    seqstat.input_fastq = fastqfile
    seqstat.out = new File(outDir + fastqfile.getName.substring(0, fastqfile.getName.lastIndexOf(".")) + ".seqstats.json")
    return seqstat
  }

  def mergeSummarys(jsons: List[Json]): Json = {
    def addJson(json:Json, total:Map[String, Int]) {
      for (key <- json.objectFieldsOrEmpty) {
        if (json.field(key).get.isObject) addJson(json.field(key).get, total)
        else if (json.field(key).get.isNumber) {
          val number = json.field(key).get.numberOrZero.toInt
          if (total.contains(key)) {
            if (key == "len_min") {
              if (total(key) > number) total(key) = number
            } else if (key == "len_max") {
              if (total(key) < number) total(key) = number
            } else total(key) += number
          }
          else total += (key -> number)
        }
      }
    }
    
    var basesTotal: Map[String, Int] = Map()
    var readsTotal: Map[String, Int] = Map()
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
        ) ) ->: jEmptyObject)) ->:
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
        ) ) ->: jEmptyObject)) ->:
      ("qual_encoding" := encoding.head) ->:
      jEmptyObject
  }
}
